package com.oxygenxml.sdksamples.workspace.git.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.view.event.ChangeEvent;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

public class FileTableModel extends AbstractTableModel implements Subject, Observer {

	private List<FileStatus> unstagedFiles = new ArrayList<FileStatus>();

	private Observer observer;
	private boolean forStaging;

	public FileTableModel(boolean forStaging) {
		this.forStaging = forStaging;
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
			clazz = String.class;
			break;
		case 1:
			clazz = String.class;
			break;
		case 2:
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
			temp = unstagedFiles.get(rowIndex).getChangeType();
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

	public void setUnstagedFiles(List<FileStatus> unstagedFiles) {
		this.unstagedFiles = unstagedFiles;
		fireTableDataChanged();

	}

	public void removeUnstageFile(int convertedRow) {
		// Update the table model. remove the file.
		FileStatus fileStatus = unstagedFiles.remove(convertedRow);

		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		List<FileStatus> fileToBeUpdated = Arrays.asList(new FileStatus[] { fileStatus });
		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, fileToBeUpdated, this);
		notifyObservers(changeEvent);
	}

	public FileStatus getUnstageFile(int convertedRow) {
		return unstagedFiles.get(convertedRow);
	}

	@Override
	public void addObserver(Observer observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	@Override
	public void removeObserver(Observer obj) {
		observer = null;
	}

	public void notifyObservers(ChangeEvent changeEvent) {
		observer.stateChanged(changeEvent);
	}

	public List<FileStatus> getUnstagedFiles() {
		return unstagedFiles;
	}

	public void removeAllFiles() {
		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, new ArrayList<FileStatus>(unstagedFiles), this);
		notifyObservers(changeEvent);

		// Update inner model.
		unstagedFiles.clear();
	}

	@Override
	public void stateChanged(ChangeEvent changeEvent) {

		List<FileStatus> fileToBeUpdated = changeEvent.getFileToBeUpdated();
		if (changeEvent.getNewState() == StageState.STAGED) {
			if (forStaging) {
				insertRows(fileToBeUpdated);
			} else {
				deleteRows(fileToBeUpdated);
			}
		} else {
			if (forStaging) {
				deleteRows(fileToBeUpdated);
			} else {
				insertRows(fileToBeUpdated);
			}
		}
		fireTableDataChanged();
	}

	private void deleteRows(List<FileStatus> fileToBeUpdated) {
		unstagedFiles.removeAll(fileToBeUpdated);

	}

	private void insertRows(List<FileStatus> fileToBeUpdated) {
		unstagedFiles.addAll(fileToBeUpdated);
	}

	public String getChangeType(String fullPath) {
		for (FileStatus fileStatus : unstagedFiles) {
			if (fileStatus.getFileLocation().equals(fullPath)) {
				return fileStatus.getChangeType();
			}
		}
		return "";
	}

	public String getFileLocation(int convertedRow) {
		return unstagedFiles.get(convertedRow).getFileLocation();
	}

	@Override
	public void clear(List<FileStatus> files) {
		if(forStaging){
			deleteRows(files);
			fireTableDataChanged();
		}
	}

}
