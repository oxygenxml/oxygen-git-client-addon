package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class FileTableModel extends AbstractTableModel {

	private List<String> fileNames = new ArrayList<String>();
	private List<Boolean> fileChecked = new ArrayList<Boolean>();

	public FileTableModel() {
		this.fileNames.add("teste");
		this.fileNames.add("pocpac");
		this.fileNames.add("pac");
		this.fileNames.add("poc");
		for (int i = 0; i < fileNames.size(); i++) {
			fileChecked.add(false);
		}
	}

	public FileTableModel(List<String> fileNames) {
		this.fileNames = fileNames;
		for (int i = 0; i < fileNames.size(); i++) {
			fileChecked.add(false);
		}
	}

	public int getRowCount() {
		int size;
		if (fileNames == null) {
			size = 0;
		} else {
			size = fileNames.size();
		}
		return size;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class clazz = null;
		switch (columnIndex) {
		case 0:
			clazz = Boolean.class;
			break;
		case 1:
			clazz = String.class;
			break;
		case 2:
			// TODO Same as column 0
			//clazz = JButton.class;
			clazz = String.class;
			break;
		}
		return clazz;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		if (aValue instanceof Boolean && column == 0) {
			fileChecked.set(row, (Boolean) aValue);
			fireTableCellUpdated(row, column);
		}
	}

	public int getColumnCount() {
		return 3;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		Object temp = null;
		switch (columnIndex) {
		case 0:
			temp = fileChecked.get(rowIndex);
			break;
		case 1:
			temp = fileNames.get(rowIndex);
			break;
		case 2:
			/*JButton button = new JButton("Stage");
			button.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					System.err.println("merge");

				}
			});*/
			temp = "Stage";
			break;

		}
		return temp;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
		for (int i = 0; i < fileNames.size(); i++) {
			fileChecked.add(false);
		}
		fireTableDataChanged();
		
	}

}
