package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RemoteRepositoryChangeWatcher;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.history.CommitCharacteristics;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Action that reverts all the changes from a commit
 * and creates a new commit for the reverted state. 
 */
public class RevertCommitAction extends AbstractAction {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(CreateBranchFromCommitAction.class.getName());

  /**
   * Commit characteristics.
   */
  private CommitCharacteristics commitCharacteristics;

  /**
   * Constructor.
   * 
   * @param commitCharacteristics Commit characteristics.
   */
  public RevertCommitAction(CommitCharacteristics commitCharacteristics) {
    super(Translator.getInstance().getTranslation(Tags.REVERT_COMMIT));
    this.commitCharacteristics = commitCharacteristics;
  }

  /**
   * Action performed.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        int result = FileStatusDialog.showQuestionMessage(
            Translator.getInstance().getTranslation(Tags.REVERT_COMMIT),
            Translator.getInstance().getTranslation(Tags.REVERT_COMMIT_WARNING), 
            Translator.getInstance().getTranslation(Tags.YES),
            Translator.getInstance().getTranslation(Tags.NO));
        if ( result == OKCancelDialog.RESULT_OK) {
          GitAccess.getInstance().revertCommit(commitCharacteristics.getCommitId());
          RemoteRepositoryChangeWatcher.markAsNotified();
        }
      } catch (NoRepositorySelected | IOException ex) {
        LOGGER.debug(ex);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
  }

}