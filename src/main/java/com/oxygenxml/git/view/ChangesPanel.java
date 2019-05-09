package com.oxygenxml.git.view;

import java.awt.Color;
import java.awt.Component;
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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.xml.bind.annotation.XmlEnum;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
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
	private StageController stageController;

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
	 * Image utilities.
	 */
	private ImageUtilities imageUtilities = 
	    PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
	
	/**
	 * <code>true</code> if the contextual menu is showing for the resources in the tree view.
	 */
	private boolean isContextMenuShowing = false;

	/**
	 * Constructor.
	 * 
	 * @param stageController     Staging controller.
	 * @param forStagedResources  <code>true</code> if for staged resources.
	 */
	public ChangesPanel(StageController stageController, boolean forStagedResources) {
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
              List<FileStatus> files = null;
              if (forStagedResources) {
                files = gitAccess.getStagedFiles();
              } else {
                files = gitAccess.getUnstagedFiles();
              }

              ChangesPanel.this.repositoryChanged(files);

              getChangeSelectedButton().setEnabled(true);
            }
          } catch (NoRepositorySelected ex) {
            logger.debug(ex, ex);

            ChangesPanel.this.repositoryChanged(Collections.emptyList());
          }
        }
      }
      
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
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
	 * @param path
	 *          - the root
	 * @param filesStatus
	 *          - files to generate the nodes
	 */
	private void updateTreeView(String rootFolder, List<FileStatus> filesStatus) {
	  if (tree != null) {
	    if (rootFolder == null) {
	      rootFolder = "";
	    }
	    StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
	    GitTreeNode rootNode = (GitTreeNode) treeModel.getRoot();
	    Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
	    TreePath[] selectionPaths = tree.getSelectionPaths();

	    if (rootNode == null || !rootFolder.equals(rootNode.getUserObject())) {
	      GitTreeNode root = new GitTreeNode(rootFolder);
	      // Create the tree model and add the root node to it
	      treeModel = new StagingResourcesTreeModel(stageController, root, forStagedResources, new ArrayList<>(filesStatus));

	      // Create the tree with the new model
	      tree.setModel(treeModel);
	    }

	    treeModel.setFilesStatus(filesStatus);

	    tree.setCellRenderer(new ChangesTreeCellRenderer());

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
  public void update(String rootFolder, List<FileStatus> newFiles) {
    if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
      updateFlatView(newFiles);
    } else {
      updateTreeView(rootFolder, newFiles);
    }
  }
  
	
	/**
	 * Notify the models about the change.
	 * 
	 * @param changeEvent Change event.
	 */
	public void stateChanged(ChangeEvent changeEvent) {
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

		addSwitchButtonListener();
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
		          DiffPresenter diff = new DiffPresenter(file, stageController);
		          diff.showDiff();
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

		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.STAGING_PANEL_HEIGHT));
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
	  tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        toggleSelectedButton();
      }
    });
	  
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
	            DiffPresenter diff = new DiffPresenter(file, stageController);
	            diff.showDiff();
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
   * Check if the repository is in merging state.
   * 
   * @return <code>true</code> if the repository is in merging state.
   */
	private boolean isRepoInMergingState() {
	  boolean toReturn = false;
	  Repository repo = getCurrentRepository();
	  if (repo != null) {
	    RepositoryState repositoryState = repo.getRepositoryState();
	    toReturn = repositoryState == RepositoryState.MERGING_RESOLVED
	        || repositoryState == RepositoryState.MERGING;
	  }
	  return toReturn;
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
        forStagedResources,
        isRepoInMergingState());
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
		changeAllButton.addActionListener(new ActionListener() {

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
		});
	}

	/**
	 * Adds a listener on the changeSelected button: When clicked all the files
	 * selected will go in the staging or unstaging area, depending on the
	 * forStaging variable
	 */
	private void addChangeSelectedButtonListener() {
		changeSelectedButton.addActionListener(new ActionListener() {
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
	private void addSwitchButtonListener() {
		switchViewButton.addActionListener(new ActionListener() {
			@Override
      public void actionPerformed(ActionEvent e) {
				setResourcesViewMode(currentViewMode == ResourcesViewMode.FLAT_VIEW ? 
				    ResourcesViewMode.TREE_VIEW : ResourcesViewMode.FLAT_VIEW);
				isContextMenuShowing = false;
			}
		});
	}

	/**
	 * Set the current view mode for the resources: tree or table.
	 * 
	 * @param viewMode The new view mode.
	 */
	void setResourcesViewMode(ResourcesViewMode viewMode) {
	  this.currentViewMode = viewMode;
	  
	  if (viewMode == ResourcesViewMode.TREE_VIEW) {
	    TreePath[] selectedPaths = restoreSelectedPathsFromTableToTree();
	    tree.setSelectionPaths(selectedPaths);
	    scrollPane.setViewportView(tree);
	    if (switchViewButton != null) {
	      URL resource = getClass().getResource(ImageConstants.TABLE_VIEW);
	      if (resource != null) {
	        ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
	        switchViewButton.setIcon(icon);
	      }
	      switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_FLAT_VIEW_BUTTON_TOOLTIP));
	    }
	  } else {
	    if (switchViewButton != null) {
	      URL resource = getClass().getResource(ImageConstants.TREE_VIEW);
	      if (resource != null) {
	        ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
	        switchViewButton.setIcon(icon);
	      }
	      switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
	    }
	    filesTable.clearSelection();
	    StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();

	    List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(tree.getSelectionPaths());
	    List<Integer> tableRowsToSelect = new ArrayList<>();
	    for (TreePath treePath : commonAncestors) {
	      String path = TreeFormatter.getStringPath(treePath);
	      tableRowsToSelect.addAll(fileTableModel.getRows(path));
	    }

	    for (Integer i : tableRowsToSelect) {
	      filesTable.addRowSelectionInterval(i, i);
	    }
	    scrollPane.setViewportView(filesTable);
	  }

	  if (forStagedResources) {
	    OptionsManager.getInstance().saveStagedResViewMode(viewMode);
	  } else {
	    OptionsManager.getInstance().saveUnstagedResViewMode(viewMode);
	  }
	}

	/**
	 * Calculates the treePaths from the table selected files and return the
	 * treePaths
	 * 
	 * @return TreePath array containing the the files selected from the table
	 *         view
	 */
	private TreePath[] restoreSelectedPathsFromTableToTree() {
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
		return selPaths;
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
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
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
		URL resource = currentViewMode == ResourcesViewMode.FLAT_VIEW 
		    ? getClass().getResource(ImageConstants.TREE_VIEW)
		    : getClass().getResource(ImageConstants.TABLE_VIEW);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  switchViewButton.setIcon(icon);
		}
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
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		StagingResourcesTableModel fileTableModel = new StagingResourcesTableModel(stageController, forStagedResources);
		
		filesTable = createTable(fileTableModel);
		filesTable.setTableHeader(null);
		filesTable.setShowGrid(false);
		
		URL resource = getClass().getResource(ImageConstants.GIT_ADD_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  int iconWidth = icon.getIconWidth();
		  TableColumn statusCol = filesTable.getColumnModel().getColumn(StagingResourcesTableModel.FILE_STATUS_COLUMN);
      statusCol.setPreferredWidth(iconWidth);
		  statusCol.setMaxWidth(iconWidth + 4);
		}

		filesTable.setDefaultRenderer(Object.class, new ChangesTableCellRenderer());
		filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          toggleSelectedButton();
        }
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
        forStagedResources,
        isRepoInMergingState());
    
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
		DiffPresenter diff = new DiffPresenter(file, stageController);
		diff.showDiff();
	}

	/**
	 * Enable or disable the changeSelected button depending wheter or not
	 * something is selected in the current view(flat or tree)
	 */
	private void toggleSelectedButton() {
		if (currentViewMode == ResourcesViewMode.FLAT_VIEW && filesTable.getSelectedRowCount() > 0
				|| currentViewMode == ResourcesViewMode.TREE_VIEW && tree.getSelectionCount() > 0) {
			changeSelectedButton.setEnabled(true);
		} else {
			changeSelectedButton.setEnabled(false);
		}
	}

	public JButton getChangeSelectedButton() {
		return changeSelectedButton;
	}
	
	 public JButton getChangeAllButton() {
	    return changeAllButton;
	  }

	/**
	 * Renderer for the leafs icon in the tree, based on the git change type file
	 * status.
	 * 
	 * @author Beniamin Savu
	 *
	 */
	private final class ChangesTreeCellRenderer extends DefaultTreeCellRenderer {
	  /**
	   * Default selection color.
	   */
	  private final Color defaultSelectionColor = getBackgroundSelectionColor();
	  
	  /**
	   * @see javax.swing.tree.DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)
	   */
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			URL resource = getClass().getResource(ImageConstants.FOLDER_TREE_ICON);
			Icon icon = null;
			if (resource != null) {
			  icon = (Icon) imageUtilities.loadIcon(resource);
			}
			String toolTip = null;

			StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
			TreePath treePath = tree.getPathForRow(row);
			if (treePath != null) {
				String path = TreeFormatter.getStringPath(treePath);
				if (!"".equals(path) && model.isLeaf(TreeFormatter.getTreeNodeFromString(model, path))) {
					FileStatus file = model.getFileByPath(path);
					if (file != null) {
					  GitChangeType changeType = file.getChangeType();
					  RenderingInfo renderingInfo = getRenderingInfo(changeType);
					  if (renderingInfo != null) {
					    icon = renderingInfo.getIcon();
					    toolTip = renderingInfo.getTooltip();
					  }
					} else {
					  label = null;
					}
				}
			}
			
			if (label != null) {
			  label.setIcon(icon);
			  label.setToolTipText(toolTip);

			  // Active/inactive table selection
			  if (sel) {
			    if (tree.hasFocus()) {
			      setBackgroundSelectionColor(defaultSelectionColor);
			    } else if (!isContextMenuShowing) {
			      setBackgroundSelectionColor(getInactiveSelectionColor(defaultSelectionColor));
			    }
			  }
			}

      return label;
		}
	}

	/**
	 * Get inactive selection color.
	 * 
	 * @return the color.
	 */
	private Color getInactiveSelectionColor(Color defaultColor) {
	  Color inactiveBgColor = defaultColor;
	  try {
	    Class<?> colorProviderClass = Class.forName("ro.sync.ui.theme.SAThemeColorProvider");
	    Object colorProvider = colorProviderClass.newInstance();
	    Method getInactiveSelBgColorMethod = colorProviderClass.getMethod("getInactiveSelectionBgColor");
	    int[] rgb = (int[]) getInactiveSelBgColorMethod.invoke(colorProvider);
	    inactiveBgColor = new Color(rgb[0], rgb[1], rgb[2]);
	  } catch (Exception e) {
	    if (isDoubleBuffered()) {
	      logger.debug(e, e);
	    }
	  }
	  return inactiveBgColor;
	}

	/**
	 * Renderer for the staged/unstaged tables.
	 */
	private final class ChangesTableCellRenderer extends DefaultTableCellRenderer {
	  /**
	   * @see javax.swing.table.TableCellRenderer.getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	   */
	  @Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
		    int row, int column) {
	    Icon icon = null;
	    String tooltipText = null;
	    String labelText = "";
	    
	    JLabel tableCellRendererComponent = (JLabel) super.getTableCellRendererComponent(
	        table, value, isSelected, hasFocus, row, column);
	    
	    if (value instanceof GitChangeType) {
	      RenderingInfo renderingInfo = getRenderingInfo((GitChangeType) value);
	      if (renderingInfo != null) {
	        icon = renderingInfo.getIcon();
	        tooltipText = renderingInfo.getTooltip();
	      }
	    } else if (value instanceof String) {
	      tooltipText = (String) value;
	      String fileName = tooltipText.substring(tooltipText.lastIndexOf('/') + 1);
	      if (!fileName.equals(tooltipText)) {
	        tooltipText = tooltipText.replace("/" + fileName, "");
	        tooltipText = fileName + " - " + tooltipText;
	      }
	      labelText = (String) value;
	    }
	    
	    tableCellRendererComponent.setIcon(icon);
	    tableCellRendererComponent.setToolTipText(tooltipText);
	    tableCellRendererComponent.setText(labelText);
	    
	    // Active/inactive table selection
	    if (table.isRowSelected(row)) {
	      if (table.hasFocus()) {
	        tableCellRendererComponent.setBackground(table.getSelectionBackground());
	      } else if (!isContextMenuShowing) {
	        Color defaultColor = table.getSelectionBackground();
	        tableCellRendererComponent.setBackground(getInactiveSelectionColor(defaultColor));
	      }
	    } else {
	      tableCellRendererComponent.setBackground(table.getBackground());
	    }

	    return tableCellRendererComponent;
	  }
	}

	/**
	 * Get the rendering info (such as icon or tooltip text) for the given Git change type.
	 * 
	 * @param changeType The Git change type.
	 * 
	 * @return the rendering info.
	 */
	private RenderingInfo getRenderingInfo(GitChangeType changeType) {
	  RenderingInfo renderingInfo = null;
	  if (GitChangeType.ADD == changeType || GitChangeType.UNTRACKED == changeType) {
	    renderingInfo = new RenderingInfo(
	        getIcon(ImageConstants.GIT_ADD_ICON),
	        translator.getTranslation(Tags.ADD_ICON_TOOLTIP));
    } else if (GitChangeType.MODIFIED == changeType || GitChangeType.CHANGED == changeType) {
      renderingInfo = new RenderingInfo(
          getIcon(ImageConstants.GIT_MODIFIED_ICON),
          translator.getTranslation(Tags.MODIFIED_ICON_TOOLTIP));
    } else if (GitChangeType.MISSING == changeType || GitChangeType.REMOVED == changeType) {
      renderingInfo = new RenderingInfo(
          getIcon(ImageConstants.GIT_DELETE_ICON),
          translator.getTranslation(Tags.DELETE_ICON_TOOLTIP));
    } else if (GitChangeType.CONFLICT == changeType) {
      renderingInfo = new RenderingInfo(
          getIcon(ImageConstants.GIT_CONFLICT_ICON),
          translator.getTranslation(Tags.CONFLICT_ICON_TOOLTIP));
    } else if (GitChangeType.SUBMODULE == changeType) {
      renderingInfo = new RenderingInfo(
          getIcon(ImageConstants.GIT_SUBMODULE_FILE_ICON),
          translator.getTranslation(Tags.SUBMODULE_ICON_TOOLTIP));
    }
	  return renderingInfo;
	}

	/**
	 * Get icon.
	 *  
	 * @param imgKey The image key.
	 * 
	 * @return the icon.
	 */
  private Icon getIcon(String imgKey) {
    Icon toReturn = null;
    URL resource = getClass().getResource(imgKey);
    if (resource != null) {
      toReturn = (Icon) imageUtilities.loadIcon(resource);
    }
    return toReturn;
  }
	
	/**
	 * Rendering info.
	 */
	private static final class RenderingInfo {
	  /**
	   * Icon.
	   */
	  private Icon icon;
	  /**
	   * Tootlip text.
	   */
	  private String tooltip;
	  
    /**
     * Constructor.
     * 
     * @param icon     Icon.
     * @param tooltip  Tooltip text.
     */
    public RenderingInfo(Icon icon, String tooltip) {
      this.icon = icon;
      this.tooltip = tooltip;
    }
    
    /**
     * @return the icon
     */
    public Icon getIcon() {
      return icon;
    }
    
    /**
     * @return the tooltip
     */
    public String getTooltip() {
      return tooltip;
    }
	}
	
	/**
	 * @return The tree that presents the resources. 
	 */
	private JTree createTree() {
	  JTree t = null;
	  try {
      Class<?> treeClass = getClass().getClassLoader().loadClass("ro.sync.exml.workspace.api.standalone.ui.Tree");
      t = (JTree) treeClass.newInstance();
    } catch (Exception e) {
      logger.debug(e, e);
    }
	  
	  if (t == null) {
	    t = new JTree();
	  }
	  
	  t.setModel(new StagingResourcesTreeModel(stageController, null, forStagedResources, null));
	  
    return t;
  }
	
	/**
	 * @param fileTableModel The model for the table.
	 * 
	 * @return The table that presents the resources.
	 */
  private JTable createTable(StagingResourcesTableModel fileTableModel) {
    JTable table = null;
    try {
      Class<?> tableClass = getClass().getClassLoader().loadClass("ro.sync.exml.workspace.api.standalone.ui.Table");
      table = (JTable) tableClass.newInstance();
    } catch (Exception e) {
      logger.debug(e, e);
    }
    
    if (table == null) {
      table = new JTable();
    }
    
    table.setModel(fileTableModel);
    
    return table;
  }

  /**
   * The repository changed.
   * 
   * @param unstagedFiles The changed files from the new repository.
   */
  private void repositoryChanged(List<FileStatus> unstagedFiles) {
    updateFlatView(unstagedFiles);

    String rootFolder = StagingPanel.NO_REPOSITORY;
    try {
      rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
    } catch (NoRepositorySelected e) {
      // Never happens.
      logger.error(e, e);
    }
    updateTreeView(rootFolder, unstagedFiles);
  }
}
