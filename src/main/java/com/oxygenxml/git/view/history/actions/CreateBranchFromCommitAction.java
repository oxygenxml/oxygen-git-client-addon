package com.oxygenxml.git.view.history.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.branches.IBranchesCreator;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;

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
   * The Git controller.
   */
  private GitController gitController;
  
  /**
   * Constructor.
   * @param commitId 
   */
  public CreateBranchFromCommitAction(GitController gitController, String commitId) {
    super(Translator.getInstance().getTranslation(Tags.CREATE_BRANCH) + "...");
    this.commitId = commitId;
    this.gitController = gitController;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    IBranchesCreator branchesCreator = new IBranchesCreator() {
      @Override
      public void createBranch(String branchName, boolean shouldCheckoutBranch) {
        GitOperationScheduler.getInstance().schedule(() -> {
          try {
            final Optional<IGitViewProgressMonitor> progMon = Optional.of(
                new GitOperationProgressMonitor(new ProgressDialog(Translator.getInstance().getTranslation(Tags.CREATE_BRANCH), false)));
            if(shouldCheckoutBranch) {
              GitAccess.getInstance().checkoutCommitAndCreateBranch(branchName, commitId, progMon);
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
      }
    }; 
    
    gitController.getBranchesCheckoutMediator().createBranch(Translator.getInstance().getTranslation(Tags.CREATE_BRANCH), 
        null, 
        false, branchesCreator, 
        Objects.equals(gitController.getGitAccess().getHeadName(), commitId) // warn user only for local head in history if the repository is outdated
    );
  }
  
}
