package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;



import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;

public class UnstagedChangesPanel extends JPanel {

	private JButton stageAllButton;
	private JButton switchViewButton;
	private JScrollPane filesToBeStaged;
	private FilesPanel filesPanel = new FilesPanel();
	private JScrollPane scrollPane;
	private JTable filesTable;

	public UnstagedChangesPanel() {
		init();
		this.setBorder(BorderFactory.createTitledBorder("UnstagedChanges"));

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

	public JScrollPane getFilesToBeStaged() {
		return filesToBeStaged;
	}

	public void setFilesToBeStaged(JScrollPane filesToBeStaged) {
		this.filesToBeStaged = filesToBeStaged;
	}

	public FilesPanel getFilesPanel() {
		return filesPanel;
	}

	public void setFilesPanel(FilesPanel filesPanel) {
		this.filesPanel = filesPanel;
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void setScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	private void init() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		addStageAllButton(gbc);
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
		gbc.weightx = 1;
		gbc.weighty = 0;
		stageAllButton = new JButton("Stage all");
		this.add(stageAllButton, gbc);
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
		stageAllButton = new JButton("Cheange View");
		this.add(stageAllButton, gbc);

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
		gbc.gridwidth = 2;
		filesTable = new JTable(new FileTableModel());
		filesTable.setTableHeader(null);
		filesTable.setShowGrid(false);
		filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		

		filesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		// set the first column width
		filesTable.getColumnModel().getColumn(0).setMaxWidth(30);
		// set the button column width
		filesTable.getColumnModel().getColumn(2).setMaxWidth(80);
		
		
		
		TableButton tableRendereEditor =  new TableButton(filesTable);
		
		TableColumn column = filesTable.getColumnModel().getColumn(2);
		column.setCellRenderer(tableRendereEditor);
		column.setCellEditor(tableRendereEditor);
		
		scrollPane = new JScrollPane(filesTable);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		filesTable.setFillsViewportHeight(true);
		this.add(scrollPane, gbc);
	}
	
	

}
