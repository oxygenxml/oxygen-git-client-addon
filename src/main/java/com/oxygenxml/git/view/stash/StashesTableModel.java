package com.oxygenxml.git.view.stash;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Model for stashes table.
 *
 * @author Alex_Smarandache
 */
public class StashesTableModel extends AbstractTableModel {

  /**
   * Constant for the column index representing the stash index.
   */
  public static final int STASH_INDEX_COLUMN = 0;

  /**
   * Constant for the column index representing the stash description.
   */
  public static final int STASH_DESCRIPTION_COLUMN = 1;
  
  /**
   * Constant for the column index representing the stash date.
   */
  public static final int STASH_DATE_COLUMN = 2;
  
  /**
   * Constant for the columns number.
   */
  public static final int NO_COLUMNS = 3;

  /**
   * The internal representation of the model.
   */
  private final List<RevCommit> stashes = new ArrayList<>();

  /**
   * The columns names.
   */
  private static final String[] COLUMNS_NAMES = new String[]{
    Translator.getInstance().getTranslation(Tags.ID),
    Translator.getInstance().getTranslation(Tags.DESCRIPTION),
    Translator.getInstance().getTranslation(Tags.CREATION_DATE)
  };


  /**
   * The public constructor.
   *
   * @param stashes List of stashes.
   */
  public StashesTableModel(List<RevCommit> stashes) {
    this.stashes.addAll(stashes);
  }


  @Override
  public int getRowCount() {
    return stashes.size();
  }


  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Class<?> clazz = null;
    switch (columnIndex) {
      case STASH_INDEX_COLUMN:
        clazz = Integer.class;
        break;
      case STASH_DESCRIPTION_COLUMN:
        clazz = String.class;
        break;
      case STASH_DATE_COLUMN:
          clazz = Date.class;
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
    return NO_COLUMNS;
  }


  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Object temp = null;
    switch (columnIndex) {
      case STASH_INDEX_COLUMN:
        temp = rowIndex;
        break;
      case STASH_DESCRIPTION_COLUMN:
        temp = stashes.get(rowIndex).getFullMessage();
        break;
      case STASH_DATE_COLUMN:
          temp = stashes.get(rowIndex).getAuthorIdent().getWhen();
          break;
      default:
        break;
    }

    return temp;
  }


  @Override
  public String getColumnName(int col) {
    return COLUMNS_NAMES[col];
  }


  /**
   * Removes all rows.
   */
  public void clear() {
    int size = stashes.size();
    stashes.clear();
    this.fireTableRowsDeleted(0, size - 1);
  }


  /**
   * Removes the row from given index.
   *
   * @param index Index of row to be deleted.
   */
  public void removeRow(int index) {
    stashes.remove(index);
    fireTableRowsDeleted(index, index);
  }


}
