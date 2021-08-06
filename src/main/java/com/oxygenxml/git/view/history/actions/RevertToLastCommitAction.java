package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.revertcommit.RevertCommitDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class RevertToLastCommitAction extends AbstractAction {
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
   * @param commitId
   */
  public RevertToLastCommitAction(CommitCharacteristics commitCharacteristics) {
    super(Translator.getInstance().getTranslation(Tags.REVERT_COMMIT));
    this.commitCharacteristics = commitCharacteristics;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        RevertCommitDialog dialog = new RevertCommitDialog(Translator.getInstance().getTranslation(Tags.REVERT_COMMIT));
        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
          
         GitAccess.getInstance().revertCommit(GitAccess.getInstance().getRepository(),commitCharacteristics.getCommitId());
        }
      }  catch (Exception ex) {
        LOGGER.debug(ex);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
  }

}