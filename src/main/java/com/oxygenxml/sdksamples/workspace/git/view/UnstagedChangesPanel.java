package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.utils.TreeFormatter;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageController;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

public class UnstagedChangesPanel extends JPanel {

	private static final int FLAT_VIEW = 1;
	private static final int TREE_VIEW = 2;

	private JButton stageAllButton;
	private JButton stageSelectedButton;
	private JButton switchViewButton;
	private JScrollPane scrollPane;
	private JTable filesTable;
	private JTree tree = new JTree(new FileTreeModel(null, false));
	private StageController stageController;

	private boolean staging;

	private GitAccess gitAccess;

	private int currentView = 0;

	public UnstagedChangesPanel(GitAccess gitAccess, StageController observer, boolean staging) {
		this.staging = staging;
		this.stageController = observer;
		this.gitAccess = gitAccess;
		currentView = FLAT_VIEW;
		if (staging) {
			this.setBorder(BorderFactory.createTitledBorder("StagedChanges"));
		} else {
			this.setBorder(BorderFactory.createTitledBorder("UnstagedChanges"));
		}

	}

	public JTree getTree() {
		return tree;
	}

	public void setTree(JTree tree) {
		if (currentView == TREE_VIEW) {
			scrollPane.setViewportView(tree);
		}

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

	public void createTreeView(String path, List<FileStatus> unstagedFiles) {
		stageController.unregisterObserver((Observer) tree.getModel());
		stageController.unregisterSubject((Subject) tree.getModel());

		path = path.replace("\\", "/");
		String rootFolder = path.substring(path.lastIndexOf("/") + 1);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootFolder);

		// Create the tree model and add the root node to it
		DefaultTreeModel modelTree = new FileTreeModel(root, false);
		if (staging) {
			modelTree = new FileTreeModel(root, true);
		}

		// Create the tree with the new model
		tree.setModel(modelTree);
		for (FileStatus unstageFile : unstagedFiles) {
			TreeFormatter.buildTreeFromString(modelTree, unstageFile.getFileLocation());
		}
		expandAllNodes(tree, 0, tree.getRowCount());

		stageController.registerObserver((Observer) tree.getModel());
		stageController.registerSubject((Subject) tree.getModel());

		// TODO Restore selection.

		this.setTree(tree);
	}

	private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			tree.expandRow(i);
		}

		if (tree.getRowCount() != rowCount) {
			expandAllNodes(tree, rowCount, tree.getRowCount());
		}
	}

	public void createFlatView(List<FileStatus> unstagedFiles) {

		FileTableModel modelTable = (FileTableModel) this.getFilesTable().getModel();
		modelTable.setUnstagedFiles(unstagedFiles);
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
	}

	private void addStageAllButtonListener() {
		stageAllButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FileTableModel fileTableModel = (FileTableModel) filesTable.getModel();
				fileTableModel.removeAllFiles();
			}
		});
	}

	private void addStageSelectedButtonListener() {
		stageSelectedButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					FileTableModel fileTableModel = (FileTableModel) filesTable.getModel();
					for (int i = selectedRows.length - 1; i >= 0; i--) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						fileTableModel.removeUnstageFile(convertedRow);
					}
				} else {
					TreePath[] selectedPaths = tree.getSelectionPaths();
					List<FileStatus> selectedFiles = new ArrayList<FileStatus>();
					String fullPath = "";
					for (int i = 0; i < selectedPaths.length; i++) {
						Object[] pathNodes = selectedPaths[i].getPath();

						for (int j = 1; j < pathNodes.length; j++) {
							if (j == pathNodes.length - 1) {
								fullPath += pathNodes[j];
							} else {
								fullPath += pathNodes[j] + "/";
							}

						}
						FileTableModel fileTableModel = (FileTableModel) filesTable.getModel();
						String changeType = fileTableModel.getChangeType(fullPath);
						selectedFiles.add(new FileStatus(changeType, fullPath));
						fullPath = "";
					}
					FileTreeModel fileTreeModel = (FileTreeModel) tree.getModel();
					fileTreeModel.removeUnstageFiles(selectedFiles);

				}
				expandAllNodes(tree, 0, tree.getRowCount());

			}
		});
	}

	private void addSwitchButtonListener() {

		switchViewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					int[] selectedRows = filesTable.getSelectedRows();
					FileTableModel fileTableModel = (FileTableModel) filesTable.getModel();

					TreePath[] selectedPaths = new TreePath[selectedRows.length];
					for (int i = 0; i < selectedRows.length; i++) {
						int convertedRow = filesTable.convertRowIndexToModel(selectedRows[i]);
						String[] path = fileTableModel.getFileLocation(convertedRow).split("/");
						
						DefaultMutableTreeNode root = new DefaultMutableTreeNode("Oxygen-Git-Plugin");
						DefaultMutableTreeNode node = root;
						for (int j = 0; j < path.length; j++) {
							DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(path[j]);
							node.add(newNode);
							node = newNode;
						}
						selectedPaths[i] = new TreePath(root);
					}
					tree.setSelectionPaths(selectedPaths);
					currentView = TREE_VIEW;
					expandAllNodes(tree, 0, tree.getRowCount());
					scrollPane.setViewportView(tree);
				} else {
					currentView = FLAT_VIEW;
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
		switchViewButton = new JButton("Change View");
		this.add(switchViewButton, gbc);

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
		FileTableModel fileTableModel = new FileTableModel(false);
		if (staging) {
			fileTableModel = new FileTableModel(true);
		}
		stageController.registerObserver(fileTableModel);
		stageController.registerSubject(fileTableModel);

		filesTable = new JTable(fileTableModel);
		filesTable.setTableHeader(null);
		filesTable.setShowGrid(false);

		// set the checkbox column width
		filesTable.getColumnModel().getColumn(0).setMaxWidth(30);
		// set the button column width
		filesTable.getColumnModel().getColumn(Constants.STAGE_BUTTON_COLUMN).setMaxWidth(80);

		TableRendererEditor.install(filesTable);

		filesTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				ImageIcon icon = null;
				switch ((String) value) {
				case "ADD":
					icon = new ImageIcon("src/main/resources/images/GitAdd10.png");
					break;
				case "MODIFY":
					icon = new ImageIcon("src/main/resources/images/GitModified10.png");
					break;
				case "DELETE":
					icon = new ImageIcon("src/main/resources/images/GitRemoved10.png");
					break;
				}
				return new JLabel(icon);

			}
		});

		scrollPane = new JScrollPane(filesTable);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		filesTable.setFillsViewportHeight(true);
		this.add(scrollPane, gbc);
	}

}
