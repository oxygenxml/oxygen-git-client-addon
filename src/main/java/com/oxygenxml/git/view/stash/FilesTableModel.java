package com.oxygenxml.git.view.stash;

import javax.swing.table.DefaultTableModel;

/**
 * The model for affected files.
 *
 * @author Alex_Smarandache
 */
public class FilesTableModel extends DefaultTableModel {

  /**
   * The public constructor.
   *
   * @param columns   the columns
   * @param rowCount  number of rows
   */
  public FilesTableModel(Object[] columns, int rowCount) {
    super(columns, rowCount);

  }


  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }


  /**
   * Remove all rows.
   */
  public void clear() {
    while(this.getRowCount() > 0) {
      this.removeRow(this.getRowCount() - 1);
    }
  }

}
