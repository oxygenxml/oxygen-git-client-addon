package com.oxygenxml.git.view.staging;

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
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitEventInfo;

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
  private static final Logger LOGGER = Logger.getLogger(StagingResourcesTableModel.class);

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
	 * The path for which history is shown.
	 */
	private String searchedPath = null;

  /**
	 * Compares file statuses.
	 */
	private final Comparator<FileStatus> fileStatusComparator = (f1, f2) -> {
	  int comparationResult = 0;
	  
	  if(searchedPath != null) {
	    boolean file1IsSearched = !(searchedPath.equals(f1.getFileLocation()) ||
	        f1.getFileLocation().startsWith(searchedPath + "/", 0));
	    boolean file2IsSearched = !(searchedPath.equals(f2.getFileLocation()) ||
	        f2.getFileLocation().startsWith(searchedPath + "/", 0));
	    comparationResult = Boolean.compare(file1IsSearched, file2IsSearched);
	  }
	  
	  if(comparationResult == 0) {
	    comparationResult = f1.getChangeType().compareTo(f2.getChangeType());
	    if(comparationResult == 0) {
	      comparationResult = f1.getFileLocation().compareTo(f2.getFileLocation());
	    }
	  }
	  
	  return comparationResult;
  };

	/**
	 * <code>true</code> if this model presents the resources from the index.
	 * <code>false</code> if it presents the modified resources that can be put in the index.
	 */
	private final boolean inIndex;

	/**
	 * Git controller.
	 */
  private final GitControllerBase gitController;

  /**
   * Constructor.
   * 
   * @param gitCtrl  Git controller.
   * @param inIndex  <code>true</code> if this is the model of the staged files.
   */
	public StagingResourcesTableModel(GitControllerBase gitCtrl, boolean inIndex) {
		this.gitController = gitCtrl;
    this.inIndex = inIndex;
	}

	@Override
	public int getColumnCount() {
	  return 2;
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
	      clazz = FileStatus.class;
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
  public Object getValueAt(int rowIndex, int columnIndex) {
		Object temp = null;
		switch (columnIndex) {
		  case FILE_STATUS_COLUMN:
		    temp = filesStatuses.get(rowIndex).getChangeType();
		    break;
		  case FILE_LOCATION_COLUMN:
		    temp = filesStatuses.get(rowIndex);
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
		this.filesStatuses.sort(fileStatusComparator);
		
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
	 * Change the files stage state from unstaged to staged or from staged to unstaged.
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

		if (inIndex) {
		  gitController.asyncReset(filesToBeUpdated);
		} else {
		  gitController.asyncAddToIndex(filesToBeUpdated);
		}
	}

	/**
	 * Some resources changed.
	 * 
	 * @param changeEvent Change information.
	 */
	void stateChanged(GitEventInfo changeEvent) {
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Change event in the " + (inIndex ? "'unstaged'" : "'staged'") + " area: " + changeEvent);
	    
	  }

	  updateTableModel(changeEvent);
	  removeDuplicates();
	  filesStatuses.sort(fileStatusComparator);
	  fireTableDataChanged();
	}

	/**
	 * Update the table model based on the given event.
	 * 
	 * @param changeEvent Event.
	 */
  private void updateTableModel(GitEventInfo changeEvent) {
    switch (changeEvent.getGitOperation()) {
      case STAGE:
        if (inIndex) {
          insertRows(GitAccess.getInstance().getStagedFile(((FileGitEventInfo) changeEvent).getAffectedFilePaths()));
        } else {
          deleteRows(((FileGitEventInfo) changeEvent).getAffectedFileStatuses());
        }
        break;
      case UNSTAGE:
        if (inIndex) {
          deleteRows(((FileGitEventInfo) changeEvent).getAffectedFileStatuses());
        } else {
          // Things were taken out of the INDEX. 
          // The same resource might be present in the UnStaged and INDEX. Remove old states.
          deleteRows(((FileGitEventInfo) changeEvent).getAffectedFileStatuses());
          insertRows(GitAccess.getInstance().getUnstagedFiles(((FileGitEventInfo) changeEvent).getAffectedFilePaths()));
        }
        break;
      case COMMIT:
        if (inIndex) {
          // Committed files are removed from the INDEX.
          filesStatuses.clear();
        }
        break;
      case DISCARD:
        deleteRows(((FileGitEventInfo) changeEvent).getAffectedFileStatuses());
        break;
      case MERGE_RESTART:
        filesStatuses.clear();
        List<FileStatus> fileStatuses = inIndex ? GitAccess.getInstance().getStagedFiles()
            : GitAccess.getInstance().getUnstagedFiles();
        insertRows(fileStatuses);
        break;
      case ABORT_REBASE:
      case CONTINUE_REBASE:
        filesStatuses.clear();
        break;
      case ABORT_MERGE:
        deleteRows(((FileGitEventInfo) changeEvent).getAffectedFileStatuses());
        break;
      default:
        break;
    }
  }

	/**
	 * Removes any duplicate entries
	 */
	private void removeDuplicates() {
		Set<FileStatus> set = new HashSet<>(this.filesStatuses);
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

	/**
	 * Sets the priority path. The files with this paths will be displayed first.
	 * If no value is set, this sort criterion will not be considered.
	 * 
	 * @param searchedPath  The searched path.
	 */
	public void setSearchedPath(String searchedPath) {
    this.searchedPath = searchedPath;
  }
}
