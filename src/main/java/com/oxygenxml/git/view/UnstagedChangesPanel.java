package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortingFocusTraversalPolicy;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;
import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.StageController;

public class UnstagedChangesPanel extends JPanel {

	private static final int FLAT_VIEW = 1;
	private static final int TREE_VIEW = 2;

	private JPopupMenu contextualMenu = new JPopupMenu();
	private JButton stageAllButton;
	private JButton stageSelectedButton;
	private JButton switchViewButton;
	private JScrollPane scrollPane;
	private JTable filesTable;
	private JTree tree = new JTree(new StagingResourcesTreeModel(null, false, null));
	private StageController stageController;

	private boolean staging;

	private GitAccess gitAccess;

	private int currentView = 0;

	public UnstagedChangesPanel(GitAccess gitAccess, StageController observer, boolean staging) {
		this.staging = staging;
		this.stageController = observer;
		this.gitAccess = gitAccess;
		currentView = FLAT_VIEW;

	}

	public JTree getTree() {
		return tree;
	}

	public JTable getFilesTable() {
		return filesTable;
	}

	public void setFilesTable(JTable filesTable) {
		this.filesTable = filesTable;
	}

	public JButton getStageAllButton() {
		return stageAllButton;
	}

	public void setStageAllButton(JButton stageAllButton) {
		this.stageAllButton = stageAllButton;
	}

	public JButton getSwitchViewButton() {
		return switchViewButton;
	}

	public void setSwitchViewButton(JButton switchViewButton) {
		this.switchViewButton = switchViewButton;
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	public void createTreeView(String path, List<FileStatus> filesStatus) {
		StagingResourcesTreeModel treeModel = (StagingResourcesTreeModel) tree.getModel();
		stageController.unregisterObserver(treeModel);
		stageController.unregisterSubject(treeModel);

		path = path.replace("\\", "/");
		String rootFolder = path.substring(path.lastIndexOf("/") + 1);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootFolder);

		// Create the tree model and add the root node to it
		treeModel = new StagingResourcesTreeModel(root, false, new ArrayList<FileStatus>(filesStatus));
		if (staging) {
			treeModel = new StagingResourcesTreeModel(root, true, new ArrayList<FileStatus>(filesStatus));
		}

		// Create the tree with the new model
		tree.setModel(treeModel);
		for (FileStatus unstageFile : filesStatus) {
			TreeFormatter.buildTreeFromString(treeModel, unstageFile.getFileLocation());
		}

		stageController.registerObserver(treeModel);
		stageController.registerSubject(treeModel);
	}

