package com.oxygenxml.git.view.history;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitController;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Equaler;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.HiDPIUtil;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Presents the commits for a given resource. 
 */
public class HistoryPanel extends JPanel implements Observer<PushPullEvent> {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class);
  /**
   * Git API access.
   */
  private static final GitAccess gitAccess = GitAccess.getInstance();
  /**
   * Table view that presents the commits.
   */
  JTable historyTable;
  /**
   * Panel presenting a detailed description of the commit (author, date, etc).
   */
  private JEditorPane commitDescriptionPane;
  /**
   * The label that shows information about the history we present.
   */
  private JLabel historyInfoLabel;
  /**
   * Intercepts clicks in the commit details area.
   */
  private HistoryHyperlinkListener hyperlinkListener;
  /**
   * Commit selection listener that updates all the views with details.
   */
  private RowHistoryTableSelectionListener revisionDataUpdater;
  /**
   * The changed files from a commit.
   */
  private JTable affectedFilesTable;
  /**
   * The file path of the resource for which we are currently presenting the history. If <code>null</code>, we 
   * present the history for the entire repository.
   */
  private String activeFilePath;
  /**
   * Presents the contextual menu.
   */
  private HistoryViewContextualMenuPresenter contextualMenuPresenter;
  /**
  * Plugin workspace access.
  */
  private StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
  
  /**
   * Constructor.
   * 
   * @param stageController Executes a set of Git commands.
   */
  public HistoryPanel(GitController stageController, PushPullController pushPullController) {
    pushPullController.addObserver(this);
    setLayout(new BorderLayout());
    
    contextualMenuPresenter = new HistoryViewContextualMenuPresenter(stageController);

    historyTable = UIUtil.createTable();
    historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    historyTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showHistoryTableContextualMenu(historyTable, e.getPoint());
        }
      }

      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        mousePressed(e);
      }
      
      @Override
      public void mouseClicked(MouseEvent e) {
        if (activeFilePath != null 
            && !e.isConsumed()
            && !e.isPopupTrigger()
            && e.getClickCount() == 2) {
          e.consume();
          int rowAtPoint = historyTable.rowAtPoint(e.getPoint());
          if (rowAtPoint != -1) {
            updateTableSelection(historyTable, rowAtPoint);
            historyDoubleClickAction(rowAtPoint);
          }
        }
      }
    });

    JScrollPane historyTableScrollPane = new JScrollPane(historyTable);
    historyTable.setFillsViewportHeight(true);

    commitDescriptionPane = new JEditorPane();
    initEditorPane(commitDescriptionPane);
    JScrollPane commitDescriptionScrollPane = new JScrollPane(commitDescriptionPane);
    
    affectedFilesTable = createAffectedFilesTable();
    affectedFilesTable.setFillsViewportHeight(true);
    JScrollPane affectedFilesTableScrollPane = new JScrollPane(affectedFilesTable);

    Dimension minimumSize = new Dimension(500, 150);
    commitDescriptionScrollPane.setPreferredSize(minimumSize);
    affectedFilesTableScrollPane.setPreferredSize(minimumSize);

    //----------
    // Top panel (with the "Showing history" label and the "Refresh" action
    //----------
    
    JPanel topPanel = new JPanel(new BorderLayout());
    historyInfoLabel = new JLabel();
    topPanel.add(historyInfoLabel, BorderLayout.WEST);
    createAndAddToolbarToTopPanel(topPanel);

    JPanel infoBoxesSplitPane = createSplitPane(
        JideSplitPane.HORIZONTAL_SPLIT,
        commitDescriptionScrollPane,
        affectedFilesTableScrollPane);
    JideSplitPane centerSplitPane = createSplitPane(
        JideSplitPane.VERTICAL_SPLIT,
        historyTableScrollPane,
        infoBoxesSplitPane);
    centerSplitPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    
    //Customize the split pane.
    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        int h = centerSplitPane.getHeight();
        centerSplitPane.setDividerLocation(0, (int)(h * 0.6));
        
        removeComponentListener(this);
      }
    });
    
    stageController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if (isShowing()) {
          GitOperation operation = info.getGitOperation();
          switch (operation) {
            case OPEN_WORKING_COPY:
              GitOperationScheduler.getInstance().schedule(HistoryPanel.this::showRepositoryHistory);
              break;
            case CREATE_BRANCH:
            case CHECKOUT:
            case DELETE_BRANCH:
            case STAGE:
            case UNSTAGE:
            case COMMIT:
            case DISCARD:
            case MERGE_RESTART:
            case ABORT_REBASE:
            case CONTINUE_REBASE:
              refresh();
              break;
            default:
              break;
          }
        }
      }
    });
    
    // Listens on the save event in the Oxygen editor and updates the history table
    pluginWS.addEditorChangeListener(
        new WSEditorChangeListener() {
          @Override
          public void editorOpened(final URL editorLocation) {
            addEditorSaveHook(editorLocation);
          }
        },
        PluginWorkspace.MAIN_EDITING_AREA);

    add(centerSplitPane, BorderLayout.CENTER);
  }
  
  /**
   * Adds a hook to refresh the models if the editor is part of the Git working copy.
   * 
   * @param editorLocation Editor to check.
   */
  private void addEditorSaveHook(final URL editorLocation) {
    WSEditor editorAccess = pluginWS.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
    if (editorAccess != null) {
      editorAccess.addEditorListener(new WSEditorListener() {
        @Override
        public void editorSaved(int operationType) {
          GitOperationScheduler.getInstance().schedule(() -> treatEditorSavedEvent(editorLocation));
        }
      });
    }
  }
  
  /**
   * Treat editor saved event.
   * 
   * @param editorLocation Editor URL.
   */
  private void treatEditorSavedEvent(final URL editorLocation) {
    File localFile = null;
    if ("file".equals(editorLocation.getProtocol())) {
      localFile = pluginWS.getUtilAccess().locateFile(editorLocation);
      if (localFile != null) {
        String fileInWorkPath = localFile.toString();
        fileInWorkPath = FileHelper.rewriteSeparator(fileInWorkPath);

        try {
          String selectedRepositoryPath = GitAccess.getInstance().getWorkingCopy().getAbsolutePath();
          selectedRepositoryPath = FileHelper.rewriteSeparator(selectedRepositoryPath);

          if (isShowing() && fileInWorkPath.startsWith(selectedRepositoryPath)) {
            refresh();
          }
        } catch (NoRepositorySelected e) {
          LOGGER.debug(e, e);
        }
      }
    }
  }
  
  /**
   * Opens the first action in the contextual menu when an element inside the
   * history table is double clicked.
   * 
   * @param rowAtPoint Position of the element in the history table.
   */
  private void historyDoubleClickAction(int rowAtPoint) {
    HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
    int convertedSelectedRow = historyTable.convertRowIndexToModel(rowAtPoint);
    CommitCharacteristics commitCharacteristics = historyTableModel.getAllCommits().get(convertedSelectedRow);
    try {
      Optional<FileStatus> optionalFileStatus = contextualMenuPresenter.getFileStatus(activeFilePath,
          commitCharacteristics);
      if (optionalFileStatus.isPresent()) {
        FileStatus fileStatus = optionalFileStatus.get();
        List<Action> contextualActions = contextualMenuPresenter.getContextualActions(fileStatus,
            commitCharacteristics, false);
        if (!contextualActions.isEmpty()) {
          contextualActions.get(0).actionPerformed(null);
        }
      }
    } catch (IOException | GitAPIException e1) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e1.getMessage());
      LOGGER.error(e1, e1);
    }
  }

  /**
   * Creates the table that presents the files changed in a revision.
   * 
   * @return The table that presents the files.
   */
  private JTable createAffectedFilesTable() {
    JTable table = UIUtil.createResourcesTable(
        new StagingResourcesTableModel(null, true),
        () -> false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showResourcesContextualMenu(table, e.getPoint());
        }
      }
      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        mousePressed(e);
      }
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!e.isConsumed()
            && !e.isPopupTrigger()
            && e.getClickCount() == 2) {
          e.consume();
          int rowAtPoint = table.rowAtPoint(e.getPoint());
          if (rowAtPoint != -1) {
            updateTableSelection(table, rowAtPoint);

            StagingResourcesTableModel model = (StagingResourcesTableModel) table.getModel();
            int convertedSelectedRow = table.convertRowIndexToModel(rowAtPoint);
            FileStatus file = model.getFileStatus(convertedSelectedRow);

            HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
            CommitCharacteristics commitCharacteristics = historyTableModel.getAllCommits()
                .get(historyTable.getSelectedRow());

            List<Action> contextualActions = contextualMenuPresenter.getContextualActions(file, commitCharacteristics, false);
            if (!contextualActions.isEmpty()) {
              contextualActions.get(0).actionPerformed(null);
            }
          }
        }
      }
    });
    
    return table;
  }
  
  /**
   * Show the contextual menu on the resources changed on a revision.
   * 
   * @param affectedFilesTable The table with the files from a committed on a revision.
   * @param point              The point where to show the contextual menu.
   */
  protected void showResourcesContextualMenu(JTable affectedFilesTable, Point point) {
    int rowAtPoint = affectedFilesTable.rowAtPoint(point);
    if (rowAtPoint != -1) {
      updateTableSelection(affectedFilesTable, rowAtPoint);
      
      StagingResourcesTableModel model = (StagingResourcesTableModel) affectedFilesTable.getModel();
      int convertedSelectedRow = affectedFilesTable.convertRowIndexToModel(rowAtPoint);
      FileStatus file = model.getFileStatus(convertedSelectedRow);
      
      HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
      CommitCharacteristics commitCharacteristics = historyTableModel.getAllCommits().get(historyTable.getSelectedRow());
      
      JPopupMenu jPopupMenu = new JPopupMenu();
      contextualMenuPresenter.populateContextActionsForFile(jPopupMenu, file, commitCharacteristics, false);
      jPopupMenu.show(affectedFilesTable, point.x, point.y);
    }
  }
  
  /**
   * Show the contextual menu on the history table.
   * 
   * @param commitResourcesTable The table with the files from a committed on a revision.
   * @param point                The point where to show the contextual menu.
   */
  protected void showHistoryTableContextualMenu(JTable historyTable, Point point) {
    // If we present the history for a specific file.
    int rowAtPoint = historyTable.rowAtPoint(point);
    if (rowAtPoint != -1) {
      updateTableSelection(historyTable, rowAtPoint);

      int[] selectedRows = historyTable.getSelectedRows();
      CommitCharacteristics[] cc = new CommitCharacteristics[selectedRows.length];
      for (int i = 0; i < selectedRows.length; i++) {
        HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
        int convertedSelectedRow = historyTable.convertRowIndexToModel(selectedRows[i]);
        CommitCharacteristics commitCharacteristics = historyTableModel.getAllCommits().get(convertedSelectedRow);
        cc[i] = commitCharacteristics;
      }

      try {
        JPopupMenu jPopupMenu = new JPopupMenu();
        contextualMenuPresenter.populateContextualActionsHistoryContext(jPopupMenu, activeFilePath, cc);

        jPopupMenu.show(historyTable, point.x, point.y);
      } catch (IOException | GitAPIException e) {
        LOGGER.error(e, e);
      }
    }
  }

  /**
   * Checks if a row is selected and selects it if it isn't.
   * 
   * @param table Table.
   * @param rowIndex Row index to check. 
   */
  private void updateTableSelection(JTable table, int rowIndex) {
    int[] selectedRows = table.getSelectedRows();
    boolean alreadySelected = Arrays.stream(selectedRows).anyMatch(r -> r == rowIndex);
    if (!alreadySelected) {
      table.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }
  }

  /**
   * Creates a split pane and puts the two components in it.
   * 
   * @param splitType {@link JideSplitPane#HORIZONTAL_SPLIT} or {@link JideSplitPane#VERTICAL_SPLIT} 
   * @param firstComponent Fist component to add.
   * @param secondComponent Second component to add.
   * 
   * @return The split pane.
   */
  private JideSplitPane createSplitPane(int splitType, JComponent firstComponent, JComponent secondComponent) {
    JideSplitPane splitPane = new JideSplitPane(splitType);
    
    splitPane.add(firstComponent);
    splitPane.add(secondComponent);
    
    splitPane.setDividerSize(5);
    splitPane.setContinuousLayout(true);
    splitPane.setOneTouchExpandable(false);
    splitPane.setBorder(null);
    
    return splitPane;
  }
  
  /**
   * Initializes the split with the proper font and other properties.
   * 
   * @param editorPane Editor pane to initialize.
   */
  private static void initEditorPane(JEditorPane editorPane) {
    // Forces the JEditorPane to take the font from the UI, rather than the HTML document.
    editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    Font font = UIManager.getDefaults().getFont("TextArea.font");
    if(font != null){
      editorPane.setFont(font);
    }
    editorPane.setBorder(null);
    editorPane.setContentType("text/html");
    editorPane.setEditable(false);

  }

  /**
   * Creates the toolbar. 
   * 
   * @param topPanel Parent for the toolbar.
   */
  private void createAndAddToolbarToTopPanel(JPanel topPanel) {
    JToolBar toolbar = new JToolBar();
    toolbar.setOpaque(false);
    toolbar.setFloatable(false);
    topPanel.add(toolbar, BorderLayout.EAST);
    
    // Add the Refresh action to the toolbar
    Action refreshAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        refresh();
      }
    };
    refreshAction.putValue(Action.SMALL_ICON, Icons.getIcon(Icons.REFRESH_ICON));
    refreshAction.putValue(Action.SHORT_DESCRIPTION, Translator.getInstance().getTranslation(Tags.REFRESH));
    ToolbarButton refreshButton = new ToolbarButton(refreshAction, false);
    toolbar.add(refreshButton);
    
    add(topPanel, BorderLayout.NORTH);
  }

  /**
   * Shows the commit history for the entire repository.
   */
  public void showRepositoryHistory() {
    showHistory(null, true);
  }
  
  /**
   * Shows the commit history for the given file.
   * 
   * @param filePath File for which to present the commit that changed him.
   */
  public void showHistory(String filePath) {
    showHistory(filePath, false);
  }
  
  /**
   * Refresh.
   */
  public void refresh() {
    GitOperationScheduler.getInstance().schedule(() -> showHistory(activeFilePath, true));
  }

  /**
   * Shows the commit history for the entire repository.
   * 
   * @param filePath File for which to present the commit that changed him.
   * @param force    <code>true</code> to recompute the history data,
   *                     even if the view already presents the history
   *                      for the given resource.
   */
  private void showHistory(String filePath, boolean force) {
    Translator translator = Translator.getInstance();
    
    updateSelectionMode(filePath);
    
    if (force
        // Check if we don't already present the history for this path!!!!
        || !Equaler.verifyEquals(filePath, activeFilePath)) {
      this.activeFilePath = filePath;

      try {
        // Make sure we know about the remote as well, to present data about the upstream branch.
        tryFetch();

        File directory = gitAccess.getWorkingCopy();
        String historyLabelMessage = translator.getTranslation(Tags.REPOSITORY) + ": " + directory.getName() + ". "
            + translator.getTranslation(Tags.BRANCH) + ": " + gitAccess.getBranchInfo().getBranchName() + ".";
        if (filePath != null) {
          directory = new File(directory, filePath);
          historyLabelMessage += " " + translator.getTranslation(Tags.FILE) + ": " + directory.getName() + ".";
        }
        historyInfoLabel.setText(historyLabelMessage);
        historyInfoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 0));

        // Install selection listener.
        if (revisionDataUpdater != null) {
          historyTable.getSelectionModel().removeListSelectionListener(revisionDataUpdater);
        }
        
        StagingResourcesTableModel dataModel = (StagingResourcesTableModel) affectedFilesTable.getModel();
        dataModel.setFilesStatus(Collections.emptyList());
        commitDescriptionPane.setText("");

        final List<CommitCharacteristics> commitCharacteristicsVector = gitAccess.getCommitsCharacteristics(filePath);
        
        Repository repo = gitAccess.getRepository();
        CommitsAheadAndBehind commitsAheadAndBehind = RevCommitUtil.getCommitsAheadAndBehind(repo, repo.getFullBranch());
        
        // Compute the row height.
        CommitMessageTableRenderer renderer = new CommitMessageTableRenderer(
            repo, 
            commitsAheadAndBehind,
            gitAccess.getBranchInfo().getBranchName(),
            getTagMap(repo),
            gitAccess.getBranchMap(repo, ConfigConstants.CONFIG_KEY_LOCAL),
            gitAccess.getBranchMap(repo, ConfigConstants.CONFIG_KEY_REMOTE));
        int rh = getRowHeight(renderer, getFirstCommit(commitCharacteristicsVector));
        
        SwingUtilities.invokeLater(() -> {
          historyTable.setModel(new HistoryCommitTableModel(commitCharacteristicsVector));
          updateHistoryTableWidths();
          
          historyTable.setDefaultRenderer(
              CommitCharacteristics.class,
              renderer);
          historyTable.setDefaultRenderer(
              Date.class,
              new DateTableCellRenderer("d MMM yyyy HH:mm"));
          TableColumn authorColumn = historyTable.getColumn(translator.getTranslation(Tags.AUTHOR));
          authorColumn.setCellRenderer(createAuthorColumnRenderer());
          
          historyTable.setRowHeight(rh);
        });
        
        revisionDataUpdater = new RowHistoryTableSelectionListener(
            getUpdateDelay(),
            historyTable, 
            commitDescriptionPane, 
            commitCharacteristicsVector, 
            affectedFilesTable);
        historyTable.getSelectionModel().addListSelectionListener(revisionDataUpdater);

        // Install hyperlink listener.
        if (hyperlinkListener != null) {
          commitDescriptionPane.removeHyperlinkListener(hyperlinkListener);  
        }
        hyperlinkListener = new HistoryHyperlinkListener(historyTable, commitCharacteristicsVector);
        commitDescriptionPane.addHyperlinkListener(hyperlinkListener);

        // Select the local branch HEAD.
        if (!commitCharacteristicsVector.isEmpty()) {
          String fullBranch = repo.getFullBranch();
          Ref branchHead = repo.exactRef(fullBranch);
          if (branchHead != null) {
            ObjectId objectId = branchHead.getObjectId();
            if (objectId != null) {
              selectCommit(objectId);
            }
          }
        } else {
          PluginWorkspaceProvider.getPluginWorkspace().showInformationMessage(
              translator.getTranslation(Tags.GIT_HISTORY) + ": "
                  + StringUtils.toLowerCase(translator.getTranslation(Tags.NOTHING_TO_SHOW_FOR_NEW_FILES)));
        }

      } catch (NoRepositorySelected | IOException e) {
        LOGGER.debug(e, e);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to present history because of: " + e.getMessage());
      }
    } else {
      if (historyTable.getModel().getRowCount() == 0) {
        PluginWorkspaceProvider.getPluginWorkspace().showInformationMessage(
            translator.getTranslation(Tags.GIT_HISTORY) + ": "
                + StringUtils.toLowerCase(translator.getTranslation(Tags.NOTHING_TO_SHOW_FOR_NEW_FILES)));
      }
    }
  }

  /**
   * Gets the tags from the current repository.
   * 
   * @param repo Git repository.
   * 
   * @return The tags or an empty map. Never null.
   */
  private Map<String, List<String>> getTagMap(Repository repo) {
    Map<String, List<String>> tagMap = new HashMap<>();
    try {
      tagMap = gitAccess.getTagMap(repo);
    } catch (GitAPIException | IOException e) {
      LOGGER.debug(e, e);
    }
    
    return tagMap;
  }

  /**
   * Gets the preferred height needed to render the commit information.
   *  
   * @param renderer Commit message renderer.
   * @param ff Commit to render.
   * 
   * @return The preferred row height.
   */
  private int getRowHeight(CommitMessageTableRenderer renderer, CommitCharacteristics ff) {
    Component tableCellRendererComponent = renderer.getTableCellRendererComponent(
        historyTable, 
        ff, 
        false, false, 1, 1);
    
    int rowHeight = historyTable.getRowHeight();
    if (rowHeight < tableCellRendererComponent.getPreferredSize().height) {
      rowHeight = tableCellRendererComponent.getPreferredSize().height;
    }
    
    return rowHeight;
  }

  /**
   * Gets the first actually commit from the list of commits. It ignores the {@link GitAccess.UNCOMMITED_CHANGES} entry.
   * 
   * @param commitCharacteristics A list with commits from the repository.
   * 
   * @return The top actual commit.
   */
  private CommitCharacteristics getFirstCommit(final List<CommitCharacteristics> commitCharacteristics) {
    Iterator<CommitCharacteristics> iterator = commitCharacteristics.iterator();
    CommitCharacteristics first = null;
    while (first == null && iterator.hasNext()) {
      CommitCharacteristics cc = iterator.next();
      
      if (cc != GitAccess.UNCOMMITED_CHANGES ) {
        first = cc;
      }
    }
    
    return first;
  }

  /**
   * Updates the selection model in the table to either single and multiple.
   * 
   * @param filePath An optional file to show the history for.
   */
  private void updateSelectionMode(String filePath) {
    if (filePath != null && filePath.length() > 0) {
      historyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    } else {
      historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
  }

  /**
   * @return A cell renderer for the author column.
   */
  private DefaultTableCellRenderer createAuthorColumnRenderer() {
    return new DefaultTableCellRenderer() { // NOSONAR
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
          boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = label.getText();
        int indexOfLT = text.indexOf(" <");
        if (indexOfLT != -1) {
          text = text.substring(0, indexOfLT);
        }
        label.setText(text);
        return label;
      }
    };
  }

  /**
   * Tries a fetch to update remote information.
   * 
   * @param gitAccess Git access.
   */
  private void tryFetch() {
    try {
      gitAccess.fetch();
    } catch (SSHPassphraseRequiredException | PrivateRepositoryException | RepositoryUnavailableException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e, e);
      }
    }
  }

  /**
   * Coalescing for selecting the row in HistoryTable.
   */
  static final int TIMER_DELAY = 500;
  
  /**
   * @return Milliseconds. Controls how fast the satellite views are updated after a new revision is selected.
   */
  protected int getUpdateDelay() {
    return TIMER_DELAY;
  }

  /**
   * Distribute widths to the columns according to their content.
   */
  private void updateHistoryTableWidths() {
    int dateColWidth = scaleColumnsWidth(100);
    int authorColWidth = scaleColumnsWidth(120);
    int commitIdColWidth = scaleColumnsWidth(80);
    
    TableColumnModel tcm = historyTable.getColumnModel();
    TableColumn column = tcm.getColumn(0);
    column.setPreferredWidth(historyTable.getWidth() - authorColWidth - authorColWidth - dateColWidth);
    
    column = tcm.getColumn(1);
    column.setPreferredWidth(dateColWidth);

    column = tcm.getColumn(2);
    column.setPreferredWidth(authorColWidth);

    column = tcm.getColumn(3);
    column.setPreferredWidth(commitIdColWidth);
  }
  
  /**
   * Applies a scaling factor depending if we are on a hidpi display.
   * 
   * @param width Width to scale.
   * 
   * @return A scaled width.
   */
  public static int scaleColumnsWidth(int width) {
    float scalingFactor = (float) 1.0;
    if (HiDPIUtil.isRetinaNoImplicitSupport()) {
      scalingFactor = HiDPIUtil.getScalingFactor();
    }
    
    return (int) (scalingFactor * width);
  }
  

  /**
   *  Shows the commit history for the given file.
   *  
   * @param filePath Path of the file, relative to the working copy.
   * @param activeRevCommit The commit to select in the view.
   */
  public void showCommit(String filePath, RevCommit activeRevCommit) {
    showHistory(filePath);
    if (activeRevCommit != null) {
      ObjectId id = activeRevCommit.getId();
      selectCommit(id);
    }
  }

  /**
   * Selects the commit with the given ID.
   * 
   * @param id Id of the repository to select.
   */
  private void selectCommit(ObjectId id) {
    SwingUtilities.invokeLater(() -> {
      HistoryCommitTableModel model =  (HistoryCommitTableModel) historyTable.getModel();
      List<CommitCharacteristics> commits = model.getAllCommits();
      for (int i = 0; i < commits.size(); i++) {
        CommitCharacteristics commitCharacteristics = commits.get(i);
        if (id.getName().equals(commitCharacteristics.getCommitId())) {
          final int sel = i;
          historyTable.scrollRectToVisible(historyTable.getCellRect(sel, 0, true));
          historyTable.getSelectionModel().setSelectionInterval(sel, sel);
          break;
        }
      }
    });
  }
  
  /**
   * Acts as an observer and listens for changes.
   */
  @Override
  public void stateChanged(PushPullEvent pushPullEvent) {
    if (isShowing() && pushPullEvent.getActionStatus() == ActionStatus.FINISHED) {
      refresh();
    }
  }
  
  /**
   * @return the table with the affected files.
   */
  public JTable getAffectedFilesTable() {
    return affectedFilesTable;
  }
  
  /**
   * @return the history table.
   */
  public JTable getHistoryTable() {
    return historyTable;
  }
}
