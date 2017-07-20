package com.oxygenxml.sdksamples.workspace.git.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.view.event.ChangeEvent;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.StageState;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

/**
 * 
 * TODO How about we call this Staging(Resources)TableModel? That's pretty much
 * what it does... switches the staging state.
 */
public class StagingResourcesTableModel extends AbstractTableModel implements Subject, Observer {

	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	private Observer observer;
	/**
	 * <code>true</code> if this model presents un-staged resources that will be
	 * staged. <code>false</code> if this model presents staged resources that
	 * will be unstaged.
	 */
	private boolean forStaging;

	public StagingResourcesTableModel(boolean forStaging) {
		this.forStaging = forStaging;
	}

	public int getRowCount() {
		int size;
		if (filesStatus == null) {
			size = 0;
		} else {
			size = filesStatus.size();
		}
		return size;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class clazz = null;
		// TODO Create contants for each index and use them everywhere in this
		// class.
		switch (columnIndex) {
		case 0:
			clazz = ChangeType.class;
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
			temp = filesStatus.get(rowIndex).getChangeType();
			break;
		case 1:
			temp = filesStatus.get(rowIndex).getFileLocation();
			break;
		case 2:
			temp = "Stage";
			break;

		}
		return temp;
	}

	public void setFilesStatus(List<FileStatus> filesStatus) {
		this.filesStatus = filesStatus;
		fireTableDataChanged();

	}

	public void switchFileStageState(int convertedRow) {
		// Update the table model. remove the file.
		FileStatus fileStatus = filesStatus.remove(convertedRow);

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
		return filesStatus.get(convertedRow);
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
		return filesStatus;
	}

	public void switchAllFilesStageState() {
		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, new ArrayList<FileStatus>(filesStatus), this);
		notifyObservers(changeEvent);

		// Update inner model.
		filesStatus.clear();
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
		} else if (changeEvent.getNewState() == StageState.UNSTAGED) {
			if (forStaging) {
				deleteRows(fileToBeUpdated);
			} else {
				insertRows(fileToBeUpdated);
			}
		} else {
			if (forStaging) {
				filesStatus.clear();
			}
		}
		fireTableDataChanged();
	}

	private void deleteRows(List<FileStatus> fileToBeUpdated) {
		filesStatus.removeAll(fileToBeUpdated);

	}

	private void insertRows(List<FileStatus> fileToBeUpdated) {
		filesStatus.addAll(fileToBeUpdated);
	}

	public ChangeType getChangeType(String fullPath) {
		for (FileStatus fileStatus : filesStatus) {
			if (fileStatus.getFileLocation().equals(fullPath)) {
				return fileStatus.getChangeType();
			}
		}
		return null;
	}

	public String getFileLocation(int convertedRow) {
		return filesStatus.get(convertedRow).getFileLocation();
	}

	public List<Integer> getRows(String path) {
		List<Integer> rows = new ArrayList<Integer>();

		for (int i = 0; i < filesStatus.size(); i++) {
			if (filesStatus.get(i).getFileLocation().contains(path)) {
				rows.add(i);
			}

		}
		return rows;
	}

}
