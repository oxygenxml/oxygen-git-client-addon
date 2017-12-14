package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
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
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.xml.bind.annotation.XmlEnum;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * This is the staging or the unstaging area, depending on the forStaging
 * variable (<code>true</code> if this model presents un-staged resources that
 * will be staged. <code>false</code> if this model presents staged resources
 * that will be unstaged).
 * 
 * 
 * TODO Sorin Only one view is visible at a given time: either the TRE or the TABLE.
 * Create update just the visible one and mark the other one as dirty. When the user
 * switches to the other view, we will recreate it if it's dirty. 
 * 
 * @author Beniamin Savu
 *
 */
public class ChangesPanel extends JPanel {
  
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
	 * The git API, containg the commands
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * The active view in the scroll pane
	 */
	private ResourcesViewMode currentViewMode;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator = Translator.getInstance();

	/**
	 * Selected paths in tree.
	 */
	private TreePath[] selectedPaths = null;

	public ChangesPanel(StageController stageController, boolean forStagedResources) {
		this.forStagedResources = forStagedResources;
		this.stageController = stageController;
		
		tree = createTree();
		this.stageController.addTree(this.tree);
		ToolTipManager.sharedInstance().registerComponent(tree);
		this.currentViewMode = forStagedResources ? OptionsManager.getInstance().getStagedResViewMode()
		    : OptionsManager.getInstance().getUntagedResViewMode();
		
		// TODO More lazy. mark the hidden view as dirty.
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        // Update the table.
        ((StagingResourcesTableModel)filesTable.getModel()).stateChanged(changeEvent);
        
        // Update the tree.
        StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
        treeModel.stateChanged(changeEvent);
        
        toggleSelectedButton();
      }
    });

	}

	public JTable getFilesTable() {
		return filesTable;
	}
	
	/**
	 * @return The tree that renders resources.
	 */
	public JTree getTreeView() {
    return tree;
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
	public void createTreeView(String path, List<FileStatus> filesStatus) {
		StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
		GitTreeNode rootNode = (GitTreeNode) treeModel.getRoot();
		Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
		
		path = path.replace("\\", "/");
		String rootFolder = path.substring(path.lastIndexOf('/') + 1);
		if (rootNode == null || !rootFolder.equals(rootNode.getUserObject())) {
			GitTreeNode root = new GitTreeNode(rootFolder);
			// Create the tree model and add the root node to it
			treeModel = new StagingResourcesTreeModel(root, false, new ArrayList<FileStatus>(filesStatus));
			if (forStagedResources) {
				treeModel = new StagingResourcesTreeModel(root, true, new ArrayList<FileStatus>(filesStatus));
			}

			// Create the tree with the new model
			tree.setModel(treeModel);
		}

		treeModel.setFilesStatus(filesStatus);

		CustomTreeIconRenderer treeRenderer = new CustomTreeIconRenderer();
		tree.setCellRenderer(treeRenderer);

		// restore last expanded paths after refresh
		TreeFormatter.restoreLastExpandedPaths(expandedPaths, tree);
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
	public void updateFlatView(List<FileStatus> unstagedFiles) {
		StagingResourcesTableModel modelTable = (StagingResourcesTableModel) filesTable.getModel();
		List<FileStatus> selectedFiles = getTableSelectedFiles();
		modelTable.setFilesStatus(unstagedFiles);

		restoreTableSelection(modelTable, selectedFiles);
		selectedFiles.clear();
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
		List<FileStatus> selectedFiles = new ArrayList<FileStatus>();
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
		addChangeAllButton(gbc);
		addChangeSelectedButton(gbc);
		addSwitchViewButton(gbc);
		addFilesPanel(gbc);

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
					StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
					int[] selectionRows = tree.getSelectionRows();
					if (selectionRows != null && selectionRows.length > 0) {
						pathForRow = tree.getPathForRow(selectionRows[selectionRows.length - 1]);
					}
					if (pathForRow != null) {
					  String stringPath = TreeFormatter.getStringPath(pathForRow);
					  GitTreeNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
					  if (model != null && node != null
					      && model.isLeaf(node) && !model.getRoot().equals(node)) {
					    FileStatus file = model.getFileByPath(stringPath);
					    DiffPresenter diff = new DiffPresenter(file, stageController);
					    diff.showDiff();
					  }
					}
				}
			}
		});

		if (!forStagedResources) {
			List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
			updateFlatView(unstagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), unstagedFiles);
		} else {
			List<FileStatus> stagedFiles = gitAccess.getStagedFile();
			updateFlatView(stagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), stagedFiles);
		}
		
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
			public void mouseReleased(MouseEvent e) {
				StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
				TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
				if (treePath != null) {
				  
					String stringPath = TreeFormatter.getStringPath(treePath);
					GitTreeNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
					
					// ============= Double click event ==============
					if (model.isLeaf(node) && !model.getRoot().equals(node)
					    && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					  FileStatus file = model.getFileByPath(stringPath);
					  DiffPresenter diff = new DiffPresenter(file, stageController);
					  diff.showDiff();
					}
					
					// ============= Tight click event ================
					if (SwingUtilities.isRightMouseButton(e)
					    && (!node.isRoot() || node.children().hasMoreElements())) {
					  boolean treeInSelection = false;
					  TreePath[] paths = tree.getSelectionPaths();
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
						List<String> selectedPaths = TreeFormatter.getStringComonAncestor(tree);
						List<FileStatus> selectedFileStatuses = model.getFilesByPaths(selectedPaths);
						GitViewResourceContextualMenu contextualMenu = new GitViewResourceContextualMenu(
						    selectedFileStatuses,
						    stageController,
						    forStagedResources);
						contextualMenu.show(tree, e.getX(), e.getY());
					}
				} else {
					tree.clearSelection();
				}
			}
		});

	}

	/**
	 * Adds a listener on the changeAll button: When clicked all the files will go
	 * in the staging or unstaging area, depending on the forStaging variable
	 */
	private void addChangeAllButtonListener() {
		changeAllButton.addActionListener(new ActionListener() {

			@Override
      public void actionPerformed(ActionEvent e) {
				StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
				fileTableModel.switchAllFilesStageState();
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
			  List<FileStatus> fss = new ArrayList<FileStatus>();
				if (currentViewMode == ResourcesViewMode.FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
					for (int i = selectedRows.length - 1; i >= 0; i--) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						FileStatus fileStatus = fileTableModel.getFileStatus(convertedRow);
						fss.add(fileStatus);
					}
				} else {
					List<String> selectedFiles = TreeFormatter.getStringComonAncestor(tree);
					StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
					for (Iterator<String> iterator = selectedFiles.iterator(); iterator.hasNext();) {
            String path = iterator.next();
            FileStatus fileByPath = fileTreeModel.getFileByPath(path);
            fss.add(fileByPath);
          }
				}
				
	      // "Stage"/"Unstage" actions
	      AbstractAction stageUnstageAction = new StageUnstageResourceAction(
	          fss, 
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
	    selectedPaths = restoreSelectedPathsFromTableToTree();
	    tree.setSelectionPaths(selectedPaths);
	    scrollPane.setViewportView(tree);
	    switchViewButton.setIcon(Icons.getIcon(ImageConstants.TABLE_VIEW));
	    switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_FLAT_VIEW_BUTTON_TOOLTIP));
	  } else {
	    switchViewButton.setIcon(Icons.getIcon(ImageConstants.TREE_VIEW));
	    switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
	    filesTable.clearSelection();
	    StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();

	    List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(tree.getSelectionPaths());
	    List<Integer> tableRowsToSelect = new ArrayList<Integer>();
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

		TreePath[] selectedPaths = new TreePath[selectedRows.length];
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

			selectedPaths[i] = new TreePath(selectedPath);
		}
		return selectedPaths;
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
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
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
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
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
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JToolBar toolbar = new JToolBar();
		switchViewButton = new ToolbarButton(null, false);
		switchViewButton.setToolTipText(translator.getTranslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
		switchViewButton.setIcon(currentViewMode == ResourcesViewMode.FLAT_VIEW ? Icons.getIcon(ImageConstants.TREE_VIEW)
		    : Icons.getIcon(ImageConstants.TABLE_VIEW));
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
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		StagingResourcesTableModel fileTableModel = new StagingResourcesTableModel(stageController, forStagedResources);
		
		filesTable = createTable(fileTableModel);
		filesTable.setTableHeader(null);
		filesTable.setShowGrid(false);
		
		ImageIcon icon = Icons.getIcon(ImageConstants.GIT_ADD_ICON);
		
		int iconWidth = icon.getIconWidth();
    filesTable.getColumnModel().getColumn(0).setPreferredWidth(iconWidth);
    filesTable.getColumnModel().getColumn(0).setMaxWidth(iconWidth + 4);
		
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
			public void mouseReleased(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				int row = filesTable.convertRowIndexToModel(filesTable.rowAtPoint(point));
				int column = filesTable.columnAtPoint(point);
				if (column == -1 || row == -1) {
					filesTable.clearSelection();
				} else {
				  // ======== LEFT DOUBLE CLICK ========
					if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
						openFileInCompareEditor(row);
					}
					
					// ======== RIGHT CLICK ==========
					if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1 && row != -1) {
						boolean inSelection = false;
						List<FileStatus> files = new ArrayList<FileStatus>();
						int clickedRow = filesTable.rowAtPoint(e.getPoint());
						int[] selectedRows = filesTable.getSelectedRows();
						for (int i = 0; i < selectedRows.length; i++) {
							if (clickedRow == selectedRows[i]) {
								inSelection = true;
								break;
							}

						}

						if (clickedRow >= 0 && clickedRow < filesTable.getRowCount()) {
							if (!inSelection) {
								filesTable.setRowSelectionInterval(clickedRow, clickedRow);
								selectedRows = filesTable.getSelectedRows();
							}

							for (int i = 0; i < selectedRows.length; i++) {
								int convertedSelectedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
								StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
								FileStatus file = new FileStatus(model.getUnstageFile(convertedSelectedRow));
								files.add(file);
							}
							
							GitViewResourceContextualMenu contextualMenu = new GitViewResourceContextualMenu(
							    files,
	                stageController,
	                forStagedResources);
							contextualMenu.show(filesTable, e.getX(), e.getY());
						} else {
							filesTable.clearSelection();
						}
					}
				}
				toggleSelectedButton();
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
	private final class CustomTreeIconRenderer extends DefaultTreeCellRenderer {
	  /**
	   * @see javax.swing.tree.DefaultTreeCellRenderer.getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)
	   */
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			Icon icon = Icons.getIcon(ImageConstants.FOLDER_TREE_ICON);
			String toolTip = "";

			StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
			TreePath treePath = tree.getPathForRow(row);
			if (treePath != null) {
				String path = TreeFormatter.getStringPath(treePath);
				if (!"".equals(path) && model.isLeaf(TreeFormatter.getTreeNodeFromString(model, path))) {
					FileStatus file = model.getFileByPath(path);
					GitChangeType changeType = file.getChangeType();
					RenderingInfo renderingInfo = getRenderingInfo(changeType);
					if (renderingInfo != null) {
					  icon = renderingInfo.getIcon();
					  toolTip = renderingInfo.getTooltip();
					}
				}
			}
			label.setIcon(icon);
			label.setToolTipText(toolTip);

			return label;
		}
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
	        Icons.getIcon(ImageConstants.GIT_ADD_ICON),
	        translator.getTranslation(Tags.ADD_ICON_TOOLTIP));
    } else if (GitChangeType.MODIFIED == changeType || GitChangeType.CHANGED == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(ImageConstants.GIT_MODIFIED_ICON),
          translator.getTranslation(Tags.MODIFIED_ICON_TOOLTIP));
    } else if (GitChangeType.MISSING == changeType || GitChangeType.REMOVED == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(ImageConstants.GIT_DELETE_ICON),
          translator.getTranslation(Tags.DELETE_ICON_TOOLTIP));
    } else if (GitChangeType.CONFLICT == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(ImageConstants.GIT_CONFLICT_ICON),
          translator.getTranslation(Tags.CONFLICT_ICON_TOOLTIP));
    } else if (GitChangeType.SUBMODULE == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(ImageConstants.GIT_SUBMODULE_FILE_ICON),
          translator.getTranslation(Tags.SUBMODULE_ICON_TOOLTIP));
    }
	  return renderingInfo;
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
	  
	  t.setModel(new StagingResourcesTreeModel(null, false, null));
	  
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
}
