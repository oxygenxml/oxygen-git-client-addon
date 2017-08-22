package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * Custom table model
 * 
 * @author Beniamin Savu
 *
 */
public class StagingResourcesTableModel extends AbstractTableModel
		implements Subject<ChangeEvent>, Observer<ChangeEvent> {

	/**
	 * Constant for the index representing the file status
	 */
	private static final int FILE_STATUS_COLUMN = 0;

	/**
	 * Constant for the index representing the file location
	 */
	private static final int FILE_LOCATION_COLUMN = 1;

	/**
	 * Constant for the index representing the button for stage/unstage
	 */
	private static final int BUTTON_COLUMN = 2;

	/**
	 * The internal representation of the model
	 */
	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	/**
	 * Observer to delegate the event
	 */
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

	/**
	 * Sets the model with the given files, and also sorts it
	 * 
	 * @param filesStatus
	 *          - the files
	 */
	public void setFilesStatus(List<FileStatus> filesStatus) {
		this.filesStatus = filesStatus;
		removeDuplicates();
		Collections.sort(this.filesStatus, new FileStatusComparator());
		fireTableDataChanged();
	}

	/**
	 * Changet the stage state of the file located at the given row
	 * 
	 * @param convertedRow
	 *          - row for the file to change its state
	 */
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

	/**
	 * Returns the file from the given row
	 * 
	 * @param convertedRow
	 *          - the row
	 * @return the file
	 */
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

	/**
	 * Delegate the given event to the observer
	 * 
	 * @param changeEvent
	 *          - the event to delegate
	 */
	public void notifyObservers(ChangeEvent changeEvent) {
		observer.stateChanged(changeEvent);
	}

	public List<FileStatus> getUnstagedFiles() {
		return filesStatus;
	}

	/**
	 * Change the files stage state from unstaged to staged or from staged to
	 * unstaged
	 * 
	 * @param selectedFiles
	 *          - the files to change their stage state
	 */
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
		removeDuplicates();
		Collections.sort(filesStatus, new FileStatusComparator());
		fireTableDataChanged();
	}

	/**
	 * Removes any duplicate entries
	 */
	private void removeDuplicates() {
		Set<FileStatus> set = new HashSet<FileStatus>();
		set.addAll(this.filesStatus);
		this.filesStatus.clear();
		this.filesStatus.addAll(set);
	}

	/**
	 * Delete the given files from the model
	 * 
	 * @param fileToBeUpdated
	 *          - the files to be deleted from the model
	 */
	private void deleteRows(List<FileStatus> fileToBeUpdated) {
		filesStatus.removeAll(fileToBeUpdated);
	}

	/**
	 * Insert the given files to the model
	 * 
	 * @param fileToBeUpdated
	 *          - the files to be inserted in the model
	 */
	private void insertRows(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			if (fileStatus.getChangeType() == GitChangeType.CONFLICT) {
				fileStatus.setChangeType(GitChangeType.MODIFY);
			}
		}
		filesStatus.addAll(fileToBeUpdated);

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

	/**
	 * Return the row from the given file location
	 * 
	 * @param fileLocation
	 *          - the file location
	 * @return the row
	 */
	public int getRow(String fileLocation) {
		for (int i = 0; i < filesStatus.size(); i++) {
			if (filesStatus.get(i).getFileLocation().equals(fileLocation)) {
				return i;
			}
		}
		return -1;
	}

}
