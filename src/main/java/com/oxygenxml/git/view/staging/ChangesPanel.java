package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import javax.xml.bind.annotation.XmlEnum;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PlatformDetectionUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.util.HiDPIUtil;
import com.oxygenxml.git.view.util.TreeUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;
import ro.sync.exml.workspace.api.standalone.ui.Tree;

/**
 * This is the staging or the unstaging area, depending on the forStaging
 * variable (<code>true</code> if this model presents un-staged resources that
 * will be staged. <code>false</code> if this model presents staged resources
 * that will be unstaged).
 * 
 * 
 * @author Beniamin Savu
 */
public class ChangesPanel extends JPanel {

  /**
   * Provides the selected resources, sometimes filtered.
   */
  public interface SelectedResourcesProvider {
    /**
     * For the tree mode, get only the selected leaves.
     * For the table/flat view, get all selected resources,
     * because all are, so to say, "leaves".
     * 
     * @return The directly selected resources.
     */
    List<FileStatus> getOnlySelectedLeaves();
    /**
     * Get all the selected resources, including the ones
     * from inside folders. Never <code>null</code>.
     * 
     * @return All selected resources. Either directly or indirectly, thorugh parent selection.
     */
    List<FileStatus> getAllSelectedResources();
  }

  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(ChangesPanel.class);
  
  /**
   * The current view name.
   */
  @XmlEnum
  public enum ResourcesViewMode {
    /**
     * Flat (list) view.
     */
    FLAT_VIEW,
    /**
     * Tree view.
     */
    TREE_VIEW;
  }

	/**
	 * Button that stages/unstages all files
	 */
	private JButton changeAllButton;

	/**
	 * Button that stages/unstages selected files
	 */
	private JButton changeSelectedButton;

	/**
	 * Button that switches the view form flat to tree or from tree to flat
	 */
	private JButton switchViewButton;

	/**
	 * Used to present either the tree view or the flat view
	 */
	private JScrollPane scrollPane;

	/**
	 * Table that stores the files
	 */
	private JTable filesTable;

	/**
	 * Tree that stores the files
	 */
	private JTree tree = null;

	/**
	 * Used to fire an event
	 */
	private GitControllerBase gitController;

	/**
	 * Shows whether or not this is the panel for staged or unstaged resources. 
	 */
	private boolean forStagedResources;

	/**
	 * The active view in the scroll pane
	 */
	private ResourcesViewMode currentViewMode;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator = Translator.getInstance();

	/**
	 * <code>true</code> if the contextual menu is showing for the resources in the tree view.
	 */
	private boolean isContextMenuShowing = false;
	/**
	 * History interface.
	 */
  private HistoryController historyController;