	public void updateFlatView(List<FileStatus> unstagedFiles) {
		StagingResourcesTableModel modelTable = (StagingResourcesTableModel) this.getFilesTable().getModel();
		modelTable.setFilesStatus(unstagedFiles);
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		addStageAllButton(gbc);
		addStageSelectedButton(gbc);
		addSwitchViewButton(gbc);
		addFilesPanel(gbc);
		
		addSwitchButtonListener();
		addStageSelectedButtonListener();
		addStageAllButtonListener();
	
		if(!staging){
			List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
			updateFlatView(unstagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), unstagedFiles);
		} else {
			List<FileStatus> stagedFiles = gitAccess.getStagedFile();
			updateFlatView(stagedFiles);
			createTreeView(OptionsManager.getInstance().getSelectedRepository(), stagedFiles);
		}
	}

	private void addStageAllButtonListener() {
		stageAllButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
				fileTableModel.switchAllFilesStageState();
			}
		});
	}

	private void addStageSelectedButtonListener() {
		stageSelectedButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();
					for (int i = selectedRows.length - 1; i >= 0; i--) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						fileTableModel.switchFileStageState(convertedRow);
					}
				} else {
					List<String> selectedFiles = new ArrayList<String>();
					TreePath[] selectedPaths = tree.getSelectionPaths();
					List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(selectedPaths);
					String fullPath = "";
					for (TreePath treePath : commonAncestors) {
						fullPath = TreeFormatter.getStringPath(treePath);
						selectedFiles.add(new String(fullPath));
						fullPath = "";
					}
					StagingResourcesTreeModel fileTreeModel = (StagingResourcesTreeModel) tree.getModel();
					fileTreeModel.switchFilesStageState(selectedFiles);

				}
			}

		});
	}

	private void addSwitchButtonListener() {

		switchViewButton.addActionListener(new ActionListener() {

			TreePath[] selectedPaths = null;

			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					StagingResourcesTableModel fileTableModel = (StagingResourcesTableModel) filesTable.getModel();

					selectedPaths = new TreePath[selectedRows.length];
					for (int i = 0; i < selectedRows.length; i++) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						String absolutePath = fileTableModel.getFileLocation(convertedRow);

						DefaultMutableTreeNode nodeBuilder = TreeFormatter
								.getTreeNodeFromString((StagingResourcesTreeModel) tree.getModel(), absolutePath);
						DefaultMutableTreeNode[] selectedPath = new DefaultMutableTreeNode[absolutePath.split("/").length + 1];
						int count = selectedPath.length;
						while (nodeBuilder != null) {
							count--;
							selectedPath[count] = nodeBuilder;
							nodeBuilder = (DefaultMutableTreeNode) nodeBuilder.getParent();
						}

						selectedPaths[i] = new TreePath(selectedPath);
					}
					tree.setSelectionPaths(selectedPaths);

					scrollPane.setViewportView(tree);
					currentView = TREE_VIEW;
					switchViewButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.TABLE_VIEW)));
				} else {
					currentView = FLAT_VIEW;
					switchViewButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.TREE_VIEW)));
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

	private void addStageAllButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		if (staging) {
			stageAllButton = new JButton("Unstage all");
		} else {
			stageAllButton = new JButton("Stage all");
		}
		this.add(stageAllButton, gbc);
	}

	private void addStageSelectedButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		if (staging) {
			stageSelectedButton = new JButton("Unstage selected");
		} else {
			stageSelectedButton = new JButton("Stage selected");
		}
		this.add(stageSelectedButton, gbc);

	}

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
		switchViewButton = new JButton();
		switchViewButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.TREE_VIEW)));
		toolbar.add(switchViewButton);
		toolbar.setFloatable(false);
		this.add(toolbar, gbc);

	}

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
		if (staging) {
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
		filesTable.getColumnModel().getColumn(Constants.STAGE_BUTTON_COLUMN).setMaxWidth(100);

		TableRendererEditor.install(filesTable);

		filesTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				ImageIcon icon = null;
				String toolTip = "";
				if (GitChangeType.ADD == value) {
					icon = new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_ADD_ICON));
					toolTip = "File Created";
				} else if (GitChangeType.MODIFY == value) {
					icon = new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_MODIFIED_ICON));
					toolTip = "File Modified";
				} else if (GitChangeType.DELETE == value) {
					icon = new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_DELETE_ICON));
					toolTip = "File Deleted";
				} else if (GitChangeType.CONFLICT == value) {
					icon = new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_CONFLICT_ICON));
					toolTip = "Conflict";
				}
				JLabel iconLabel = new JLabel(icon);
				iconLabel.setToolTipText(toolTip);
				return iconLabel;
			}
		});

		filesTable.getColumnModel().getColumn(1).setCellRenderer(new TableCellRenderer() {

			public Component getTableCellRendererComponent(final JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

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
					label.setBackground(UIManager.getColor("Button.background"));

				}
				label.setToolTipText(toRender);
				return label;

			}
		});

		filesTable.addMouseListener(new MouseListener() {

			public void mouseReleased(MouseEvent e) {

			}

			public void mousePressed(MouseEvent e) {
				Point point = new Point(e.getX(), e.getY());
				int row = filesTable.convertRowIndexToModel(filesTable.rowAtPoint(point));
				int column = filesTable.columnAtPoint(point);
				if (column == 1 && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
					int convertedRow = filesTable.convertRowIndexToModel(row);
					FileStatus file = model.getUnstageFile(convertedRow);
					DiffPresenter diff = new DiffPresenter(file);
					diff.showDiff();
				}
				if (column == 1 && e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1) {

					int clickedRow = filesTable.rowAtPoint(e.getPoint());
					if (clickedRow >= 0 && clickedRow < filesTable.getRowCount()) {
						filesTable.setRowSelectionInterval(clickedRow, clickedRow);
					} else {
						filesTable.clearSelection();
					}
					contextualMenu.removeAll();
					FileStatus file = getSelectedFile();
					addContextualMenu(file);
					contextualMenu.show(filesTable, e.getX(), e.getY());
				}
			}

			public void mouseExited(MouseEvent e) {

			}

			public void mouseEntered(MouseEvent e) {

			}

			public void mouseClicked(MouseEvent e) {

			}
		});

		scrollPane = new JScrollPane(filesTable);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		filesTable.setFillsViewportHeight(true);
		this.add(scrollPane, gbc);
	}

	private void addContextualMenu(final FileStatus file) {
		//Open menu
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				DiffPresenter diff = new DiffPresenter(file);
				diff.openFile();
			}

		});
		contextualMenu.add(open);

		//Show Diff menu
		JMenuItem showDiff = new JMenuItem("Show Diff");
		if (file.getChangeType() == GitChangeType.ADD || file.getChangeType() == GitChangeType.DELETE) {
			showDiff.setEnabled(false);
		}
		showDiff.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				DiffPresenter diff = new DiffPresenter(file);
				diff.showDiff();
			}
		});
		contextualMenu.add(showDiff);

		//Discard Menu
		JMenuItem discard = new JMenuItem("Discard");
		discard.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (file.getChangeType() == GitChangeType.ADD) {
					try {
						FileUtils.forceDelete(new File(OptionsManager.getInstance().getSelectedRepository() + "/" + file.getFileLocation()));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else {
					gitAccess.remove(file);
					gitAccess.restoreLastCommit(file.getFileLocation());
				}
			}
		});
		contextualMenu.add(discard);
	}

	private FileStatus getSelectedFile() {
		int selectedRow = filesTable.getSelectedRow();
		int convertedSelectedRow = filesTable.convertRowIndexToModel(selectedRow);
		StagingResourcesTableModel model = (StagingResourcesTableModel) filesTable.getModel();
		FileStatus file = new FileStatus(model.getUnstageFile(convertedSelectedRow));
		return file;
	}

}
