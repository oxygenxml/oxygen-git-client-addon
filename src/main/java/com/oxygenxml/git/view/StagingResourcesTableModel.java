package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

/**
 * Custom table model
 * 
 * @author Beniamin Savu
 *
 */
public class StagingResourcesTableModel extends AbstractTableModel {
  
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(StagingResourcesTableModel.class);

	/**
	 * Constant for the index representing the file status
	 */
	public static final int FILE_STATUS_COLUMN = 0;

	/**
	 * Constant for the index representing the file location
	 */
	public static final int FILE_LOCATION_COLUMN = 1;

	/**
	 * The internal representation of the model
	 */
	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	/**
	 * Compares file statuses.
	 */
	private Comparator<FileStatus> fileStatusComparator = new Comparator<FileStatus>() {
    @Override
    public int compare(FileStatus f1, FileStatus f2) {
      int changeTypeCompareResult = f1.getChangeType().compareTo(f2.getChangeType());
      if(changeTypeCompareResult == 0) {
        return f1.getFileLocation().compareTo(f2.getFileLocation());
      } else {
        return changeTypeCompareResult;
      }
    }
  };

	/**
	 * <code>true</code> if this model presents the resources from the index.
	 * <code>false</code> if it presents the modified resources that can be put in the index.
	 */
	private boolean inIndex;

  private StageController stageController;

	public StagingResourcesTableModel(StageController stageController, boolean inIndex) {
		this.stageController = stageController;
    this.inIndex = inIndex;
	}

	@Override
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
	  Class<?> clazz = null;
	  switch (columnIndex) {
	    case FILE_STATUS_COLUMN:
	      clazz = ChangeType.class;
	      break;
	    case FILE_LOCATION_COLUMN:
	      clazz = String.class;
	      break;
	    default:
	      break;
	  }
	  return clazz;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
  public int getColumnCount() {
		return 2;
	}

	@Override
  public Object getValueAt(int rowIndex, int columnIndex) {
		Object temp = null;
		switch (columnIndex) {
		  case FILE_STATUS_COLUMN:
		    temp = filesStatus.get(rowIndex).getChangeType();
		    break;
		  case FILE_LOCATION_COLUMN:
		    temp = filesStatus.get(rowIndex).getFileLocation();
		    break;
		  default:
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
		Collections.sort(this.filesStatus, fileStatusComparator);
		fireTableDataChanged();
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

	/**
	 * @return The files in the model.
	 */
	public List<FileStatus> getFilesStatuses() {
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
		List<FileStatus> filesToBeUpdated = new ArrayList<FileStatus>();
		for (FileStatus fileStatus : filesStatus) {
			if (fileStatus.getChangeType() != GitChangeType.CONFLICT) {
				filesToBeUpdated.add(fileStatus);
			}
		}
		
		GitCommand action = GitCommand.UNSTAGE;
		if (!inIndex) {
		  action = GitCommand.STAGE;
		}
		
    stageController.doGitCommand(filesToBeUpdated, action);
	}

	/**
	 * Some resources changed.
	 * 
	 * @param changeEvent Change information.
	 */
	public void stateChanged(ChangeEvent changeEvent) {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Change event " + (inIndex ? " un-staged " : "staged  ") + changeEvent);
	  }
	  
		List<FileStatus> newStates = 
		    inIndex ? 
		        GitAccess.getInstance().getStagedFile(changeEvent.getChangedFiles()) :
		        GitAccess.getInstance().getUnstagedFiles(changeEvent.getChangedFiles());
    List<FileStatus> oldStates = changeEvent.getOldStates();
    
    if (changeEvent.getCommand() == GitCommand.STAGE) {
			if (inIndex) {
				insertRows(newStates);
			} else {
				deleteRows(oldStates);
			}
		} else if (changeEvent.getCommand() == GitCommand.UNSTAGE) {
			if (inIndex) {
				deleteRows(oldStates);
			} else {
			  // Things were taken out of the INDEX. 
			  // The same resource might be present in the UnStaged and INDEX. Remove old states.
			  deleteRows(oldStates);
				insertRows(newStates);
			}
		} else if (changeEvent.getCommand() == GitCommand.COMMIT) {
			if (inIndex) {
			  // Committed files are removed from the INDEX.
				filesStatus.clear();
			}
		} else if (changeEvent.getCommand() == GitCommand.DISCARD) {
		  // Discarded files are no longer presented by neither model.
			deleteRows(oldStates);
		} else if (changeEvent.getCommand() == GitCommand.MERGE_RESTART) {
		  filesStatus.clear();
		  List<FileStatus> fileStatuses = inIndex ? GitAccess.getInstance().getStagedFile() :
		    GitAccess.getInstance().getUnstagedFiles();
		  insertRows(fileStatuses);
		}
		
		removeDuplicates();
		Collections.sort(filesStatus, fileStatusComparator);
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
		filesStatus.addAll(fileToBeUpdated);

	}

	public String getFileLocation(int convertedRow) {
		return filesStatus.get(convertedRow).getFileLocation();
	}
	
	public FileStatus getFileStatus(int convertedRow) {
    return filesStatus.get(convertedRow);
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
