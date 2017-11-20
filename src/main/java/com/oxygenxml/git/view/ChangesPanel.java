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
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * This is the staging or the unstaging area, depending on the forStaging
 * variable (<code>true</code> if this model presents un-staged resources that
 * will be staged. <code>false</code> if this model presents staged resources
 * that will be unstaged).
 * 
 * @author Beniamin Savu
 *
 */
public class ChangesPanel extends JPanel implements Observer<ChangeEvent> {

	/**
	 * constant that specifies the flat view
	 */
	private static final int FLAT_VIEW = 1;

	/**
	 * constant that specifies the tree view
	 */
	private static final int TREE_VIEW = 2;

	/**
	 * Contextual menu when you right click on a file. Works for both views
	 */
	private CustomContextualMenu contextualMenu;

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
	private JTree tree = new JTree(new StagingResourcesTreeModel(null, false, null));

	/**
	 * Used to fire an event
	 */
	private StageController stageController;

	/**
	 * Shows wheter or not this is the staging panel or the unstaging panel
	 */
	private boolean forStaging;

	/**
	 * The git API, containg the commands
	 */
	private GitAccess gitAccess;

	/**
	 * The active view in the scroll pane
	 */
	private int currentView;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator;

	public ChangesPanel(GitAccess gitAccess, StageController observer, boolean forStaging, Translator translator) {
		this.contextualMenu = new CustomContextualMenu(translator, observer, gitAccess);
		this.forStaging = forStaging;
		this.stageController = observer;
		this.stageController.addTree(this.tree);
		this.gitAccess = gitAccess;
		this.translator = translator;
		ToolTipManager.sharedInstance().registerComponent(tree);
		this.currentView = FLAT_VIEW;

	}

