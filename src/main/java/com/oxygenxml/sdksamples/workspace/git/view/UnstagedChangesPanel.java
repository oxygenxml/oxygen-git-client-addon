package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
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
	private JTree tree = new JTree();
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
		// TODO chenge the slashes
		String rootFolder = path.substring(path.lastIndexOf("\\") + 1);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootFolder);

		// Create the tree model and add the root node to it
		DefaultTreeModel modelTree = new FileTreeModel(root);

		// Create the tree with the new model
		tree.setModel(modelTree);
		for (FileStatus unstageFile : unstagedFiles) {
			TreeFormatter.buildTreeFromString(modelTree, unstageFile.getFileLocation());
		}
		tree.expandRow(0);

		stageController.unregisterObserver((Observer) tree.getModel());
		stageController.registerObserver((Observer) tree.getModel());

		stageController.unregisterSubject((Subject) tree.getModel());
		stageController.registerSubject((Subject) tree.getModel());

		// TODO Restore selection.

		this.setTree(tree);
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
				}

			}
		});
	}

	private void addSwitchButtonListener() {

		switchViewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentView == FLAT_VIEW) {
					currentView = TREE_VIEW;
					FileTableModel fileTableModel = (FileTableModel) filesTable.getModel();
					createTreeView(OptionsManager.getInstance().getSelectedRepository(), fileTableModel.getUnstagedFiles());
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
