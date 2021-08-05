package com.oxygenxml.git.view.history.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.branches.CreateBranchDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class RevertToLastCommitAction extends AbstractAction {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(CreateBranchFromCommitAction.class.getName());

  /**
   * The ID of the commit that needs to be reverted.
   */
  private String commitId;

  /**
   * Constructor.
   * 
   * @param commitId
   */
  public RevertToLastCommitAction(String commitId) {
    super(Translator.getInstance().getTranslation(Tags.REVERT_COMMIT));
    this.commitId = commitId;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitOperationScheduler.getInstance().schedule(() -> {
      //aici trebuie sa pun revert commit action-ul
      try {
        CreateBranchDialog dialog = new CreateBranchDialog(Translator.getInstance().getTranslation(Tags.CREATE_BRANCH),
            null, false);
        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
          GitAccess.getInstance().checkoutCommitAndCreateBranch(dialog.getBranchName(), commitId);
        }
      } catch (CheckoutConflictException ex) {
        BranchesUtil.showCannotCheckoutNewBranchMessage();
      } catch (HeadlessException | GitAPIException ex) {
        LOGGER.debug(ex);
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
  }

}