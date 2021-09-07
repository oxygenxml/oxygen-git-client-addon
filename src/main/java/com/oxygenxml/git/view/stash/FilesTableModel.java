package com.oxygenxml.git.view.stash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
  private List<FileStatus> filesStatuses = new ArrayList<>();


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
    return filesStatuses != null ? filesStatuses.size() : 0;
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

    this.filesStatuses = new ArrayList<>(filesStatuses);

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
  public FileStatus getFileAt(int convertedRow) {
    return filesStatuses.get(convertedRow);
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
    fireTableRowsUpdated(0, size);
  }


  /**
   * Get the file path.
   *
   * @param convertedRow  file index.
   *
   * @return The location of the file.
   */
  public String getFileLocation(int convertedRow) {
    return filesStatuses.get(convertedRow).getFileLocation();
  }


  /**
   * Get thd file status at the specified index.
   *
   * @param convertedRow file index.
   *
   * @return The file status.
   */
  public FileStatus getFileStatus(int convertedRow) {
    return filesStatuses.get(convertedRow);
  }


  /**
   * Updates the table based on the currently selected stash.
   *
   * @param stashIndex The index of the stashed changes.
   */
  public void updateTable(int stashIndex) {
    if(stashIndex >= 0) {
      List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
      try {
        clear();
        filesStatuses.addAll(RevCommitUtil.getChangedFiles(stashesList.get(stashIndex).getName()));
        fireTableRowsUpdated(0, filesStatuses.size());
      } catch (IOException | GitAPIException exc) {
        LOGGER.error(exc, exc);
      }
    }
  }


}