	public JTable getFilesTable() {
		return filesTable;
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
		MyNode rootNode = (MyNode) treeModel.getRoot();
		Enumeration<TreePath> expandedPaths = getLastExpandedPaths();
		stageController.unregisterObserver(treeModel);
		stageController.unregisterSubject(treeModel);

		path = path.replace("\\", "/");
		String rootFolder = path.substring(path.lastIndexOf("/") + 1);
		if (rootNode == null || !rootFolder.equals(rootNode.getUserObject())) {
			MyNode root = new MyNode(rootFolder);
			// Create the tree model and add the root node to it
			treeModel = new StagingResourcesTreeModel(root, false, new ArrayList<FileStatus>(filesStatus));
			if (forStaging) {
				treeModel = new StagingResourcesTreeModel(root, true, new ArrayList<FileStatus>(filesStatus));
			}

			// Create the tree with the new model
			tree.setModel(treeModel);
		}

		treeModel.setFilesStatus(filesStatus);

		stageController.registerObserver(treeModel);
		stageController.registerSubject(treeModel);

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
		MyNode rootNode = (MyNode) treeModel.getRoot();
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
					if (selectionRows.length >= 0) {
						pathForRow = tree.getPathForRow(selectionRows[selectionRows.length - 1]);
					}
					if (pathForRow != null) {
						String stringPath = TreeFormatter.getStringPath(pathForRow);
						MyNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
						if (model != null && node != null) {
							if (model.isLeaf(node) && !model.getRoot().equals(node)) {
								FileStatus file = model.getFileByPath(stringPath);
								DiffPresenter diff = new DiffPresenter(file, stageController, translator);
								diff.showDiff();
							}
						}
					}
				}
			}
		});

		if (!forStaging) {
			List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
			updateFlatView(unstagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), unstagedFiles);
		} else {
			List<FileStatus> stagedFiles = gitAccess.getStagedFile();
			updateFlatView(stagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), stagedFiles);
		}
		stageController.registerObserver(this);
		this.setMinimumSize(new Dimension(Constants.PANEL_WIDTH, Constants.STAGING_PANEl_HEIGHT));
	}

	/**
	 * Adds an expand listener to the tree: When the user expands a node the node
	 * will expand as long as it has only one child.
	 */
	private void addTreeExpandListener() {
		tree.addTreeExpansionListener(new TreeExpansionListener() {

			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
				MyNode node = (MyNode) path.getLastPathComponent();
				if (!model.isLeaf(node)) {
					int children = node.getChildCount();
					if (children == 1) {
						MyNode child = (MyNode) node.getChildAt(0);
						TreePath childPath = new TreePath(child.getPath());
						tree.expandPath(childPath);
					}
				}
			}

			public void treeCollapsed(TreeExpansionEvent event) {
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
		tree.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				StagingResourcesTreeModel model = (StagingResourcesTreeModel) tree.getModel();
				TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
				if (treePath != null) {
					String stringPath = TreeFormatter.getStringPath(treePath);
					MyNode node = TreeFormatter.getTreeNodeFromString(model, stringPath);
					// double click event
					if (model.isLeaf(node) && !model.getRoot().equals(node)) {
						if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
							FileStatus file = model.getFileByPath(stringPath);
							DiffPresenter diff = new DiffPresenter(file, stageController, translator);
							diff.showDiff();
						}

					}
					// right click event
					if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1 && rootHasChilds(node)) {
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
						contextualMenu.removeAll();
						contextualMenu.createContextualMenuFor(model.getFilesByPaths(selectedPaths), forStaging);
						contextualMenu.show(tree, e.getX(), e.getY());
					}
				} else {
					tree.clearSelection();
				}
				toggleSelectedButton();
			}

			public boolean rootHasChilds(MyNode node) {
				return !(node.isRoot() && !node.children().hasMoreElements());
			}

		});

	}

	/**
	 * Adds a listener on the changeAll button: When clicked all the files will go
	 * in the staging or unstaging area, depending on the forStaging variable
	 */
	private void addChangeAllButtonListener() {
		changeAllButton.addActionListener(new ActionListener() {

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

			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
					for (int i = selectedRows.length - 1; i >= 0; i--) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						fileTableModel.switchFileStageState(convertedRow);
					}
				} else {
					List<String> selectedFiles = TreeFormatter.getStringComonAncestor(tree);
					StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
					fileTreeModel.switchFilesStageState(selectedFiles);

				}
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

			TreePath[] selectedPaths = null;

			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					selectedPaths = restoreSelectedPathsFromTableToTree();
					tree.setSelectionPaths(selectedPaths);
					scrollPane.setViewportView(tree);
					currentView = TREE_VIEW;
					switchViewButton.setIcon(Icons.getIcon(ImageConstants.TABLE_VIEW));
					switchViewButton.setToolTipText(translator.getTraslation(Tags.CHANGE_FLAT_VIEW_BUTTON_TOOLTIP));
				} else {
					currentView = FLAT_VIEW;
					switchViewButton.setIcon(Icons.getIcon(ImageConstants.TREE_VIEW));
					switchViewButton.setToolTipText(translator.getTraslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
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
			}

		});
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

			MyNode nodeBuilder = TreeFormatter.getTreeNodeFromString((StagingResourcesTreeModel) tree.getModel(),
					absolutePath);
			MyNode[] selectedPath = new MyNode[absolutePath.split("/").length + 1];
			int count = selectedPath.length;
			while (nodeBuilder != null) {
				count--;
				selectedPath[count] = nodeBuilder;
				nodeBuilder = (MyNode) nodeBuilder.getParent();
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
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		if (forStaging) {
			changeAllButton = new JButton(translator.getTraslation(Tags.UNSTAGE_ALL_BUTTON_TEXT));
		} else {
			changeAllButton = new JButton(translator.getTraslation(Tags.STAGE_ALL_BUTTON_TEXT));
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
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		if (forStaging) {
			changeSelectedButton = new JButton(translator.getTraslation(Tags.UNSTAGE_SELECTED_BUTTON_TEXT));
		} else {
			changeSelectedButton = new JButton(translator.getTraslation(Tags.STAGE_SELECTED_BUTTON_TEXT));
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
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JToolBar toolbar = new JToolBar();
		switchViewButton = new ToolbarButton(null, false);
		switchViewButton.setToolTipText(translator.getTraslation(Tags.CHANGE_TREE_VIEW_BUTTON_TOOLTIP));
		switchViewButton.setIcon(Icons.getIcon(ImageConstants.TREE_VIEW));
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
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		StagingResourcesTableModel fileTableModel = new StagingResourcesTableModel(false);
		if (forStaging) {
			fileTableModel = new StagingResourcesTableModel(true);
		}
		stageController.registerObserver(fileTableModel);
		stageController.registerSubject(fileTableModel);

		filesTable = new JTable(fileTableModel);
		filesTable.setTableHeader(null);
		filesTable.setShowGrid(false);

		// set the checkbox column width
		filesTable.getColumnModel().getColumn(0).setMaxWidth(30);

		// set the button column width
		// filesTable.getColumnModel().getColumn(Constants.STAGE_BUTTON_COLUMN).setMaxWidth(100);
		// TableRendererEditor.install(filesTable, stageController);

		filesTable.getColumnModel().getColumn(0).setCellRenderer(new TableIconCellRenderer());
		filesTable.getColumnModel().getColumn(1).setCellRenderer(new TableFileLocationTextCellRenderer());

		// Adds mouse listener on the table: When the user right clicks on an item
		// in the table, a
		// contextual menu will pop. Also when the user double clicks on a leaf node
		// an action will occur depending on it's file status. If the status is
		// MODIFY,
		// the open in compare editor will be executed, if the status is Add the
		// file
		// will be opened in the Oxygen.
		filesTable.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				int row = filesTable.convertRowIndexToModel(filesTable.rowAtPoint(point));
				int column = filesTable.columnAtPoint(point);
				if (column != 1 || row == -1) {
					filesTable.clearSelection();
				} else {
					if (column == 1 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						openFileInCompareEditor(row);
					}
					if (column == 1 && e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1 && row != -1) {
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
							contextualMenu.removeAll();

							for (int i = 0; i < selectedRows.length; i++) {
								int convertedSelectedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
								StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
								FileStatus file = new FileStatus(model.getUnstageFile(convertedSelectedRow));
								files.add(file);
							}
							contextualMenu.createContextualMenuFor(files, forStaging);
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
		filesTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		filesTable.getActionMap().put("Enter", new AbstractAction() {
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
		DiffPresenter diff = new DiffPresenter(file, stageController, translator);
		diff.showDiff();
	}

	public void stateChanged(ChangeEvent changeEvent) {
		toggleSelectedButton();

	}

	/**
	 * Enable or disable the changeSelected button depending wheter or not
	 * something is selected in the current view(flat or tree)
	 */
	private void toggleSelectedButton() {
		if (currentView == FLAT_VIEW && filesTable.getSelectedRowCount() > 0
				|| currentView == TREE_VIEW && tree.getSelectionCount() > 0) {
			changeSelectedButton.setEnabled(true);
		} else {
			changeSelectedButton.setEnabled(false);
		}
	}

	public JButton getStageSelectedButton() {
		return changeSelectedButton;
	}

	/**
	 * Renderer for the leafs icon in the tree, based on the git change type file
	 * status.
	 * 
	 * @author Beniamin Savu
	 *
	 */
	class CustomTreeIconRenderer extends DefaultTreeCellRenderer {

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
          if (GitChangeType.ADD == changeType
					    || GitChangeType.UNTRACKED == changeType) {
						icon = Icons.getIcon(ImageConstants.GIT_ADD_ICON);
						toolTip = translator.getTraslation(Tags.ADD_ICON_TOOLTIP);
					} else if (GitChangeType.MODIFIED == changeType 
					    || GitChangeType.CHANGED == changeType) {
						icon = Icons.getIcon(ImageConstants.GIT_MODIFIED_ICON);
						toolTip = translator.getTraslation(Tags.MODIFIED_ICON_TOOLTIP);
					} else if (GitChangeType.MISSING == changeType || GitChangeType.REMOVED == changeType) {
						icon = Icons.getIcon(ImageConstants.GIT_DELETE_ICON);
						toolTip = translator.getTraslation(Tags.DELETE_ICON_TOOLTIP);
					} else if (GitChangeType.CONFLICT == changeType) {
						icon = Icons.getIcon(ImageConstants.GIT_CONFLICT_ICON);
						toolTip = translator.getTraslation(Tags.CONFLICT_ICON_TOOLTIP);
					} else if (GitChangeType.SUBMODULE == changeType) {
						icon = Icons.getIcon(ImageConstants.GIT_SUBMODULE_FILE_ICON);
						toolTip = translator.getTraslation(Tags.SUBMODULE_ICON_TOOLTIP);
					}
				}
			}
			label.setIcon(icon);
			label.setToolTipText(toolTip);

			return label;
		}
	}

	/**
	 * Renderer for the text displayed in the table. For exaple the text is
	 * src/main/java/oxygenxml/App.java, the renderer will display "App.java -
	 * src/main/java/oxygen".
	 * 
	 * @author Beniamin Savu
	 *
	 */
	class TableFileLocationTextCellRenderer implements TableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (value != null && value instanceof String) {
				String toRender = (String) value;
				String fileName = toRender.substring(toRender.lastIndexOf("/") + 1);
				if (!fileName.equals(toRender)) {
					toRender = toRender.replace("/" + fileName, "");
					toRender = fileName + " - " + toRender;
				}
				JTextField label = new JTextField(toRender);
				label.setBorder(null);

				if (isSelected) {
					label.setForeground(table.getSelectionForeground());
					label.setBackground(table.getSelectionBackground());
				} else {
					label.setForeground(table.getForeground());
				}
				label.setToolTipText(toRender);
				return label;
			} else {
				return new JTextField();
			}
		}
	}

	/**
	 * Table icon renderer based on the git change file status
	 * 
	 * @author Beniamin Savu
	 *
	 */
	class TableIconCellRenderer implements TableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			ImageIcon icon = null;
			String toolTip = "";
			
			// TODO Extract a common code to be used here and in the com.oxygenxml.git.view.ChangesPanel.CustomTreeIconRenderer
			
			if (GitChangeType.ADD == value || GitChangeType.UNTRACKED == value) {
				icon = Icons.getIcon(ImageConstants.GIT_ADD_ICON);
				toolTip = translator.getTraslation(Tags.ADD_ICON_TOOLTIP);
			} else if (GitChangeType.MODIFIED == value || GitChangeType.CHANGED == value) {
				icon = Icons.getIcon(ImageConstants.GIT_MODIFIED_ICON);
				toolTip = translator.getTraslation(Tags.MODIFIED_ICON_TOOLTIP);
			} else if (GitChangeType.MISSING == value || GitChangeType.REMOVED == value) {
				icon = Icons.getIcon(ImageConstants.GIT_DELETE_ICON);
				toolTip = translator.getTraslation(Tags.DELETE_ICON_TOOLTIP);
			} else if (GitChangeType.CONFLICT == value) {
				icon = Icons.getIcon(ImageConstants.GIT_CONFLICT_ICON);
				toolTip = translator.getTraslation(Tags.CONFLICT_ICON_TOOLTIP);
			} else if (GitChangeType.SUBMODULE == value) {
				icon = Icons.getIcon(ImageConstants.GIT_SUBMODULE_FILE_ICON);
				toolTip = translator.getTraslation(Tags.SUBMODULE_ICON_TOOLTIP);
			}
			JLabel iconLabel = new JLabel(icon);
			iconLabel.setToolTipText(toolTip);
			return iconLabel;
		}
	}

}
