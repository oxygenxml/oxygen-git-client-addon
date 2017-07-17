package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageController;

public class StagedChangesPanel extends JPanel {

	private JButton unstageAllButton;
	private JButton switchViewButton;
	private JButton unstageSelectedButton;
	private JScrollPane scrollPane;
	private JTable filesTable;;
	private GitAccess gitAccess;
	private StageController stageController;

	public StagedChangesPanel(GitAccess gitAccess, StageController observer) {
		this.stageController = observer;
		this.gitAccess = gitAccess;
		this.setBorder(BorderFactory.createTitledBorder("StagedChanges"));
	}

	public JButton getUnstageAllButton() {
		return unstageAllButton;
	}

	public void setUnstageAllButton(JButton unstageAllButton) {
		this.unstageAllButton = unstageAllButton;
	}

	public JButton getSwitchViewButton() {
		return switchViewButton;
	}

	public void setSwitchViewButton(JButton switchViewButton) {
		this.switchViewButton = switchViewButton;
	}

	public JButton getUnstageSelectedButton() {
		return unstageSelectedButton;
	}

	public void setUnstageSelectedButton(JButton unstageSelectedButton) {
		this.unstageSelectedButton = unstageSelectedButton;
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	public JTable getFilesTable() {
		return filesTable;
	}

	public void setFilesTable(JTable filesTable) {
		this.filesTable = filesTable;
	}

	public GitAccess getGitAccess() {
		return gitAccess;
	}

	public void setGitAccess(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		addStageAllButton(gbc);
		addStageSelectedButton(gbc);
		addSwitchViewButton(gbc);
		addFilesPanel(gbc);
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
		unstageAllButton = new JButton("Unstage all");
		this.add(unstageAllButton, gbc);
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
		unstageAllButton = new JButton("Unstage selected");
		this.add(unstageAllButton, gbc);
	}

	private void addSwitchViewButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		unstageAllButton = new JButton("Cheange View");
		this.add(unstageAllButton, gbc);

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
