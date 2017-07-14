package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTable;

public class StageCommand implements ActionListener{

	private JTable unstagedTable;
	private JTable stagedTabel;
	private int row;
	
	public StageCommand(JTable unstagedTable, JTable stagedTable, int row) {
		this.unstagedTable = unstagedTable;
		this.stagedTabel =stagedTable;
		this.row = 0;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		FileTableModel unstagedModel = (FileTableModel) unstagedTable.getModel();
	}
	

}
