package com.oxygenxml.git.view.stash;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Model for stashes table.
 *
 * @author Alex_Smarandache
 */
public class StashesTableModel extends AbstractTableModel {

  /**
   * Constant for the index representing the stash index.
   */
  public static final int STASH_INDEX_COLUMN = 0;

  /**
   * Constant for the index representing the stash description.
   */
  public static final int STASH_DESCRIPTION_COLUMN = 1;

  /**
   * The internal representation of the model
   */
  private final List<RevCommit> stashes = new ArrayList<>();

  /**
   * The columns names.
   */
  private static final String[] COLUMNS_NAMES = new String[]{
    Translator.getInstance().getTranslation(Tags.ID),
            Translator.getInstance().getTranslation(Tags.DESCRIPTION)
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
      case STASH_INDEX_COLUMN:
        temp = rowIndex;
        break;
      case STASH_DESCRIPTION_COLUMN:
        temp = stashes.get(rowIndex).getFullMessage();
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
    while(!stashes.isEmpty()) {
      int index = stashes.size() - 1;
      GitAccess.getInstance().dropStash(index);
      stashes.remove(index);
    }

    this.fireTableRowsUpdated(0, size);
  }


  /**
   * Removes the row from given index.
   *
   * @param index Index of row to be deleted.
   */
  public void removeRow(int index) {
    GitAccess.getInstance().dropStash(index);
    stashes.remove(index);
    
    fireTableRowsUpdated(index, stashes.size());
  }


  /**
   * Returns the file from the given row
   *
   * @param convertedRow
   *          - the row
   * @return the file
   */
  public RevCommit getStashAt(int convertedRow) {
    return stashes.get(convertedRow);
  }


  /**
   * @return The files in the model.
   */
  public List<RevCommit> getStashes() {
    return stashes;
  }


}