	/**
	 * Constructor.
	 * 
	 * @param gitController       Git controller.
	 * @param historyController   History interface.
	 * @param forStagedResources  <code>true</code> if for staged resources.
	 */
	public ChangesPanel(
	    GitControllerBase gitController, 
	    HistoryController historyController, 
	    boolean forStagedResources) {
		this.historyController = historyController;
		this.forStagedResources = forStagedResources;
		this.gitController = gitController;
		
		tree = createTree();
		// ==== EXM-41138 hack: expand/collapse on second mouse released ====
		tree.setDragEnabled(true);
		// ==================================================================
		ToolTipManager.sharedInstance().registerComponent(tree);
		this.currentViewMode = forStagedResources ? OptionsManager.getInstance().getStagedResViewMode()
		    : OptionsManager.getInstance().getUntagedResViewMode();
		
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          // TODO Disable widgets to avoid unwanted actions.
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        switch (operation) {
          case OPEN_WORKING_COPY:
            // TODO Enable widgets.

            // The event might come too early.
            GitAccess gitAccess = GitAccess.getInstance();
            try {
              Repository repository = gitAccess.getRepository();
              if (repository != null) {
                
                gitController.asyncTask(
                    () -> {
                      if (forStagedResources) {
                        return gitAccess.getStagedFiles();
                      } else {
                        return gitAccess.getUnstagedFiles();
                      }
                    }, 
                    this::refresh, 
                    ex -> refresh(Collections.emptyList()));
              }
            } catch (NoRepositorySelected ex) {
              logger.debug(ex, ex);
              ChangesPanel.this.repositoryChanged(Collections.emptyList());
            }
          
            break;
          case STAGE:
          case UNSTAGE:
          case COMMIT:
          case DISCARD:
          case MERGE_RESTART:
          case ABORT_MERGE:
          case ABORT_REBASE:
          case CONTINUE_REBASE:
            SwingUtilities.invokeLater(() -> ChangesPanel.this.fileStatesChanged(info));
            break;
          default:
            break;
        }
      }
      /**
       * Update models with newly detected files.
       * 
       * @param files Newly detected files.
       */
      private void refresh(List<FileStatus> files) {
        SwingUtilities.invokeLater(() -> {
          ChangesPanel.this.repositoryChanged(files);
          toggleSelectedButton();
        }); 
      }
      
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          // TODO Enable widgets
        }
      }
    });
	}

	JTable getFilesTable() {
		return filesTable;
	}
	
	/**
	 * @return The tree that renders resources.
	 */
	JTree getTreeView() {
    return tree;
  }
	
	 /**
   * @return The Git files in the model.
   */
	public List<FileStatus> getFilesStatuses() {
	  if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
	    return ((StagingResourcesTableModel) filesTable.getModel()).getFilesStatuses();
	  } else {
	    return ((StagingResourcesTreeModel) tree.getModel()).getFilesStatuses();
	  }
  }

	/**
	 * Generate a tree structure with the given files. The given path is used to
	 * get the root for the structure that will be generated from the files. An
	 * example will be: if path = "C:/test/repository" the root node name will be
	 * "repository", then the files will pe placed under this root
	 * 
	 * @param filesStatus
	 *          - files to generate the nodes
	 */
	private void updateTreeView(List<FileStatus> filesStatus) {
	  if (tree != null) {
	    Enumeration<TreePath> expandedPaths = TreeUtil.getLastExpandedPaths(tree);
	    TreePath[] selectionPaths = tree.getSelectionPaths();

	    // Create the tree with the new model
	    tree.setModel(
	        new StagingResourcesTreeModel(
	            gitController, 
	            GitAccess.getInstance().getWorkingCopyName(), 
	            forStagedResources, 
	            filesStatus));

	    // restore last expanded paths after refresh
	    TreeUtil.restoreLastExpandedPaths(expandedPaths, tree);
	    tree.setSelectionPaths(selectionPaths);
	  }
	}

	/**
	 * Updates the flat view with the given files. Also if in the view some of the
	 * files were selected, the selection is preserved.
	 * 
	 * @param unstagedFiles
	 *          - the new files to update the flat view
	 */
	private void updateFlatView(List<FileStatus> unstagedFiles) {
	  if (filesTable != null) {
	    StagingResourcesTableModel modelTable = (StagingResourcesTableModel) filesTable.getModel();
	    List<FileStatus> selectedFiles = getTableSelectedFiles();
	    modelTable.setFilesStatus(unstagedFiles);

	    restoreTableSelection(modelTable, selectedFiles);
	    selectedFiles.clear();
	  }
	}
	
	 /**
   * Updates the flat view with the given files. Also if in the view some of the
   * files were selected, the selection is preserved.
   * 
   * @param newFiles The new files to update in the flat view.
   */
  public void update(List<FileStatus> newFiles) {
    if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
      updateFlatView(newFiles);
    } else {
      updateTreeView(newFiles);
    }
  }
  
	
	/**
	 * Notify the models about the change.
	 * 
	 * @param changeEvent Change event.
	 */
	void fileStatesChanged(GitEventInfo changeEvent) {
	  if (currentViewMode == ResourcesViewMode.FLAT_VIEW && filesTable != null) {
	    StagingResourcesTableModel modelTable = (StagingResourcesTableModel) filesTable.getModel();
	    modelTable.stateChanged(changeEvent);
	  } else if (currentViewMode == ResourcesViewMode.TREE_VIEW && tree != null) {
	    Enumeration<TreePath> expandedPaths = TreeUtil.getLastExpandedPaths(tree);
	    TreePath[] selectionPaths = tree.getSelectionPaths();

	    StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
	    treeModel.fileStatesChanged(changeEvent);

	    // Restore last expanded paths after refresh
	    TreeUtil.restoreLastExpandedPaths(expandedPaths, tree);
	    tree.setSelectionPaths(selectionPaths);
	  }
	  
    toggleSelectedButton();
  }

	/**
	 * Restores the last selected files in the table view
	 * 
	 * @param model
	 *          - the table model
	 * @param previouslySelectedFiles
	 *          - previously selected files to restore
	 */
	private void restoreTableSelection(StagingResourcesTableModel model, List<FileStatus> previouslySelectedFiles) {
		for (FileStatus fileStatus : previouslySelectedFiles) {
			int row = model.getRow(fileStatus.getFileLocation());
			if (row != -1) {
				filesTable.addRowSelectionInterval(row, row);
			}
		}
	}

	/**
	 * Return the table selected files
	 * 
	 * @return table selected files
	 */
	private List<FileStatus> getTableSelectedFiles() {
		List<FileStatus> selectedFiles = new ArrayList<>();
		int[] selectedRows = null;
		StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
		selectedRows = filesTable.getSelectedRows();
		if (selectedRows != null) {
			for (int i = 0; i < selectedRows.length; i++) {
				int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
				selectedFiles.add(model.getUnstageFile(convertedRow));
			}
		}
		return selectedFiles;
	}

	/**
	 * Creates the components and adds listeners to some of them. Basically this
	 * creates the panel
	 */
	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		// UI
		addTopPanel(gbc);
		addFilesPanel(gbc);

		// Listeners
		addTreeMouseListener();
		addTreeExpandListener();
		
		// Compare file with last commit when enter is pressed.
		tree.addKeyListener(new KeyAdapter() {
		  @Override
		  public void keyReleased(KeyEvent e) {
		    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
		      treatEnterKeyPressedOnTreeSelection();
		    } else if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
		      treatContextMenuKeyPressedOnTreeSelection();
		    }
		  }
		});

		int minWidth = HiDPIUtil.isRetinaNoImplicitSupport() 
		    ? (int) (UIConstants.MIN_PANEL_WIDTH * HiDPIUtil.getScalingFactor())
		    : UIConstants.MIN_PANEL_WIDTH;
		this.setMinimumSize(new Dimension(minWidth, UIConstants.STAGING_PANEL_MIN_HEIGHT));
	}
	
	private void addTopPanel(GridBagConstraints gbc) {
	  JPanel topPanel = new JPanel(new GridBagLayout());
	  
	  
	  // Label
	  JLabel label = new JLabel(forStagedResources ? translator.getTranslation(Tags.STAGED_FILES) + ":" 
	      : translator.getTranslation(Tags.UNSTAGED_FILES) + ":");
	  GridBagConstraints c = new GridBagConstraints();
	  c.gridx = 0;
	  c.gridy = 0;
	  c.weightx = 1;
	  c.fill = GridBagConstraints.HORIZONTAL;
	  c.anchor = GridBagConstraints.WEST;
	  topPanel.add(label, c);
	  
	  // Toolbar
	  JToolBar actionsToolbar = new JToolBar();
    actionsToolbar.setOpaque(false);
    actionsToolbar.setFloatable(false);

    createTopPanelToolbarButtons();
    actionsToolbar.add(changeSelectedButton);
    actionsToolbar.add(changeAllButton);
    actionsToolbar.addSeparator();
    actionsToolbar.add(switchViewButton);
    
    c.gridx ++;
    c.fill = GridBagConstraints.NONE;
    c.weightx = 0;
    c.anchor = GridBagConstraints.EAST;
    topPanel.add(actionsToolbar, c);
    
    // Add top panel
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING,
        0,
        UIConstants.COMPONENT_RIGHT_PADDING - 1);
    add(topPanel, gbc);
  }

	/**
	 * Create the buttons for the toolbar in the top panel.
	 */
  private void createTopPanelToolbarButtons() {
    // Change selected
    String changeSelectedTranslationTag = forStagedResources ? Tags.UNSTAGE_SELECTED_BUTTON_TEXT 
        : Tags.STAGE_SELECTED_BUTTON_TEXT;
    String changeSelectedIconTag = forStagedResources ? Icons.UNSTAGE_SELECTED : Icons.STAGE_SELECTED;
    changeSelectedButton = OxygenUIComponentsFactory.createToolbarButton(
        new AbstractAction(
            translator.getTranslation(changeSelectedTranslationTag),
            Icons.getIcon(changeSelectedIconTag)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            List<FileStatus> fileStatuses = new ArrayList<>();
            if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
              int[] selectedRows = filesTable.getSelectedRows();
              StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
              for (int i = selectedRows.length - 1; i >= 0; i--) {
                int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
                FileStatus fileStatus = fileTableModel.getFileStatus(convertedRow);
                fileStatuses.add(fileStatus);
              }
            } else {
              List<String> selectedFiles = TreeUtil.getStringComonAncestor(tree);
              StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
              List<FileStatus> fileStatusesForPaths = fileTreeModel.getFilesByPaths(selectedFiles);
              fileStatuses.addAll(fileStatusesForPaths);
            }

            // "Stage"/"Unstage" actions
            AbstractAction stageUnstageAction = new StageUnstageResourceAction(
                fileStatuses, 
                !forStagedResources, 
                gitController);
            stageUnstageAction.actionPerformed(null);

            changeSelectedButton.setEnabled(false);
          }
        },
    false);
    changeSelectedButton.setEnabled(false);
    
    // Change all
    String changeAllTranslationTag = forStagedResources ? Tags.UNSTAGE_ALL_BUTTON_TEXT 
        : Tags.STAGE_ALL_BUTTON_TEXT;
    String changeAllIconTag = forStagedResources ? Icons.UNSTAGE_ALL : Icons.STAGE_ALL;
    changeAllButton = OxygenUIComponentsFactory.createToolbarButton(
        new AbstractAction(
            translator.getTranslation(changeAllTranslationTag),
            Icons.getIcon(changeAllIconTag)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
              StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
              fileTableModel.switchAllFilesStageState();
            } else {
              StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
              treeModel.switchAllFilesStageState();
            }
          }
        },
        false);
    changeAllButton.setEnabled(true);
    
    // Switch view mode
    String iconType = currentViewMode == ResourcesViewMode.FLAT_VIEW ? Icons.TREE_VIEW : Icons.LIST_VIEW;
    switchViewButton = OxygenUIComponentsFactory.createToolbarButton(
        new AbstractAction(null, Icons.getIcon(iconType)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            setResourcesViewMode(currentViewMode == ResourcesViewMode.FLAT_VIEW ? 
                ResourcesViewMode.TREE_VIEW : ResourcesViewMode.FLAT_VIEW);
            isContextMenuShowing = false;
          }
        }, 
        false);
    switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
  }
	
	/**
   * Adds the scollPane to the panel
   * 
   * @param gbc
   *          - the constraints used for this component
   */
  private void addFilesPanel(GridBagConstraints gbc) {
    gbc.insets = new Insets(
        0,
        UIConstants.COMPONENT_LEFT_PADDING,
        0,
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1;
    gbc.weighty = 1;
    
    filesTable = UIUtil.createResourcesTable(
        new StagingResourcesTableModel(gitController, forStagedResources),
        ()-> isContextMenuShowing);

    filesTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        toggleSelectedButton();
      }
    });
    
    // Adds mouse listener on the table: When the user right clicks on an item
    // in the table, a
    // contextual menu will pop. Also when the user double clicks on a leaf node
    // an action will occur depending on it's file status. If the status is
    // MODIFY,
    // the open in compare editor will be executed, if the status is Add the
    // file
    // will be opened in the Oxygen.
    filesTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // For MacOS the popup trigger comes on mouse pressed.
        handleContexMenuEvent(e);
      }
      
      @Override
      public void mouseReleased(MouseEvent e) {
        // Switching between Staged and UnStaged with right click introduced some paint artifacts.
        filesTable.requestFocus();
        filesTable.repaint();
        
        // Maybe the event is a pop-up trigger
        handleContexMenuEvent(e);
        // Maybe (not pop-up trigger) double click
        if (!e.isPopupTrigger() && e.getClickCount() == 2) {
          Point point = new Point(e.getX(), e.getY());
          int clickedRow = filesTable.rowAtPoint(point);
          if (clickedRow != -1) {
            openFileInCompareEditor(clickedRow);
          }
        }
      }

      /**
       * Present the contextual menu if this is the proper event.
       * 
       * @param e Mouse event.
       */
      private void handleContexMenuEvent(MouseEvent e) {
        if (e.isPopupTrigger() && e.getClickCount() == 1) {
          Point point = new Point(e.getX(), e.getY());
          int clickedRow = filesTable.rowAtPoint(point);
          int[] selectedRows = filesTable.getSelectedRows();
          if (clickedRow != -1) {
            // Might be a right click over a non-selected row. 
            boolean inSelection = false;
            for (int i = 0; i < selectedRows.length; i++) {
              if (clickedRow == selectedRows[i]) {
                inSelection = true;
                break;
              }
            }

            if (!inSelection) {
              filesTable.setRowSelectionInterval(clickedRow, clickedRow);
              selectedRows = filesTable.getSelectedRows();
            }
          }
          
          if (selectedRows.length == 0) {
            // When resolving a conflict "using mine" and there are no more entries in the tables,
            // show the contextual menu for being able to restart the merging
            if (filesTable.getRowCount() == 0 && isMergingResolved()) {
              showContextualMenuForFlatView(e.getX(), e.getY(), new int[0]);
            }
          } else {
            // ======== RIGHT CLICK ==========
            showContextualMenuForFlatView(e.getX(), e.getY(), selectedRows);
          }
        }
      }

    });
    
    filesTable.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
          int[] selectedRows = filesTable.getSelectedRows();
          if (selectedRows.length > 0) {
            Rectangle cellRect = filesTable.getCellRect(selectedRows[selectedRows.length - 1], 0, true);
            showContextualMenuForFlatView(cellRect.x, cellRect.y + cellRect.height, selectedRows);
          }
        }
      }
    });
    
    // Compare files on enter.
    filesTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
    filesTable.getActionMap().put("Enter", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int row = filesTable.convertRowIndexToModel(filesTable.getSelectedRow());
        if (row != -1) {
          openFileInCompareEditor(row);
        }
      }
    });
    
    scrollPane = new JScrollPane(filesTable);
    scrollPane.add(tree);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setPreferredSize(new Dimension(200, PlatformDetectionUtil.isMacOS() ? 160 : 220));
    UIUtil.setDefaultScrollPaneBorder(scrollPane);
    filesTable.setFillsViewportHeight(true);
    this.add(scrollPane, gbc);
    
    setResourcesViewMode(currentViewMode);
  }

  /**
	 * Context menu key was pressed when having something selected in the resources tree. Treat the event.
	 */
	private void treatContextMenuKeyPressedOnTreeSelection() {
    // Show context menu
    TreePath[] treePaths = tree.getSelectionPaths();
    if (treePaths != null && treePaths.length > 0) {
      TreePath lastTreePath = treePaths[treePaths.length - 1];
      Rectangle pathBounds = tree.getPathBounds(lastTreePath);
      if (pathBounds != null) {
        showContextualMenuForTree(
            pathBounds.x,
            pathBounds.y + pathBounds.height,
            (StagingResourcesTreeModel) tree.getModel());
      }
    }
  }

	/**
	 * Enter key was pressed when having something selected in the resources tree. Treat the event. 
	 */
  private void treatEnterKeyPressedOnTreeSelection() {
    TreePath pathForRow = null;
    int[] selectionRows = tree.getSelectionRows();
    if (selectionRows != null && selectionRows.length > 0) {
      pathForRow = tree.getPathForRow(selectionRows[selectionRows.length - 1]);
    }
    if (pathForRow != null) {
      StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
      String stringPath = TreeUtil.getStringPath(pathForRow);
      GitTreeNode node = TreeUtil.getTreeNodeFromString(model, stringPath);
      if (model != null && node != null
          && model.isLeaf(node) && !model.getRoot().equals(node)) {
        FileStatus file = model.getFileByPath(stringPath);
        DiffPresenter.showDiff(file, gitController);
      }
    }
  }

	/**
	 * Adds an expand listener to the tree: When the user expands a node the node
	 * will expand as long as it has only one child.
	 */
	private void addTreeExpandListener() {
		tree.addTreeExpansionListener(new TreeExpansionListener() {
		  @Override
			public void treeExpanded(TreeExpansionEvent event) {
		    TreeUtil.expandSingleChildPath(tree, event);
		  }
		  @Override
			public void treeCollapsed(TreeExpansionEvent event) {
		    // Nothing
			}
		});
	}

	/**
	 * Adds a mouse listener to the tree: When the user right clicks on a node, a
	 * contextual menu will pop. Also when the user double clicks on a leaf node
	 * an action will occur depending on it's file status. If the status is MODIFY
	 * the open in compare editor will be executed, if the status is Add the file
	 * will be opened in the Oxygen
	 */
	private void addTreeMouseListener() {
	  tree.getSelectionModel().addTreeSelectionListener(e -> toggleSelectedButton());
	  
	  tree.addMouseListener(new MouseAdapter() {
	    @Override
	    public void mousePressed(MouseEvent e) {
	      // For MacOS the popup trigger comes on mouse pressed.
	      handleContextualMenuEvent(e);
	    }

	    @Override
	    public void mouseReleased(MouseEvent e) {
	      // Switching between Staged and UnStaged with right click introduced some paint artifacts. 
	      tree.requestFocus();
	      tree.repaint();

	      // Maybe the event was a (not pop-up trigger) double-click
	      showDiff(e);
	      // Or maybe it was a right click
	      handleContextualMenuEvent(e);
	    }

	    /**
	     * Shows the contextual menu, if the mouse event is a pop-up trigger.
	     * 
	     * @param e Mouse event.
	     */
	    private void handleContextualMenuEvent(MouseEvent e) {
	      if (e.isPopupTrigger() && e.getClickCount() == 1) {
	        // ============= Right click event ================
	        // First, check the node under the mouse.
	        TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
	        if (treePath != null) {
	          boolean treeInSelection = false;
	          TreePath[] paths = tree.getSelectionPaths();
	          // The node under the mouse might not be the selected one.
	          // A JTree only updates selection for a left button. Also do it for a right click.
	          if (paths != null) {
	            for (int i = 0; i < paths.length; i++) {
	              if (treePath.equals(paths[i])) {
	                treeInSelection = true;
	                break;
	              }
	            }
	          }
	          if (!treeInSelection) {
	            tree.setSelectionPath(treePath);
	          }
	        } else {
	          // A click outside the tree. Go with a selected path.
	          treePath = tree.getSelectionPath();
	        }

	        if (treePath != null) {
	          String stringPath = TreeUtil.getStringPath(treePath);
	          StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
	          GitTreeNode node = TreeUtil.getTreeNodeFromString(model, stringPath);

	          if (node != null 
	              && (!node.isRoot() 
	                  || node.children().hasMoreElements()
	                  || isMergingResolved())) {
	            showContextualMenuForTree(e.getX(), e.getY(), model);
	          }
	        }
	      }
	    }

	    /**
	     * Shows DIFF for a double click mouse event.
	     * 
	     * @param e Mouse event.
	     */
	    private void showDiff(MouseEvent e) {
	      if (!e.isPopupTrigger() && e.getClickCount() == 2) {
	        // ============= Double click event ==============
	        TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
	        if (treePath != null) {
	          String stringPath = TreeUtil.getStringPath(treePath);
	          StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
	          GitTreeNode node = TreeUtil.getTreeNodeFromString(model, stringPath);
	          if (model.isLeaf(node) && !model.getRoot().equals(node)) {
	            FileStatus file = model.getFileByPath(stringPath);
	            DiffPresenter.showDiff(file, gitController);
	          }
	        }
	      }
	    }
	  });
	}
	
	/**
	 * Check if the merging has been resolved.
	 * 
	 * @return <code>true</code> if the merging has been resolved.
	 */
	private boolean isMergingResolved() {
	  RepositoryState repositoryState = RepoUtil.getRepoState();
    return repositoryState != null && repositoryState == RepositoryState.MERGING_RESOLVED;
  }
	
  /**
   * Show contextual menu
   * 
   * @param x         X coordinate where to show.
   * @param y         Y coordinate where to show.
   * @param model     The model of the tree.
   */
  private void showContextualMenuForTree(int x, int y, final StagingResourcesTreeModel model) {
    final List<String> selPaths = TreeUtil.getStringComonAncestor(tree);
    GitResourceContextualMenu contextualMenu = new GitResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return model.getFileLeavesByPaths(selPaths);
          }
          @Override
          public List<FileStatus> getAllSelectedResources() {
            return model.getFilesByPaths(selPaths);
          }
        },
        gitController,
        historyController,
        forStagedResources,
        RepoUtil.getRepoState());
    contextualMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        isContextMenuShowing = true;
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        isContextMenuShowing = false;
        contextualMenu.removePopupMenuListener(this);
      }
      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        isContextMenuShowing = false;
      }
    });
    contextualMenu.show(tree, x, y);
  }
	
	/**
	 * Set the current view mode for the resources: tree or table.
	 * 
	 * @param viewMode The new view mode.
	 */
	void setResourcesViewMode(ResourcesViewMode viewMode) {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Switch to " + viewMode);
	  }
	  
	  this.currentViewMode = viewMode;
	  
	  if (viewMode == ResourcesViewMode.TREE_VIEW) {
	    StagingResourcesTableModel modelTable = (StagingResourcesTableModel) filesTable.getModel();
	    List<FileStatus> filesStatuses = modelTable.getFilesStatuses();
	    
	    if (logger.isDebugEnabled()) {
	      logger.debug("Table model " + filesStatuses);
	    }
	    
	     // Create the tree with the new model
      tree.setModel(
          new StagingResourcesTreeModel(
              gitController, 
              GitAccess.getInstance().getWorkingCopyName(), 
              forStagedResources, 
              filesStatuses));
	    
	    restoreSelectedPathsFromTableToTree();
	    
	    // Activate the tree view.
	    scrollPane.setViewportView(tree);
	  } else {
	    
	    // Get the list of files from the tree model and update the table.
	    StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
	    List<FileStatus> filesStatuses = treeModel.getFilesStatuses();
	    
	    if (logger.isDebugEnabled()) {
	      logger.debug("Tree model " + filesStatuses);
	    }
	    
	    StagingResourcesTableModel tableModel = (StagingResourcesTableModel) filesTable.getModel();
	    tableModel.setFilesStatus(filesStatuses);

	    List<TreePath> commonAncestors = TreeUtil.getTreeCommonAncestors(tree.getSelectionPaths());
	    List<Integer> tableRowsToSelect = new ArrayList<>();
	    for (TreePath treePath : commonAncestors) {
	      String path = TreeUtil.getStringPath(treePath);
	      tableRowsToSelect.addAll(tableModel.getRows(path));
	    }

	    filesTable.clearSelection();
	    for (Integer i : tableRowsToSelect) {
	      filesTable.addRowSelectionInterval(i, i);
	    }
	    
	    // Activate the table view.
	    scrollPane.setViewportView(filesTable);
	  }
	  
	  updateChangeViewButton();

	  if (forStagedResources) {
	    OptionsManager.getInstance().saveStagedResViewMode(viewMode);
	  } else {
	    OptionsManager.getInstance().saveUnstagedResViewMode(viewMode);
	  }
	}

	/**
	 * Updates the button that switches the view with the a image and tooltip that correspond
	 * to the current active view mode.
	 */
  private void updateChangeViewButton() {
    if (switchViewButton != null) {
      if (currentViewMode == ResourcesViewMode.TREE_VIEW) {
        Icon icon = Icons.getIcon(Icons.LIST_VIEW);
          switchViewButton.setIcon(icon);
        switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TO_LIST_VIEW));
      } else {
        Icon icon = Icons.getIcon(Icons.TREE_VIEW);
          switchViewButton.setIcon(icon);
        switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
      }
    }
  }

	/**
	 * Calculates the treePaths from the table selected files and sets them into the tree.
	 */
	private void restoreSelectedPathsFromTableToTree() {
		int[] selectedRows = filesTable.getSelectedRows();
		StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();

		TreePath[] selPaths = new TreePath[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++) {
			int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
			String absolutePath = fileTableModel.getFileLocation(convertedRow);

			GitTreeNode nodeBuilder = TreeUtil.getTreeNodeFromString((StagingResourcesTreeModel) tree.getModel(),
					absolutePath);
			GitTreeNode[] selectedPath = new GitTreeNode[absolutePath.split("/").length + 1];
			int count = selectedPath.length;
			while (nodeBuilder != null) {
				count--;
				selectedPath[count] = nodeBuilder;
				nodeBuilder = (GitTreeNode) nodeBuilder.getParent();
			}

			selPaths[i] = new TreePath(selectedPath);
		}
		
		tree.setSelectionPaths(selPaths);
	}

	/**
	 * Show contextual menu for flat view.
	 * 
	 * @param x             The X coordinate where to show the menu.
	 * @param y             The Y coordinate where to show the menu.
	 * @param selectedRows  The selected rows.
	 */
	private void showContextualMenuForFlatView(int x, int y, int[] selectedRows) {
	  final List<FileStatus> files = new ArrayList<>();
	  
    for (int i = 0; i < selectedRows.length; i++) {
      int convertedSelectedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
      StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
      FileStatus file = new FileStatus(model.getUnstageFile(convertedSelectedRow));
      files.add(file);
    }
    
    GitResourceContextualMenu contextualMenu = new GitResourceContextualMenu(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            // All resources are "leaves" in the table view.
            return getAllSelectedResources();
          }
          @Override
          public List<FileStatus> getAllSelectedResources() {
            return files;
          }
        },
        gitController,
        historyController,
        forStagedResources,
        RepoUtil.getRepoState());
    
    contextualMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        isContextMenuShowing = true;
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        isContextMenuShowing  = false;
        contextualMenu.removePopupMenuListener(this);
      }
      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        isContextMenuShowing  = false;
      }
    });
    
    contextualMenu.show(filesTable, x, y);
  }
	
	/**
	 * Open an instance of diff presenter and compares current file with the last commit.
	 * 
	 * @param row Selection index of file in the current table.
	 */
	private void openFileInCompareEditor(int row) {
		StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
		int convertedRow = filesTable.convertRowIndexToModel(row);
		FileStatus file = model.getUnstageFile(convertedRow);
		DiffPresenter.showDiff(file, gitController);
	}

	/**
	 * @return <code>true</code> if there are conflicts in the selected files.
	 *
	 */
	private boolean isAnyConflictInSelection() {
	  boolean hasConflict;
    if(currentViewMode == ResourcesViewMode.FLAT_VIEW) {
      List<FileStatus> selectedFilesTable = getTableSelectedFiles();
			hasConflict = selectedFilesTable.stream().anyMatch(
							file -> file.getChangeType() == GitChangeType.CONFLICT);
    } else {
      List<String> selectedPaths = TreeUtil.getStringComonAncestor(tree);
      StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
      List<FileStatus> fileStatusesForPaths = fileTreeModel.getFilesByPaths(selectedPaths);
			hasConflict = fileStatusesForPaths.stream().anyMatch(
							file -> file.getChangeType() == GitChangeType.CONFLICT);
    }
    return hasConflict;
	}
    
	/**
	 * Enable or disable the changeSelected button depending on whether or not
	 * something is selected in the current view (flat or tree).
	 */
	private void toggleSelectedButton() {
	  if (changeSelectedButton != null) {
			boolean isAnyTableEntrySelected = currentViewMode == ResourcesViewMode.FLAT_VIEW
			    && filesTable != null
					&& filesTable.getSelectedRowCount() > 0;
			boolean isAnyTreeItemSelected = currentViewMode == ResourcesViewMode.TREE_VIEW 
			    && tree != null
			    && tree.getSelectionCount() > 0;
	    boolean isEnabled = (isAnyTreeItemSelected || isAnyTableEntrySelected) && !isAnyConflictInSelection();
	    changeSelectedButton.setEnabled(isEnabled);
	  }
	}

	public JButton getChangeSelectedButton() {
		return changeSelectedButton;
	}
	
	 public JButton getChangeAllButton() {
	    return changeAllButton;
	  }

	/**
	 * @return The tree that presents the resources. 
	 */
	private JTree createTree() {
	  JTree t = new Tree() {
	    @Override
	    public JToolTip createToolTip() {
	      return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
	    }
	  };
	  
	  t.setCellRenderer(new ChangesTreeCellRenderer(() -> isContextMenuShowing));
	  t.setModel(new StagingResourcesTreeModel(gitController, null, forStagedResources, null));
	  t.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
	  t.setLargeModel(true);
	  
    return t;
  }
	
    
  /**
   * The repository changed.
   * 
   * @param unstagedFiles The changed files from the new repository.
   */
  private void repositoryChanged(List<FileStatus> unstagedFiles) {
    updateFlatView(unstagedFiles);

    updateTreeView(unstagedFiles);
  }
}
