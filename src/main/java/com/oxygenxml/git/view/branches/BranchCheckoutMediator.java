package com.oxygenxml.git.view.branches;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.dialog.AskForBranchUpdateDialog;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.ActionStatus;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullEvent;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Mediates the communication about the external environment and the branch dialog creation.
 * 
 * @author alex_smarandache
 */
public class BranchCheckoutMediator {
  
  /**
   * The Git controller to manage git operations.
   */
  private GitController ctrl;
  
  /**
   * The pull operation listener.
   */
  private GitEventListener pullListener;
  
  /**
   * <code>true</code> when the pull listener can ask user about the branch creation. 
   * Used to avoid call create branch dialog on every successfully pull. 
   */
  private final AtomicBoolean shouldShowPullDialog = new AtomicBoolean();
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BranchesTooltipsCache.class);
  
  /**
   * The name proposed for the branch to be created.
   */
  private String currentProposedBranchName = null;
  
  /**
   * The progress dialog of the pull operation.
   */
  private final ProgressDialog pullOperationProgressDialog = new ProgressDialog(
      Translator.getInstance().getTranslation(Tags.PULL), true);
  
  
  /**
   * Initialize the Git support.
   * 
   * @param ctrl The Git controller to manage git operations.
   */
  public BranchCheckoutMediator(@NonNull GitController ctrl) {
    this.ctrl = ctrl;
  }
  
  
  /**
   * Show the create branch dialog.
   * 
   * @param createBranchDialogTitle      The title of the dialog.
   * @param branchProposedName           The proposed name of the branch.
   * @param isCheckoutRemote             <code>true</code> if the checkout branch is a remote branch.
   * @param branchCreator                The branch creator after the user confirmation.
   * @param warnIfRepositoryIsOutdated   <code>true</code> if the user should be warned if the repository is outdated.
   */
  public void createBranch(
      String createBranchDialogTitle, 
      String branchProposedName, 
      boolean isCheckoutRemote, 
      IBranchesCreator branchCreator,
      boolean warnIfRepositoryIsOutdated) {
    try {
      currentProposedBranchName = branchProposedName;
      ctrl.getGitAccess().fetch();
      boolean isRepoUpToDate = 0 == ctrl.getGitAccess().getPullsBehind();
      if(isRepoUpToDate || !warnIfRepositoryIsOutdated) {
        showCreateBranchDialog(createBranchDialogTitle, isCheckoutRemote, branchCreator);
      } else {
        AskForBranchUpdateDialog askForBranchDialog = new AskForBranchUpdateDialog();
        SwingUtilities.invokeLater(() -> {
          askForBranchDialog.setVisible(true);
          if(askForBranchDialog.getResult() == OKOtherAndCancelDialog.RESULT_OK) {
            tryPull(createBranchDialogTitle, isCheckoutRemote, branchCreator);
          } else if(askForBranchDialog.getResult() == OKOtherAndCancelDialog.RESULT_OTHER) {
            showCreateBranchDialog(createBranchDialogTitle, isCheckoutRemote, branchCreator);
          } 
        });
      }
    } catch(Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    } 
  }

  
  
  /**
   * Show the create branch dialog.
   * 
   * @param dialogTitle           The title of the dialog.
   * @param isCheckoutRemote      <code>true</code> if the checkout branch is a remote branch.
   * @param branchCreator         The branch creator after the user confirmation.
   */
  private void showCreateBranchDialog(
      String dialogTitle,  
      boolean isCheckoutRemote, 
      IBranchesCreator branchCreator) {
    CreateBranchDialog branchDialog = new CreateBranchDialog(dialogTitle, currentProposedBranchName, isCheckoutRemote);
    if(branchDialog.getResult() == OKCancelDialog.RESULT_OK) {
      branchCreator.createBranch(branchDialog.getBranchName(), branchDialog.shouldCheckoutNewBranch());
    }
  }

  /**
   * Try to complete the pull operation before to create a new branch. 
   * <br>
   * If the pull success, the user will be asked to create a new branch.
   * <br>
   * If the pull fails, the process will be aborted.
   * 
   * @param dialogTitle          The title of the create branch dialog.
   * @param isCheckoutRemote     <code>true</code> if the checkout branch is a remote branch.
   * @param branchCreator        The branch creator after the user confirmation.
   */
  private void tryPull(String dialogTitle, 
      boolean isCheckoutRemote, 
      IBranchesCreator branchCreator) {
    
    shouldShowPullDialog.set(true);
    
    if(pullListener == null) {
      pullListener = createGitPullListener(dialogTitle, isCheckoutRemote, branchCreator);
      ctrl.addGitListener(pullListener);
    }

    SwingUtilities.invokeLater(() -> {
      pullOperationProgressDialog.initUI();
      SwingUtilities.invokeLater(() -> pullOperationProgressDialog.setVisible(true));
    });
        
    PullType pullType = OptionsManager.getInstance().getDefaultPullType();
    ctrl.pull(
        pullType == PullType.UKNOWN ? PullType.MERGE_FF : pullType, 
        new GitOperationProgressMonitor(pullOperationProgressDialog));
  }

  /**
   * Create the GIT pull listener to receive the pull operation.
   * 
   * @param dialogTitle          The title of the create branch dialog.
   * @param isCheckoutRemote     <code>true</code> if the checkout branch is a remote branch.
   * @param branchCreator        The branch creator after the user confirmation.
   * 
   * @return The created listener.
   */
  private GitEventListener createGitPullListener(String dialogTitle, boolean isCheckoutRemote, IBranchesCreator branchCreator) {
    return new GitEventAdapter() {
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (info.getGitOperation() == GitOperation.PULL && shouldShowPullDialog.getAndSet(false)) {
          SwingUtilities.invokeLater(pullOperationProgressDialog::dispose);
        }
      }

      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if(info.getGitOperation() == GitOperation.PULL && shouldShowPullDialog.getAndSet(false)) {
          if(info instanceof PushPullEvent) {
            PushPullEvent pullInfo = (PushPullEvent) info;
            boolean isConflictedPull = pullInfo.getActionStatus() == ActionStatus.PULL_REBASE_CONFLICT_GENERATED 
                || pullInfo.getActionStatus() == ActionStatus.PULL_MERGE_CONFLICT_GENERATED;
            if(isConflictedPull) {
              SwingUtilities.invokeLater(pullOperationProgressDialog::dispose);
              return;
            } 
          }
          
          SwingUtilities.invokeLater(() -> {
            pullOperationProgressDialog.dispose();
            showCreateBranchDialog(dialogTitle, isCheckoutRemote, branchCreator);
          });
        }
      }
    };
  }
 
}
