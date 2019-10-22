package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
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

import com.jidesoft.utils.SwingWorker;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.event.GitEvent;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.historycomponents.HistoryController;
import com.oxygenxml.git.view.renderer.ChangesTreeCellRenderer;

import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

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
  interface SelectedResourcesProvider {
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
    FLAT_VIEW,
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
	private GitController stageController;

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
	 * @param stageController     Staging controller.
	 * @param historyController   History interface.
	 * @param forStagedResources  <code>true</code> if for staged resources.
	 */
	public ChangesPanel(
	    GitController stageController, 
	    HistoryController historyController, 
	    boolean forStagedResources) {
		this.historyController = historyController;
		this.forStagedResources = forStagedResources;
		this.stageController = stageController;
		
		tree = createTree();
		// ==== EXM-41138 hack: expand/collapse on second mouse released ====
		tree.setDragEnabled(true);
		// ==================================================================
		ToolTipManager.sharedInstance().registerComponent(tree);
		this.currentViewMode = forStagedResources ? OptionsManager.getInstance().getStagedResViewMode()
		    : OptionsManager.getInstance().getUntagedResViewMode();
		
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        if (filesTable != null) {
          // The event might come too early.
          GitAccess gitAccess = GitAccess.getInstance();
          try {
            Repository repository = gitAccess.getRepository();
            if (repository != null) {
              Runnable updateTask = new SwingWorker<List<FileStatus>, Void>() {
                @Override
                protected List<FileStatus> doInBackground() throws Exception {
                  if (forStagedResources) {
                    return gitAccess.getStagedFiles();
                  } else {
                    return gitAccess.getUnstagedFiles();
                  }
                }
                @Override
                protected void done() {
                  List<FileStatus> files = Collections.emptyList();
                  try {
                    files = get();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error(e, e);
                  } catch (ExecutionException e) {
                    logger.error(e, e);
                  }
                  ChangesPanel.this.repositoryChanged(files);
                  getChangeSelectedButton().setEnabled(true);
                }
              };
              
              GitOperationScheduler.getInstance().schedule(updateTask);
            }
          } catch (NoRepositorySelected ex) {
            logger.debug(ex, ex);

            ChangesPanel.this.repositoryChanged(Collections.emptyList());
          }
        }
      }
      
      @Override
      public void stateChanged(GitEvent changeEvent) {
        // Update the table.
        ChangesPanel.this.stateChanged(changeEvent);
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
	    Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
	    TreePath[] selectionPaths = tree.getSelectionPaths();

	    // Create the tree with the new model
	    tree.setModel(
	        new StagingResourcesTreeModel(
	            stageController, 
	            GitAccess.getInstance().getWorkingCopyName(), 
	            forStagedResources, 
	            filesStatus));

	    // restore last expanded paths after refresh
	    TreeFormatter.restoreLastExpandedPaths(expandedPaths, tree);
	    tree.setSelectionPaths(selectionPaths);
	  }
	}

	/**
	 * Returns all the current expanded paths
	 * 
	 * @return - the current expanded paths
	 */
	private Enumeration<TreePath> getLastExpandedPaths() {
		StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
		GitTreeNode rootNode = (GitTreeNode) treeModel.getRoot();
		Enumeration<TreePath> expandedPaths = null;
		if (rootNode != null) {
			TreePath rootTreePath = new TreePath(rootNode);
			expandedPaths = tree.getExpandedDescendants(rootTreePath);
		}
		return expandedPaths;
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
   * @param unstagedFiles
   *          - the new files to update the flat view
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
	public void stateChanged(GitEvent changeEvent) {
	  if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
	    StagingResourcesTableModel modelTable = (StagingResourcesTableModel) filesTable.getModel();
	    modelTable.stateChanged(changeEvent);
	  } else {
	    Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
	    TreePath[] selectionPaths = tree.getSelectionPaths();

	    StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
	    treeModel.stateChanged(changeEvent);

	    // Restore last expanded paths after refresh
	    TreeFormatter.restoreLastExpandedPaths(expandedPaths, tree);
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
		addFilesPanel(gbc);
		addChangeSelectedButton(gbc);
		addChangeAllButton(gbc);
		addSwitchViewButton(gbc);

		addSwitchViewButtonListener();
		addChangeSelectedButtonListener();
		addChangeAllButtonListener();
		addTreeMouseListener();
		addTreeExpandListener();
		
		// Compare file with last commit when enter is pressed.
		tree.addKeyListener(new KeyAdapter() {
		  @Override
		  public void keyReleased(KeyEvent e) {
		    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
		      TreePath pathForRow = null;
		      int[] selectionRows = tree.getSelectionRows();
		      if (selectionRows != null && selectionRows.length > 0) {
		        pathForRow = tree.getPathForRow(selectionRows[selectionRows.length - 1]);
		      }
		      if (pathForRow != null) {
		        StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
		        String stringPath = TreeFormatter.getStringPath(pathForRow);
		        GitTreeNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
		        if (model != null && node != null
		            && model.isLeaf(node) && !model.getRoot().equals(node)) {
		          FileStatus file = model.getFileByPath(stringPath);
		          DiffPresenter.showDiff(file, stageController);
		        }
		      }
		    } else if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
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
		  }
		});

		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.STAGING_PANEL_MIN_HEIGHT));
	}

	/**
	 * Adds an expand listener to the tree: When the user expands a node the node
	 * will expand as long as it has only one child.
	 */
	private void addTreeExpandListener() {
		tree.addTreeExpansionListener(new TreeExpansionListener() {
		  @Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
				GitTreeNode node = (GitTreeNode) path.getLastPathComponent();
				if (!model.isLeaf(node)) {
					int children = node.getChildCount();
					if (children == 1) {
						GitTreeNode child = (GitTreeNode) node.getChildAt(0);
						TreePath childPath = new TreePath(child.getPath());
						tree.expandPath(childPath);
					}
				}
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
	          String stringPath = TreeFormatter.getStringPath(treePath);
	          StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
	          GitTreeNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);

	          if (!node.isRoot() 
	              || node.children().hasMoreElements()
	              || isMergingResolved()) {

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
	          String stringPath = TreeFormatter.getStringPath(treePath);
	          StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
	          GitTreeNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
	          if (model.isLeaf(node) && !model.getRoot().equals(node)) {
	            FileStatus file = model.getFileByPath(stringPath);
	            DiffPresenter.showDiff(file, stageController);
	          }
	        }
	      }
	    }
	  });
	}
	
	/**
	 * @return the state of the current repository or <code>null</code>, if there's no repository selected.
	 */
	 private RepositoryState getRepositoryState() {
	   RepositoryState repositoryState = null;
     Repository repo = getCurrentRepository();
     if (repo != null) {
       repositoryState = repo.getRepositoryState();
     }
     return repositoryState;
   }
	
	/**
	 * Check if the merging has been resolved.
	 * 
	 * @param repositoryState the repository state.
	 * 
	 * @return <code>true</code> if the merging has been resolved.
	 */
	private boolean isMergingResolved() {
	  RepositoryState repositoryState = getRepositoryState();
    return repositoryState != null && repositoryState == RepositoryState.MERGING_RESOLVED;
  }
	
	/**
	 * @return the current repository or <code>null</code> if there's no repository selected.
	 */
	private Repository getCurrentRepository() {
	  Repository repo = null;
	  try {
	    repo = GitAccess.getInstance().getRepository();
    } catch (NoRepositorySelected e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    }
	  return repo;
	}
	
	 /**
   * Show contextual menu
   * 
   * @param x         X coordinate where to show.
   * @param y         Y coordinate where to show.
   * @param model     The model of the tree.
   * @param treePath  The current tree path.
   */
  private void showContextualMenuForTree(int x, int y, final StagingResourcesTreeModel model) {
    final List<String> selPaths = TreeFormatter.getStringComonAncestor(tree);
    GitViewResourceContextualMenu contextualMenu = new GitViewResourceContextualMenu(
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
        stageController,
        historyController,
        forStagedResources,
        getRepositoryState());
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
	 * Adds a listener on the changeAll button: When clicked all the files will go
	 * in the staging or unstaging area, depending on the forStaging variable
	 */
	private void addChangeAllButtonListener() {
		changeAllButton.addActionListener(e -> {
      if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
        StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
        fileTableModel.switchAllFilesStageState();
      } else {
        StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
        treeModel.switchAllFilesStageState();
      }
    });
	}

	/**
	 * Adds a listener on the changeSelected button: When clicked all the files
	 * selected will go in the staging or unstaging area, depending on the
	 * forStaging variable
	 */
	private void addChangeSelectedButtonListener() {
		changeSelectedButton.addActionListener(
		    new ActionListener() { // NOSONAR
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
					List<String> selectedFiles = TreeFormatter.getStringComonAncestor(tree);
					StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
					List<FileStatus> fileStatusesForPaths = fileTreeModel.getFilesByPaths(selectedFiles);
					fileStatuses.addAll(fileStatusesForPaths);
				}
				
	      // "Stage"/"Unstage" actions
	      AbstractAction stageUnstageAction = new StageUnstageResourceAction(
	          fileStatuses, 
	          !forStagedResources, 
	          stageController);
	      stageUnstageAction.actionPerformed(null);
				
				changeSelectedButton.setEnabled(false);
			}

		});
	}

	/**
	 * Adds a listener on the switchView button: When clicked the current view
	 * will change. Also the selected files will be selected in the new view (the
	 * selection is preserved between the view changes)
	 */
	private void addSwitchViewButtonListener() {
		switchViewButton.addActionListener(e -> {
    	setResourcesViewMode(currentViewMode == ResourcesViewMode.FLAT_VIEW ? 
    	    ResourcesViewMode.TREE_VIEW : ResourcesViewMode.FLAT_VIEW);
    	isContextMenuShowing = false;
    });
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
              stageController, 
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

	    List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(tree.getSelectionPaths());
	    List<Integer> tableRowsToSelect = new ArrayList<>();
	    for (TreePath treePath : commonAncestors) {
	      String path = TreeFormatter.getStringPath(treePath);
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
        Icon icon = Icons.getIcon(Icons.TABLE_VIEW);
          switchViewButton.setIcon(icon);
        switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_FLAT_VIEW_BUTTON_TOOLTIP));
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

			GitTreeNode nodeBuilder = TreeFormatter.getTreeNodeFromString((StagingResourcesTreeModel) tree.getModel(),
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
	 * Adds the changeAll button to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addChangeAllButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		if (forStagedResources) {
			changeAllButton = new JButton(translator.getTranslation(Tags.UNSTAGE_ALL_BUTTON_TEXT));
		} else {
			changeAllButton = new JButton(translator.getTranslation(Tags.STAGE_ALL_BUTTON_TEXT));
		}
		this.add(changeAllButton, gbc);
	}

	/**
	 * Adds the changeSelected button to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addChangeSelectedButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		if (forStagedResources) {
			changeSelectedButton = new JButton(translator.getTranslation(Tags.UNSTAGE_SELECTED_BUTTON_TEXT));
		} else {
			changeSelectedButton = new JButton(translator.getTranslation(Tags.STAGE_SELECTED_BUTTON_TEXT));
		}
		changeSelectedButton.setEnabled(false);
		this.add(changeSelectedButton, gbc);

	}

	/**
	 * Adds the switchView button the the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addSwitchViewButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JToolBar toolbar = new JToolBar();
		switchViewButton = new ToolbarButton(null, false);
		switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
		
		String iconType = currentViewMode == ResourcesViewMode.FLAT_VIEW 
		    ? Icons.TREE_VIEW
		    : Icons.TABLE_VIEW;
	  Icon icon = Icons.getIcon(iconType);
		  switchViewButton.setIcon(icon);
		toolbar.add(switchViewButton);
		toolbar.setFloatable(false);
		toolbar.setOpaque(false);
		this.add(toolbar, gbc);

	}

	/**
	 * Adds the scollPane to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addFilesPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				0, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		
		filesTable = UIUtil.createResourcesTable(new StagingResourcesTableModel(stageController, forStagedResources), ()-> isContextMenuShowing);
		

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

		        GitStatus status = GitAccess.getInstance().getStatus();
		        if (status.getStagedFiles().isEmpty()
		            && status.getUnstagedFiles().isEmpty()
		            && isMergingResolved()) {
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
		scrollPane.setPreferredSize(new Dimension(200, 200));
		filesTable.setFillsViewportHeight(true);
		this.add(scrollPane, gbc);
		
		setResourcesViewMode(currentViewMode);
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
    
    GitViewResourceContextualMenu contextualMenu = new GitViewResourceContextualMenu(
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
        stageController,
        historyController,
        forStagedResources,
        getRepositoryState());
    
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
		DiffPresenter.showDiff(file, stageController);
	}

	/**
	 * Enable or disable the changeSelected button depending wheter or not
	 * something is selected in the current view(flat or tree)
	 */
	private void toggleSelectedButton() {
		boolean isEnabled = 
		    currentViewMode == ResourcesViewMode.FLAT_VIEW && filesTable.getSelectedRowCount() > 0
				    || currentViewMode == ResourcesViewMode.TREE_VIEW && tree.getSelectionCount() > 0;
			changeSelectedButton.setEnabled(isEnabled);
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
	  JTree t = UIUtil.createTree();
	  
	  t.setCellRenderer(new ChangesTreeCellRenderer(() -> isContextMenuShowing));
	  t.setModel(new StagingResourcesTreeModel(stageController, null, forStagedResources, null));
	  
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
