package com.oxygenxml.git;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
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
  private static final Timer OUTDATED_BRANCH_CHECKER_TIMER = new Timer("Outdated Branch Checker Timer");
  
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
          OUTDATED_BRANCH_CHECKER_TIMER.schedule(
              new TimerTask() {
                @Override
                public void run() {
                  checkForOutdatedBranches();
                }
              },
              TIMER_DELAY_AFTER_REPO_IS_LOADED);
          
          OUTDATED_BRANCH_CHECKER_TIMER.scheduleAtFixedRate(
              new TimerTask() {
                @Override
                public void run() {
                  checkForOutdatedBranches();
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
      GitAccess gitAccess = GitAccess.getInstance();
      List<Ref> obsoleteBranches = gitAccess.getLocalBranchesThatNoLongerHaveRemotes();
      if (!obsoleteBranches.isEmpty()) {
        Map <String, String> branches = new HashMap<>();
        for (Ref ref : obsoleteBranches) {
          branches.put(
              Repository.shortenRefName(ref.getName()),
              // No tooltip
              null);
        }
        
        int userChoice = MessagePresenterProvider.getBuilder(i18n.getTranslation(Tags.OUTDATED_BRANCHES_DETECTED), DialogType.WARNING)
          .setMessage(i18n.getTranslation(Tags.OUTDATED_BRANCHES_INFO))
          .setTargetResourcesWithTooltips(branches)
          .setOkButtonName(i18n.getTranslation(Tags.DELETE_BRANCHES))
          .setCancelButtonName(i18n.getTranslation(Tags.CLOSE))
          .buildAndShow()
          .getResult();
        if (userChoice == OKCancelDialog.RESULT_OK) {
          int userDecision = PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
              i18n.getTranslation(Tags.DELETE_BRANCHES),
              MessageFormat.format(
                  i18n.getTranslation(Tags.OUTDATED_BRANCHES_DELETION_CONFIRMATION),
                  branches.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(item -> "- " + item)
                    .collect(Collectors.joining("\n"))),
              new String[] {i18n.getTranslation(Tags.YES), i18n.getTranslation(Tags.NO)},
              new int[] {1, 0});
          if (userDecision == 1) {
            gitAccess.deleteBranches(branches.keySet());
          }
        }
      }
    } catch (NoRepositorySelected e) {
      LOGGER.warn(e, e);
    }
  }

}