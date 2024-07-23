package com.oxygenxml.git.view.history.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.branches.BranchCheckoutMediator;
import com.oxygenxml.git.view.branches.BranchesUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Create a new branch starting from a commit in the history table.
 */
public class CreateBranchFromCommitAction extends AbstractAction {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateBranchFromCommitAction.class.getName());
  
  /**
   * The ID of the commit used as a starting point for the new branch.
   */
  private String commitId;
  
  /**
   * Constructor.
   * @param commitId 
   */
  public CreateBranchFromCommitAction(String commitId) {
    super(Translator.getInstance().getTranslation(Tags.CREATE_BRANCH) + "...");
    this.commitId = commitId;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    BranchCheckoutMediator.getInstance().createBranch(
        Translator.getInstance().getTranslation(Tags.CREATE_BRANCH),
        null,
        false,
        (branchName, shouldCheckoutNewBranch) -> {
          GitOperationScheduler.getInstance().schedule(() -> {
            try {
              if(shouldCheckoutNewBranch) {
                GitAccess.getInstance().checkoutCommitAndCreateBranch(branchName, commitId);
              } else {
                GitAccess.getInstance().createBranch(branchName, commitId);
              }
              
            } catch (CheckoutConflictException ex) {
              BranchesUtil.showCannotCheckoutNewBranchMessage();
            } catch (HeadlessException | GitAPIException ex) {
              LOGGER.debug(ex.getMessage(), ex);
              PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
            }
          });
        });
  }
  
}
