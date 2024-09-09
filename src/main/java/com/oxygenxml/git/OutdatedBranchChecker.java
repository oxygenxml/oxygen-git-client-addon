package com.oxygenxml.git;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;

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
   * Timer.
   */
  private static final Timer OUTDATED_BRANCH_CHECKER_TIMER = new Timer("Outdated Branch Checker Timer");
  
  /**
   * Task that checks for outdated local branches.
   */
  private static TimerTask showObsoleteBranchesTask = new TimerTask() {
    @Override
    public void run() {
      try {
        List<Ref> obsoleteBranches = GitAccess.getInstance().getLocalBranchesThatNoLongerHaveRemotes();
        if (!obsoleteBranches.isEmpty()) {
          Map <String, String> branches = new HashMap<>();
          for (Ref ref : obsoleteBranches) {
            branches.put(Repository.shortenRefName(ref.getName()), null);
          }
          
          MessagePresenterProvider.getBuilder(i18n.getTranslation(Tags.OUTDATED_BRANCHES_DETECTED), DialogType.WARNING)
            .setMessage(i18n.getTranslation(Tags.OUTDATED_BRANCHES_INFO))
            .setCancelButtonVisible(false)
            .setTargetResourcesWithTooltips(branches)
            .setOkButtonName(Tags.CLOSE)
            .buildAndShow();
        }
      } catch (NoRepositorySelected e) {
        LOGGER.warn(e, e);
      }
    }
  };
  
  /**
   * Init checker.
   */
  public static void initCheck() {
    OUTDATED_BRANCH_CHECKER_TIMER.schedule(showObsoleteBranchesTask, TIMER_DELAY_AFTER_REPO_IS_LOADED);
  }

}
