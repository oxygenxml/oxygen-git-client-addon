package com.oxygenxml.git;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.PrivateRepositoryException;
import com.oxygenxml.git.service.exceptions.RepositoryUnavailableException;
import com.oxygenxml.git.service.exceptions.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Check for local branches that are linked to remote branches
 * which no longer exist. 
 */
public class OutdatedBranchChecker {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OutdatedBranchChecker.class.getName());
  
  /**
    * Constructor.
    *
    * @throws UnsupportedOperationException when invoked.
    */
  private OutdatedBranchChecker() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }
  
  /**
   * i18n
   */
  private static final Translator i18n = Translator.getInstance();
  
  /**
   * Delay after the repository is loaded.
   */
  private static final int TIMER_DELAY_AFTER_REPO_IS_LOADED = 5000;
  
  /**
   * Interval for the periodic check (3.5 hours in milliseconds).
   */
  private static final int PERIODIC_CHECK_INTERVAL = (int) (3.5 * 60 * 60 * 1000L);
  
  /**
   * Timer.
   */
  private static Timer outdatedBranchCheckerTimer;
  
  /**
   * Init checker.
   * 
   * @param gitController Git controller. 
   */
  public static void init(GitController gitController) {
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          if (outdatedBranchCheckerTimer != null) {
            outdatedBranchCheckerTimer.cancel();
          }
          
          GitOperationScheduler gitOperationScheduler = GitOperationScheduler.getInstance();
          
          outdatedBranchCheckerTimer = new Timer("Outdated Branch Checker Timer");
          
          outdatedBranchCheckerTimer.schedule(
              new TimerTask() {
                @Override
                public void run() {
                  gitOperationScheduler.schedule(() -> checkForOutdatedBranches());
                }
              },
              TIMER_DELAY_AFTER_REPO_IS_LOADED);
          
          outdatedBranchCheckerTimer.scheduleAtFixedRate(
              new TimerTask() {
                @Override
                public void run() {
                  gitOperationScheduler.schedule(() -> checkForOutdatedBranches());
                }
              },
              PERIODIC_CHECK_INTERVAL,
              PERIODIC_CHECK_INTERVAL);
        }
      }
    });
  }
  
  /**
   * Check for outdated branches.
   */
  private static synchronized void checkForOutdatedBranches() {
    try {
      List<Ref> obsoleteBranches = BranchesUtil.getLocalBranchesThatNoLongerHaveRemotes();
      if (!obsoleteBranches.isEmpty()) {
        Map <String, String> branchesAndTooltips = new HashMap<>();
        for (Ref ref : obsoleteBranches) {
          branchesAndTooltips.put(
              Repository.shortenRefName(ref.getName()),
              // No tooltip
              null);
        }
        
        String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
        
        SwingUtilities.invokeLater(() -> {
          if (branchesAndTooltips.containsKey(currentBranchName) && branchesAndTooltips.size() == 1) {
            showCurrentBranchRelatedMessage(
                MessageFormat.format(i18n.getTranslation(Tags.OUTDATED_CURRENT_BRANCH), currentBranchName)
                + "\n\n"
                + MessageFormat.format(i18n.getTranslation(Tags.STASH_IMPORTANT_CHANGES), currentBranchName));
          } else {
            showOutdatedBranchesDialog(branchesAndTooltips, currentBranchName);
          }
        });
        
      }
    } catch (NoRepositorySelected e) {
      LOGGER.warn(e, e);
    } catch (SSHPassphraseRequiredException | PrivateRepositoryException | RepositoryUnavailableException e) {
      LOGGER.error(e, e);
    } 
  }

  /**
   * Show the dialog that presents the outdated branches.
   * 
   * @param branchesAndTooltips        The branches to present in the dialog and their tooltips.
   * @param currentBranchName          The name of the current branch.
   */
  private static void showOutdatedBranchesDialog(
      Map<String, String> branchesAndTooltips,
      String currentBranchName) {
    int userChoice = MessagePresenterProvider.getBuilder(i18n.getTranslation(Tags.OUTDATED_BRANCHES_DETECTED), DialogType.WARNING)
        .setMessage(i18n.getTranslation(Tags.OUTDATED_BRANCHES_INFO))
        .setTargetResourcesWithTooltips(branchesAndTooltips)
        .setOkButtonName(i18n.getTranslation(Tags.DELETE_BRANCHES))
        .setCancelButtonName(i18n.getTranslation(Tags.CLOSE))
        .buildAndShow()
        .getResult();
    if (userChoice == OKCancelDialog.RESULT_OK) {
      Set<String> branchesToDelete = branchesAndTooltips.keySet();
      int userDecision = PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
          i18n.getTranslation(Tags.DELETE_BRANCHES),
          MessageFormat.format(
              i18n.getTranslation(Tags.OUTDATED_BRANCHES_DELETION_CONFIRMATION),
              branchesToDelete.stream()
              .sorted(String.CASE_INSENSITIVE_ORDER)
              .map(item -> "- " + item)
              .collect(Collectors.joining("\n"))),
          new String[] { i18n.getTranslation(Tags.YES), i18n.getTranslation(Tags.NO) },
          new int[] { 1, 0 });
      if (userDecision == 1) {
        tryDeleteBranches(branchesToDelete, currentBranchName);
      }
    }
  }

  /**
   * Try to delete the given branches. The current branch will not be deleted.
   * 
   * @param branchesToDelete  The branches to delete.
   * @param currentBranchName The name of the current branch.
   */
  private static void tryDeleteBranches(
      Set<String> branchesToDelete,
      String currentBranchName) {
    boolean shouldDeleteCurrentBranch = branchesToDelete.contains(currentBranchName);
    if (shouldDeleteCurrentBranch) {
      branchesToDelete.remove(currentBranchName);
    }

    Runnable deleteBranchesTask = () -> {
      ProgressDialog progressDialog = new ProgressDialog(Translator.getInstance().getTranslation(Tags.DELETE_BRANCHES), true);
      GitAccess.getInstance().deleteBranches(
          branchesToDelete,
          Optional.of(new GitOperationProgressMonitor(progressDialog)));
    };
    GitOperationScheduler.getInstance().schedule(deleteBranchesTask);

    if (shouldDeleteCurrentBranch) {
      showCurrentBranchRelatedMessage(
          MessageFormat.format(i18n.getTranslation(Tags.CANNOT_DELETE_CURRENT_BRANCH), currentBranchName)
          + "\n\n"
          + MessageFormat.format(i18n.getTranslation(Tags.STASH_IMPORTANT_CHANGES), currentBranchName));
    }
  }

  /**
   * Show a message related to the fact than the current branch cannot be deleted.
   * 
   * @param message The message to show.
   */
  private static void showCurrentBranchRelatedMessage(String message) {
    MessagePresenterProvider.getBuilder(i18n.getTranslation(Tags.OUTDATED_BRANCHES_DETECTED), DialogType.INFO)
      .setMessage(message)
      .setOkButtonVisible(false)
      .setCancelButtonName(i18n.getTranslation(Tags.CLOSE))
      .buildAndShow();
  }

}
