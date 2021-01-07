package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.ResetToCommitDialog;
import com.oxygenxml.git.watcher.RepositoryChangeWatcher;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Reset branch to commit.
 */
public class ResetBranchToCommitAction extends AbstractAction {
  /**
   * i18n
   */
  private static Translator translator = Translator.getInstance();
  /**
   * Commit characteristics.
   */
  private CommitCharacteristics commitCharacteristics; // NOSONAR

  /**
   * Constructor.
   * 
   * @param commitCharacteristics Characteristics of the commit to which to reset the branch.
   */
  public ResetBranchToCommitAction(CommitCharacteristics commitCharacteristics) {
    super(
        MessageFormat.format(
            translator.getTranslation(Tags.RESET_BRANCH_TO_THIS_COMMIT),
            GitAccess.getInstance().getBranchInfo().getBranchName()) + "...");
    this.commitCharacteristics = commitCharacteristics;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    ResetToCommitDialog dialog = new ResetToCommitDialog(
        GitAccess.getInstance().getBranchInfo().getBranchName(),
        commitCharacteristics);
    dialog.setVisible(true);
    if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
      GitAccess.getInstance().resetToCommit(dialog.getResetType(), commitCharacteristics.getCommitId());
      RepositoryChangeWatcher.markAsNotified();
    }
  }

}
