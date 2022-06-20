package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
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
    final int result = MessagePresenterProvider.getBuilder(
        translator.getTranslation(Tags.REVERT_COMMIT), DialogType.QUESTION)
        .setQuestionMessage(translator.getTranslation(Tags.REVERT_COMMIT_CONFIRMATION))
        .setOkButtonName(translator.getTranslation(Tags.YES))
        .setCancelButtonName(translator.getTranslation(Tags.NO))
        .buildAndShow().getResult();
    
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