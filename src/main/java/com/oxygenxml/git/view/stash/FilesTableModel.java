package com.oxygenxml.git.view.stash;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
   * Updates the table row files.
   *
   * @param rowToUpdate   The row where commits are.
   */
  public void updateTable(int rowToUpdate) {
    if(rowToUpdate >= 0) {
      List<RevCommit> stashesList = new ArrayList<>(GitAccess.getInstance().listStashes());
      try {
        List<FileStatus> listOfChangedFiles =
                RevCommitUtil.getStashChangedFiles(stashesList.get(rowToUpdate).getName());
        while (this.getRowCount() != 0) {
          this.removeRow(this.getRowCount() - 1);
        }
        for (FileStatus file : listOfChangedFiles) {
          Object[] row = {file.getChangeType(), file};
          this.addRow(row);
        }
      } catch (IOException | GitAPIException exc) {
        LOGGER.debug(exc, exc);
      }
    }

  }

}
