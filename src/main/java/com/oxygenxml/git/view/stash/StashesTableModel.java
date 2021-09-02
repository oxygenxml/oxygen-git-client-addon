package com.oxygenxml.git.view.stash;

import org.eclipse.jgit.revwalk.RevCommit;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * Model for stahses table.
 *
 * @author Alex_Smarandache
 */
public class StashesTableModel extends DefaultTableModel {

  /**
   * The public constructor.
   *
   * @param columns   the columns
   * @param rowCount  number of rows
   */
  public StashesTableModel(Object[] columns, int rowCount, List<RevCommit> stashes) {
    super(columns, rowCount);

    for (int i = 0; i < stashes.size(); i++) {
      Object[] row = {i, stashes.get(i).getFullMessage()};
      this.addRow(row);
    }

  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

}
