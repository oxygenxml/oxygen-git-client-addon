package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateBranchFromCommitAction.class.getName());

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
    Translator translator = Translator.getInstance();
    int result = FileStatusDialog.showQuestionMessage(
        translator.getTranslation(Tags.REVERT_COMMIT),
        translator.getTranslation(Tags.REVERT_COMMIT_CONFIRMATION), 
        translator.getTranslation(Tags.YES),
        translator.getTranslation(Tags.NO));
    if ( result == OKCancelDialog.RESULT_OK) {
      GitOperationScheduler.getInstance().schedule(() -> {
        try {
          GitAccess.getInstance().revertCommit(commitCharacteristics.getCommitId());
        } catch (IOException | NoRepositorySelected | GitAPIException ex) {
          LOGGER.debug(ex.getMessage(), ex);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
        }
      });
    }
  }

}