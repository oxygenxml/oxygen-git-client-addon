package com.oxygenxml.git.view.stash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;


/**
 * The model for affected files.
 *
 * @author Alex_Smarandache
 */
public class FilesTableModel extends DefaultTableModel {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(ListStashesDialog.class.getName());


  /**
   * The public constructor.
   */
  public FilesTableModel() {
    super(new String[]{"icon", "file_path"}, 0);
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


  /**
   * Updates the table based on the currently selected stash.
   *
   * @param stashIndex The index of the stashed changes.
   */
  public void updateTable(int stashIndex) {
    if(stashIndex >= 0) {
      List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
      try {
        List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(stashesList.get(stashIndex).getName());
        clear();
        for (FileStatus file : changedFiles) {
          Object[] row = {file.getChangeType(), file};
          this.addRow(row);
        }
      } catch (IOException | GitAPIException exc) {
        LOGGER.error(exc, exc);
      }
    }
  }

}
