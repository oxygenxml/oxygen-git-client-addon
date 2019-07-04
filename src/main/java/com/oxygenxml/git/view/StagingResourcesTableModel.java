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
	private List<FileStatus> filesStatuses = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Compares file statuses.
	 */
	private Comparator<FileStatus> fileStatusComparator = (f1, f2) -> {
    int changeTypeCompareResult = f1.getChangeType().compareTo(f2.getChangeType());
    if(changeTypeCompareResult == 0) {
      return f1.getFileLocation().compareTo(f2.getFileLocation());
    } else {
      return changeTypeCompareResult;
    }
  };

	/**
	 * <code>true</code> if this model presents the resources from the index.
	 * <code>false</code> if it presents the modified resources that can be put in the index.
	 */
	private boolean inIndex;

	/**
	 * Staging controller.
	 */
  private StageController stageController;

  /**
   * Constructor.
   * 
   * @param stageController Staging controller.
   * @param inIndex         <code>true</code> if this is the model of the staged files.
   */
	public StagingResourcesTableModel(StageController stageController, boolean inIndex) {
		this.stageController = stageController;
    this.inIndex = inIndex;
	}

	@Override
  public int getRowCount() {
		return filesStatuses != null ? filesStatuses.size() : 0;
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
		    temp = filesStatuses.get(rowIndex).getChangeType();
		    break;
		  case FILE_LOCATION_COLUMN:
		    temp = filesStatuses.get(rowIndex).getFileLocation();
		    break;
		  default:
		    break;

		}
		return temp;
	}

	/**
	 * Sets the model with the given files, and also sorts it
	 * 
	 * @param filesStatuses
	 *          - the files
	 */
	public void setFilesStatus(List<FileStatus> filesStatuses) {
	  fireTableRowsDeleted(0, getRowCount());
	  
		this.filesStatuses = Collections.synchronizedList(new ArrayList<>(filesStatuses));
		removeDuplicates();
		Collections.sort(this.filesStatuses, fileStatusComparator);
		
		fireTableRowsInserted(0, getRowCount());
	}

	/**
	 * Returns the file from the given row
	 * 
	 * @param convertedRow
	 *          - the row
	 * @return the file
	 */
	public FileStatus getUnstageFile(int convertedRow) {
		return filesStatuses.get(convertedRow);
	}

	/**
	 * @return The files in the model.
	 */
	public List<FileStatus> getFilesStatuses() {
		return filesStatuses;
	}

	/**
	 * Change the files stage state from unstaged to staged or from staged to
	 * unstaged
	 * 
	 * @param selectedFiles
	 *          - the files to change their stage state
	 */
	public void switchAllFilesStageState() {
		List<FileStatus> filesToBeUpdated = new ArrayList<>();
		synchronized (filesStatuses) {
		  for (FileStatus fileStatus : filesStatuses) {
		    if (fileStatus.getChangeType() != GitChangeType.CONFLICT) {
		      filesToBeUpdated.add(fileStatus);
		    }
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
				filesStatuses.clear();
			}
		} else if (changeEvent.getCommand() == GitCommand.DISCARD) {
		  // Discarded files are no longer presented by neither model.
			deleteRows(oldStates);
		} else if (changeEvent.getCommand() == GitCommand.MERGE_RESTART) {
		  filesStatuses.clear();
		  List<FileStatus> fileStatuses = inIndex ? GitAccess.getInstance().getStagedFiles() :
		    GitAccess.getInstance().getUnstagedFiles();
		  insertRows(fileStatuses);
		}
		
		removeDuplicates();
		Collections.sort(filesStatuses, fileStatusComparator);
		fireTableDataChanged();
	}

	/**
	 * Removes any duplicate entries
	 */
	private void removeDuplicates() {
		Set<FileStatus> set = new HashSet<>();
		set.addAll(this.filesStatuses);
		this.filesStatuses.clear();
		this.filesStatuses.addAll(set);
	}

	/**
	 * Delete the given files from the model
	 * 
	 * @param fileToBeUpdated
	 *          - the files to be deleted from the model
	 */
	private void deleteRows(List<FileStatus> fileToBeUpdated) {
		filesStatuses.removeAll(fileToBeUpdated);
	}

	/**
	 * Insert the given files to the model
	 * 
	 * @param fileToBeUpdated
	 *          - the files to be inserted in the model
	 */
	private void insertRows(List<FileStatus> fileToBeUpdated) {
		filesStatuses.addAll(fileToBeUpdated);

	}

	public String getFileLocation(int convertedRow) {
		return filesStatuses.get(convertedRow).getFileLocation();
	}
	
	public FileStatus getFileStatus(int convertedRow) {
    return filesStatuses.get(convertedRow);
  }

	/**
	 * Gets all the file indexes from the given folder
	 * 
	 * @param path
	 *          - the folder from which to get the file indexes
	 * @return a list containing the file indexes
	 */
	public List<Integer> getRows(String path) {
	  List<Integer> rows = new ArrayList<>();
	  synchronized (filesStatuses) {
	    for (int i = 0; i < filesStatuses.size(); i++) {
	      if (filesStatuses.get(i).getFileLocation().contains(path)) {
	        rows.add(i);
	      }
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
	  synchronized (filesStatuses) {
	    for (int i = 0; i < filesStatuses.size(); i++) {
	      if (filesStatuses.get(i).getFileLocation().equals(fileLocation)) {
	        return i;
	      }
	    }
	  }
	  return -1;
	}

}
