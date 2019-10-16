package com.oxygenxml.git.view.historycomponents;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.jidesoft.swing.JideSplitPane;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Equaler;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.hidpi.RetinaDetector;

/**
 * Presents the commits for a given resource. 
 */
public class HistoryPanel extends JPanel {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class);
  /**
   * Table view that presents the commits.
   */
  private JTable historyTable;
  private JEditorPane commitDescriptionPane;
  /**
   * Toolbar in which the button will be placed
   */
  private JToolBar toolbar;
  /**
   * The label that shows the resource for which we present the history.
   */
  private JLabel showCurrentRepoLabel;
  /**
   * Intercepts clicks in the commit details area.
   */
  private HistoryHyperlinkListener hyperlinkListener;
  /**
   * Commit selection listener that updates all the views with details.
   */
  private RowHistoryTableSelectionListener selectionListener;
  /**
   * The changed files from a commit.
   */
  private JTable changesTable;
  /**
   * The file path of the resource for which we are currently presenting the history. If <code>null</code>, we 
   * present the history for the entire repository.
   */
  private String activeFilePath;
  /**
   * Executes GIT commands.
   */
  private transient StageController stageController;
  
  /**
   * Constructor.
   * 
   * @param stageController Executes a set of Git commands.
   */
  public HistoryPanel(StageController stageController) {
    this.stageController = stageController;
    setLayout(new BorderLayout());

    historyTable = createTable();
    
    historyTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showHistoryContextualMenu(historyTable, e.getPoint());
        }
      }

      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showHistoryContextualMenu(historyTable, e.getPoint());
        }
      }
    });

    JScrollPane tableScrollPane = new JScrollPane(historyTable);
    historyTable.setFillsViewportHeight(true);

    // set Commit Description Pane with HTML content and hyperlink.
    commitDescriptionPane = new JEditorPane();
    init(commitDescriptionPane);

    historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    JScrollPane commitDescriptionScrollPane = new JScrollPane(commitDescriptionPane);
    
    changesTable = createResourcesTable();
    
    JScrollPane fileHierarchyScrollPane = new JScrollPane(changesTable);

    Dimension minimumSize = new Dimension(500, 150);
    commitDescriptionScrollPane.setPreferredSize(minimumSize);
    fileHierarchyScrollPane.setPreferredSize(minimumSize);


    //----------
    // Top panel
    //----------
    
    showCurrentRepoLabel = new JLabel();
    JPanel topPanel = new JPanel(new BorderLayout());
    createToolbar(topPanel);

    JPanel infoBoxesSplitPane = createSplitPane(JideSplitPane.HORIZONTAL_SPLIT, commitDescriptionScrollPane,
        fileHierarchyScrollPane);
    JideSplitPane centerSplitPane = createSplitPane(JideSplitPane.VERTICAL_SPLIT, tableScrollPane, infoBoxesSplitPane);
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

    add(centerSplitPane, BorderLayout.CENTER);
  }

  /**
   * Creates the view that presents the files changed in a revision.
   * 
   * @return The view that presents the files.
   */
  private JTable createResourcesTable() {
    JTable createResourcesTable = UIUtil.createResourcesTable(new StagingResourcesTableModel(null, true), () -> false);
    createResourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    createResourcesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showResourcesContextualMenu(createResourcesTable, e.getPoint());
        }
      }

      @Override
      public void mouseReleased(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
          showResourcesContextualMenu(createResourcesTable, e.getPoint());
        }
      }
    });
    
    return createResourcesTable;
  }
  
  /**
   * Show the contextual menu on the resources changed on a revision.
   * 
   * @param commitResourcesTable The table with the files from a committed on a revision.
   * @param point The point where to show the contextual menu.
   */
  protected void showResourcesContextualMenu(JTable commitResourcesTable, Point point) {
    int rowAtPoint = commitResourcesTable.rowAtPoint(point);
    if (rowAtPoint != -1) {
      commitResourcesTable.getSelectionModel().setSelectionInterval(rowAtPoint, rowAtPoint);
      
      StagingResourcesTableModel model = (StagingResourcesTableModel) commitResourcesTable.getModel();
      int convertedSelectedRow = commitResourcesTable.convertRowIndexToModel(rowAtPoint);
      FileStatus file = model.getFileStatus(convertedSelectedRow);
      
      JPopupMenu jPopupMenu = new JPopupMenu();
      
      HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
      CommitCharacteristics commitCharacteristics = historyTableModel.getCommitVector().get(historyTable.getSelectedRow());
      
      jPopupMenu.add(createOpenFileAction(commitCharacteristics.getCommitId(), file.getFileLocation()));
      
      
      populateDiffActions(jPopupMenu, commitCharacteristics, file);
      
      
      jPopupMenu.show(commitResourcesTable, point.x, point.y);
    }
  }
  
  /**
   * Show the contextual menu on the hisotry table.
   * 
   * @param commitResourcesTable The table with the files from a committed on a revision.
   * @param point The point where to show the contextual menu.
   */
  protected void showHistoryContextualMenu(JTable historyTable, Point point) {
    if (activeFilePath != null) {
      // If we present the history for a specific file.
      int rowAtPoint = historyTable.rowAtPoint(point);
      if (rowAtPoint != -1) {
        historyTable.getSelectionModel().setSelectionInterval(rowAtPoint, rowAtPoint);
        int convertedSelectedRow = historyTable.convertRowIndexToModel(rowAtPoint);

        JPopupMenu jPopupMenu = new JPopupMenu();

        HistoryCommitTableModel historyTableModel = (HistoryCommitTableModel) historyTable.getModel();
        CommitCharacteristics commitCharacteristics = historyTableModel.getCommitVector().get(convertedSelectedRow);
        jPopupMenu.add(createOpenFileAction(commitCharacteristics.getCommitId(), activeFilePath));

        try {
          List<FileStatus> changes = RevCommitUtil.getChangedFiles(commitCharacteristics.getCommitId());
          Optional<FileStatus> findFirst = changes.stream().filter(f -> activeFilePath.equals(f.getFileLocation())).findFirst();
          if (findFirst.isPresent()) {
            populateDiffActions(jPopupMenu, commitCharacteristics, findFirst.get());
          }
        } catch (IOException | GitAPIException e) {
          LOGGER.error(e, e);
        }

        jPopupMenu.show(historyTable, point.x, point.y);
      }
    }
  }

  /**
   * Creates an action to open a file at a given revision.
   * 
   * @param revisionID Revision ID.
   * @param filePath File path, relative to the working copy.
   * 
   * @return The action that will open the file when invoked.
   */
  private AbstractAction createOpenFileAction(String revisionID, String filePath) {
    return new AbstractAction(Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_OPEN)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          URL fileURL = GitRevisionURLHandler.encodeURL(revisionID, filePath);
          PluginWorkspaceProvider.getPluginWorkspace().open(fileURL);
        } catch (MalformedURLException e1) {
          LOGGER.error(e1, e1);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to open revision: " + e1.getMessage());
        } 
      }
    };
  }

  /**
   * Contributes the DIFF actions between the current revision and the previous ones on the contextual menu.
   * 
   * @param jPopupMenu Contextual menu.
   * @param commitCharacteristics Current commit data.
   * @param fileStatus File path do diff.
   */
  private void populateDiffActions(
      JPopupMenu jPopupMenu,  
      CommitCharacteristics commitCharacteristics,
      FileStatus fileStatus) {
    String filePath = fileStatus.getFileLocation();
    if (GitAccess.UNCOMMITED_CHANGES.getCommitId() != commitCharacteristics.getCommitId()) {
      // A revision.
      List<String> parents = commitCharacteristics.getParentCommitId();
      if (parents != null && !parents.isEmpty()) {
        try {
          RevCommit[] parentsRevCommits = RevCommitUtil.getParents(GitAccess.getInstance().getRepository(), commitCharacteristics.getCommitId());
          boolean addParentID = parents.size() > 1;
          for (RevCommit parentID : parentsRevCommits) {
            // Just one parent.
            jPopupMenu.add(createDiffAction(filePath, commitCharacteristics.getCommitId(), parentID, addParentID));
          }
          
          jPopupMenu.add(new AbstractAction(
              Translator.getInstance().getTranslation(Tags.COMPARE_WITH_WORKING_TREE_VERSION)) {
            @Override
            public void actionPerformed(ActionEvent e) {
              try {
                DiffPresenter.showTwoWayDiffWithLocal(filePath, commitCharacteristics.getCommitId());
              } catch (MalformedURLException | NoRepositorySelected e1) {
                PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to compare: " + e1.getMessage());
                LOGGER.error(e1, e1);
              }
            }
          });
        } catch (IOException | NoRepositorySelected e2) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to compare: " + e2.getMessage());
          LOGGER.error(e2, e2);
        }
      }
    } else {
      // Uncommitted changes. Compare between local and HEAD.
      jPopupMenu.add(new AbstractAction(
          Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          DiffPresenter.showDiff(fileStatus, stageController);
        }
      });
    }
  }

  /**
   * Creates an action that invokes Oxygen's DIFF.
   * 
   * @param filePath File to compare. Path relative to the working tree.
   * @param commitID The current commit id. First version to compare.
   * @param parentRevCommit The parent revision. Second version to comapre.
   * @param addParentIDInActionName <code>true</code> to put the ID of the parent version in the action's name.
   * 
   * @return The action that invokes the DIFF.
   */
  private AbstractAction createDiffAction(
      String filePath,
      String commitID, 
      RevCommit parentRevCommit,
      boolean addParentIDInActionName) {
    String translation = Translator.getInstance().getTranslation(Tags.COMPARE_WITH_PREVIOUS_VERSION);
    if (addParentIDInActionName) {
      translation += " " + parentRevCommit.abbreviate(7).name();
    }
    return new AbstractAction(translation) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          DiffPresenter.showTwoWayDiff(commitID, parentRevCommit.name(), filePath);
        } catch (MalformedURLException e1) {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to compare: " + e1.getMessage());
          LOGGER.error(e1, e1);
        }
      }
    };
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
  private static void init(JEditorPane editorPane) {
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
  private void createToolbar(JPanel topPanel) {
    toolbar = new JToolBar();
    toolbar.setOpaque(false);
    toolbar.setFloatable(false);
    topPanel.add(showCurrentRepoLabel, BorderLayout.WEST);
    topPanel.add(toolbar, BorderLayout.EAST);
    
    Action refreshAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showHistory(activeFilePath, true);
      }
    };
    refreshAction.putValue(Action.SMALL_ICON, Icons.getIcon(Icons.REFRESH_ICON));
    refreshAction.putValue(Action.SHORT_DESCRIPTION, "refresh");
    ToolbarButton refreshButton = new ToolbarButton(refreshAction, false);
    toolbar.add(refreshButton);
    
    add(topPanel, BorderLayout.NORTH);
  }

  /**
   * Tries to use Oxygen's API to create a table.
   * 
   * @return An Oxygen's API table or a generic one if we run into an old Oxygen.
   */
  private JTable createTable() {
    JTable table = null;
    try {
      Class tableClass = Class.forName("ro.sync.exml.workspace.api.standalone.ui.Table");
      Constructor tableConstructor = tableClass.getConstructor();
      table = (JTable) tableConstructor.newInstance();
    } catch (Exception e) {
      // Running in an Oxygen version that lacks this API.
      table = new JTable();
    }
    
    return table;
  }

  /**
   * Shows the commit history for the entire repository.
   */
  public void showRepositoryHistory() {
    showHistory(null, true);
  }
  
  /**
   * Shows the commit history for the entire repository.
   * 
   * @param filePath File for which to present the commit that changed him.
   */
  public void showHistory(String filePath) {
    showHistory(filePath, false);
  }

  /**
   * Shows the commit history for the entire repository.
   * 
   * @param filePath File for which to present the commit that changed him.
   * @param force <code>true</code> to recompute the hisotry data even if the view already presents the history for the given resource.
   */
  private void showHistory(String filePath, boolean force) {
    // Check if we don't already present the history for this path!!!!
    if (force || !Equaler.verifyEquals(filePath, activeFilePath)) {
      this.activeFilePath = filePath;
      GitAccess gitAccess = GitAccess.getInstance();

      try {
        // Make sure we know about the remote as well, to present data about the upstream branch.
        gitAccess.fetch();

        File directory = gitAccess.getWorkingCopy();
        if (filePath != null) {
          directory = new File(directory, filePath);
        }
        showCurrentRepoLabel.setText(
            Translator.getInstance().getTranslation(Tags.SHOWING_HISTORY_FOR) + " " + directory.getName());
        showCurrentRepoLabel.setToolTipText(directory.getAbsolutePath());
        showCurrentRepoLabel.setBorder(BorderFactory.createEmptyBorder(0,2,5,0));

        historyTable.setDefaultRenderer(CommitCharacteristics.class, new CommitMessageTableRenderer(gitAccess, gitAccess.getRepository()));
        historyTable.setDefaultRenderer(Date.class, new DateTableCellRenderer("d MMM yyyy HH:mm"));

        // Install selection listener.
        if (selectionListener != null) {
          historyTable.getSelectionModel().removeListSelectionListener(selectionListener);
        }
        
        List<CommitCharacteristics> commitCharacteristicsVector = gitAccess.getCommitsCharacteristics(filePath);

        historyTable.setModel(new HistoryCommitTableModel(commitCharacteristicsVector));

        updateTableWidths();

        selectionListener = new RowHistoryTableSelectionListener(historyTable, commitDescriptionPane, commitCharacteristicsVector, changesTable);
        historyTable.getSelectionModel().addListSelectionListener(selectionListener);

        // Install hyperlink listener.
        if (hyperlinkListener != null) {
          commitDescriptionPane.removeHyperlinkListener(hyperlinkListener);  
        }

        hyperlinkListener = new HistoryHyperlinkListener(historyTable, commitCharacteristicsVector);
        commitDescriptionPane.addHyperlinkListener(hyperlinkListener);

        // Select the local branch HEAD.
        if (!commitCharacteristicsVector.isEmpty()) {
          Repository repository = gitAccess.getRepository();
          String fullBranch = repository.getFullBranch();
          Ref branchHead = repository.exactRef(fullBranch);
          ObjectId objectId = branchHead.getObjectId();
          selectCommit(objectId);
        }

      } catch (NoRepositorySelected | SSHPassphraseRequiredException | PrivateRepositoryException | RepositoryUnavailableException | IOException e) {
        LOGGER.debug(e, e);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Unable to present history because of: " + e.getMessage());
      }
    }
  }

  /**
   * Distribute widths to the columns according to their content.
   */
  private void updateTableWidths() {
    TableColumnModel tcm = historyTable.getColumnModel();
    int available = historyTable.getWidth();
    TableColumn column = tcm.getColumn(0);
    int columnDate = scaleColumnsWidth(100);
    int columnAuthor = scaleColumnsWidth(120);
    int columnCommitId = scaleColumnsWidth(80);
    
    column.setPreferredWidth(available - columnAuthor - columnAuthor - columnDate);
    
    column = tcm.getColumn(1);
    column.setPreferredWidth(columnDate);

    column = tcm.getColumn(2);
    column.setPreferredWidth(columnAuthor);

    column = tcm.getColumn(3);
    column.setPreferredWidth(columnCommitId);
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
    if (RetinaDetector.getInstance().isRetinaNoImplicitSupport()) {
      scalingFactor = RetinaDetector.getInstance().getScalingFactor();
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
    HistoryCommitTableModel model =  (HistoryCommitTableModel) historyTable.getModel();
    List<CommitCharacteristics> commitVector = model.getCommitVector();
    for (int i = 0; i < commitVector.size(); i++) {
      CommitCharacteristics commitCharacteristics = commitVector.get(i);

      if (id.getName().equals(commitCharacteristics.getCommitId())) {
        final int sel = i;
        SwingUtilities.invokeLater(() -> {
          historyTable.scrollRectToVisible(historyTable.getCellRect(sel, 0, true));
          historyTable.getSelectionModel().setSelectionInterval(sel, sel);
        });
        break;
      }
    }
  }
}
