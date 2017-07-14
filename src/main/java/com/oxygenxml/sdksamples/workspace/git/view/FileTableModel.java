package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.oxygenxml.sdksamples.workspace.git.service.entities.UnstageFile;

public class FileTableModel extends AbstractTableModel {

	private static final String ADD = "ADD";
	private static final String DELETE = "DELETE";
	private static final String MODIFY = "MODIFY";
	
	private List<UnstageFile> unstagedFiles = new ArrayList<UnstageFile>();
	private List<JLabel> fileChecked = new ArrayList<JLabel>();
	

	public FileTableModel() {
		
	}

	public FileTableModel(List<UnstageFile> unstagedFiles) {
		this.unstagedFiles = unstagedFiles;
		for (UnstageFile unstageFile : unstagedFiles) {
			ImageIcon icon = null;
			switch(unstageFile.getChangeType()){
				case ADD:
					icon = new ImageIcon("src/main/resources/images/GitAdd10.png");
					break;
				case MODIFY:
					icon = new ImageIcon("src/main/resources/images/GitModified10.png");
					break;
				case DELETE:
					icon = new ImageIcon("src/main/resources/images/GitRemoved10.png");
					break;
			}
			fileChecked.add(new JLabel(icon));
		}
	}

	public int getRowCount() {
		int size;
		if (unstagedFiles == null) {
			size = 0;
		} else {
			size = unstagedFiles.size();
		}
		return size;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class clazz = null;
		switch (columnIndex) {
		case 0:
			clazz = JLabel.class;
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
		return column == 2;
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
			temp = unstagedFiles.get(rowIndex).getFileLocation();
			break;
		case 2:
			temp = "Stage";
			break;

		}
		return temp;
	}

	public void setUnstagedFiles(List<UnstageFile> unstagedFiles) {
		this.unstagedFiles = unstagedFiles;
		fileChecked.clear();
		for (UnstageFile unstageFile : unstagedFiles) {
			ImageIcon icon = null;
			switch(unstageFile.getChangeType()){
				case ADD:
					icon = new ImageIcon("src/main/resources/images/GitAdd10.png");
					break;
				case MODIFY:
					icon = new ImageIcon("src/main/resources/images/GitModified10.png");
					break;
				case DELETE:
					icon = new ImageIcon("src/main/resources/images/GitRemoved10.png");
					break;
			}
			fileChecked.add(new JLabel(icon));
		}
		fireTableDataChanged();
		
	}

	public void removeUnstageFile(int convertedRow) {
		unstagedFiles.remove(convertedRow);
		fileChecked.remove(convertedRow);
		
	}

	public UnstageFile getUnstageFile(int convertedRow) {
		return unstagedFiles.get(convertedRow);
	}

	public void addStafeFile(UnstageFile unstageFile) {
		unstagedFiles.add(unstageFile);
		ImageIcon icon = null;
		switch(unstageFile.getChangeType()){
			case ADD:
				icon = new ImageIcon("src/main/resources/images/GitAdd10.png");
				break;
			case MODIFY:
				icon = new ImageIcon("src/main/resources/images/GitModified10.png");
				break;
			case DELETE:
				icon = new ImageIcon("src/main/resources/images/GitRemoved10.png");
				break;
		}
		fileChecked.add(new JLabel(icon));
		
	}

}
