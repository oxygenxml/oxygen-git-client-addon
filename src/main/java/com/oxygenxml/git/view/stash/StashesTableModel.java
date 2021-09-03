package com.oxygenxml.git.view.stash;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
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
   * List of all stahses from the current repository.
   */
  List<RevCommit> stashes;


  /**
   * The public constructor.
   */
  public StashesTableModel(List<RevCommit> stashes) {

    super(new String[]{Translator.getInstance().getTranslation(Tags.ID),
            Translator.getInstance().getTranslation(Tags.DESCRIPTION)}, 0);

    for (int i = 0; i < stashes.size(); i++) {
      Object[] row = {i, stashes.get(i).getFullMessage()};
      this.addRow(row);
    }

    this.stashes = stashes;

  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

  @Override
  public void removeRow(int index) {
    GitAccess.getInstance().dropStash(index);

    for (int row = index + 1; row <  this.getRowCount(); row++) {
      this.setValueAt((int)getValueAt(row, 0) - 1, row, 0);
    }

    this.fireTableRowsUpdated(index + 1, this.getRowCount());
    super.removeRow(index);
  }


  public void clear() {
    while (this.getRowCount() != 0 ) {
      this.removeRow(this.getRowCount() - 1);
    }
  }
}
