package com.oxygenxml.git.view.stash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;


/**
 * The model for Affected files bt stash table.
 *
 * @author Alex_Smarandche
 */
public class FilesTableModel extends AbstractTableModel {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(FilesTableModel.class);

  /**
   * Index of the file status column.
   */
  public static final int FILE_STATUS_COLUMN = 0;

  /**
   * Index of the file location column.
   */
  public static final int FILE_LOCATION_COLUMN = 1;

  /**
   * The internal representation of the model
   */
  private final List<FileStatus> filesStatuses = new ArrayList<>();


  /**
   * Compares file statuses.
   */
  private final Comparator<FileStatus> fileStatusComparator = (f1, f2) -> {
    int changeTypeCompareResult = f1.getChangeType().compareTo(f2.getChangeType());
    if(changeTypeCompareResult == 0) {
      return f1.getFileLocation().compareTo(f2.getFileLocation());
    } else {
      return changeTypeCompareResult;
    }
  };


  @Override
  public int getRowCount() {
    return filesStatuses.size();
  }


  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Class<?> clazz = null;
    switch (columnIndex) {
      case FILE_STATUS_COLUMN:
        clazz = DiffEntry.ChangeType.class;
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
    filesStatuses = new ArrayList<>(filesStatuses);
    filesStatuses.sort(fileStatusComparator);
    fireTableRowsInserted(0, getRowCount());
  }


  /**
   * Returns the file from the given row.
   *
   * @param rowIndex The row index.
   * 
   * @return the file
   */
  public FileStatus getFileAt(int rowIndex) {
    return filesStatuses.get(rowIndex);
  }


  /**
   * @return The files in the model.
   */
  public List<FileStatus> getFilesStatuses() {
    return filesStatuses;
  }


  /**
   * Removes all rows.
   */
  public void clear() {
    int size = filesStatuses.size();
    filesStatuses.clear();
    fireTableRowsDeleted(0, size);
  }


  /**
   * Get the file path.
   *
   * @param rowIndex  Row index.
   *
   * @return The location of the file.
   */
  public String getFileLocation(int rowIndex) {
    return filesStatuses.get(rowIndex).getFileLocation();
  }


  /**
   * Get the file status at the specified index.
   *
   * @param rowIndex Row index.
   *
   * @return The file status.
   */
  public FileStatus getFileStatus(int rowIndex) {
    return filesStatuses.get(rowIndex);
  }


  /**
   * Updates the table based on the currently selected stash.
   *
   * @param stashIndex The index of the stashed changes.
   */
  public void updateTable(int stashIndex) {
    if(stashIndex >= 0) {
      List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
      int size = filesStatuses.size();
      clear();
      try {
        filesStatuses.addAll(RevCommitUtil.getChangedFiles(stashesList.get(stashIndex).getName()));
        fireTableRowsUpdated(0, Math.max(size, filesStatuses.size()) - 1);
      } catch (IOException | GitAPIException exc) {
        LOGGER.error(exc, exc);
      }
    }
  }


}
