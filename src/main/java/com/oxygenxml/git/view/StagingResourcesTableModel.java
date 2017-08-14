package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusComparator;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.StageState;
import com.oxygenxml.git.view.event.Subject;

/**
 * 
 */
public class StagingResourcesTableModel extends AbstractTableModel
		implements Subject<ChangeEvent>, Observer<ChangeEvent> {

	private static final int FILE_STATUS_COLUMN = 0;
	private static final int FILE_LOCATION_COLUMN = 1;
	private static final int BUTTON_COLUMN = 2;

	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	private Observer<ChangeEvent> observer;
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
		switch (columnIndex) {
		case FILE_STATUS_COLUMN:
			clazz = ChangeType.class;
			break;
		case FILE_LOCATION_COLUMN:
			clazz = String.class;
			break;
		case BUTTON_COLUMN:
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
		return 2;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		Object temp = null;
		switch (columnIndex) {
		case FILE_STATUS_COLUMN:
			temp = filesStatus.get(rowIndex).getChangeType();
			break;
		case FILE_LOCATION_COLUMN:
			temp = filesStatus.get(rowIndex).getFileLocation();
			break;
		case BUTTON_COLUMN:
			if (forStaging) {
				temp = "Unstage";
			} else {
				temp = "Stage";
			}
			break;

		}
		return temp;
	}

	public void setFilesStatus(List<FileStatus> filesStatus) {
		this.filesStatus = filesStatus;
		Collections.sort(filesStatus, new FileStatusComparator());
		fireTableDataChanged();
	}

	public void switchFileStageState(int convertedRow) {
		if (filesStatus.get(convertedRow).getChangeType() == GitChangeType.CONFLICT) {
			return;
		}
		// Update the table model. remove the file.
		FileStatus fileStatus = filesStatus.remove(convertedRow);

		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		List<FileStatus> fileToBeUpdated = Arrays.asList(new FileStatus[] { fileStatus });
		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, fileToBeUpdated);
		notifyObservers(changeEvent);
	}

	public FileStatus getUnstageFile(int convertedRow) {
		return filesStatus.get(convertedRow);
	}

	public void addObserver(Observer<ChangeEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	public void removeObserver(Observer<ChangeEvent> obj) {
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

		List<FileStatus> filesToBeUpdated = new ArrayList<FileStatus>();
		for (FileStatus fileStatus : filesStatus) {
			if (fileStatus.getChangeType() != GitChangeType.CONFLICT) {
				filesToBeUpdated.add(fileStatus);
			}
		}
		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, filesToBeUpdated);
		notifyObservers(changeEvent);
	}

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
		} else if (changeEvent.getNewState() == StageState.COMMITED) {
			if (forStaging) {
				filesStatus.clear();
			}
		} else if (changeEvent.getNewState() == StageState.DISCARD) {
			deleteRows(fileToBeUpdated);
		}
		fireTableDataChanged();
	}

	private void deleteRows(List<FileStatus> fileToBeUpdated) {
		filesStatus.removeAll(fileToBeUpdated);
		Collections.sort(filesStatus, new FileStatusComparator());
	}

	private void insertRows(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			if (fileStatus.getChangeType() == GitChangeType.CONFLICT) {
				fileStatus.setChangeType(GitChangeType.MODIFY);
			}
		}
		filesStatus.addAll(fileToBeUpdated);
		Collections.sort(filesStatus, new FileStatusComparator());
	}

	public GitChangeType getChangeType(String fullPath) {
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

	/**
	 * Gets all the file indexes from the given folder
	 * 
	 * @param path
	 *          - the folder from which to get the file indexes
	 * @return a list containing the file indexes
	 */
	public List<Integer> getRows(String path) {
		List<Integer> rows = new ArrayList<Integer>();

		for (int i = 0; i < filesStatus.size(); i++) {
			if (filesStatus.get(i).getFileLocation().contains(path)) {
				rows.add(i);
			}

		}
		return rows;
	}

	public int getRow(String fileLocation) {
		for (int i = 0; i < filesStatus.size(); i++) {
			if (filesStatus.get(i).getFileLocation().equals(fileLocation)) {
				return i;
			}
		}
		return -1;
	}

}
