package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JComboBox;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RepoNotInitializedException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.BranchSwitchConfirmationDialog;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.stash.StashUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * This is a combo box to select the current branch for repo.
 * 
 * @author Alex_Smarandache
 */
public class BranchSelectionCombo extends JComboBox<String> {


  /**
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(BranchSelectionCombo.class);

  /**
   * i18n
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * Access to the Git API.
   */
  private static final GitAccess GIT_ACCESS = GitAccess.getInstance();

  /**
   * <code>true</code> if the combo popup is showing.
   */
  private boolean isComboPopupShowing;

  /**
   * <code>true</code> to inhibit branch selection listener.
   */
  private boolean inhibitBranchSelectionListener;

  /**
   * The ID of the commit on which a detached HEAD is set.
   */
  private String detachedHeadId;



  /**
   * Constructor.
   * 
   * @param gitController Git Controller.
   * @param isLabeled     <code>true</code> if the panel has a label attached.
   */
  public BranchSelectionCombo(final GitController gitController) {

    this.addItemListener(event -> {
      if (!inhibitBranchSelectionListener && event.getStateChange() == ItemEvent.SELECTED) {
        treatBranchSelectedEvent(event);
      }
    });

    this.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        isComboPopupShowing = true;
      }
      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        isComboPopupShowing = false;
      }
      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        isComboPopupShowing = false;
      }
    });

    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.OPEN_WORKING_COPY
            || operation == GitOperation.ABORT_REBASE 
            || operation == GitOperation.CONTINUE_REBASE
            || operation == GitOperation.DELETE_BRANCH
            || operation == GitOperation.CREATE_BRANCH
            || operation == GitOperation.CHECKOUT_COMMIT
            || operation == GitOperation.CHECKOUT) {
          refresh();
        }
      }
    });


  }


  /**
   * Treat a branch name selection event.
   * 
   * @param event The event to treat.
   */
  private void treatBranchSelectedEvent(final ItemEvent event) {
    final String branchName = (String) event.getItem();
    final BranchInfo currentBranchInfo = GIT_ACCESS.getBranchInfo();
    final String currentBranchName = currentBranchInfo.getBranchName();
    if (branchName.equals(currentBranchName)) {
      return;
    }

    final RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
    if(RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
      SwingUtilities.invokeLater(() -> {
        BranchSwitchConfirmationDialog dialog = new BranchSwitchConfirmationDialog(branchName);

        dialog.setVisible(true);

        int answer = dialog.getResult();

        if(answer == OKOtherAndCancelDialog.RESULT_OTHER) {
          tryCheckingOutBranch(currentBranchInfo, branchName);
        } else if(answer == OKOtherAndCancelDialog.RESULT_OK) {
          boolean wasStashCreated = StashUtil.stashChanges();
          if(wasStashCreated) {
            tryCheckingOutBranch(currentBranchInfo, branchName);
          }
        } else {
          restoreCurrentBranchSelectionInMenu();
        }
      });
    } else {
      tryCheckingOutBranch(currentBranchInfo, branchName);
    }
  }


  /**
   * Refresh.
   */
  public void refresh() {
    updateBranchesPopup();
    updateTooltipsText();
  }


  /**
   * Updates branches tooltips text.
   */
  public void updateTooltipsText() {
    int pullsBehind = GIT_ACCESS.getPullsBehind();
    int pushesAhead = -1;
    try {
      pushesAhead = GIT_ACCESS.getPushesAhead();
    } catch (RepoNotInitializedException e) {
      LOGGER.debug(e.getMessage(), e);
    }


    Repository repo = null;
    try {
      repo = GIT_ACCESS.getRepository();
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
    }

    this.setEnabled(repo != null);

    final BranchInfo branchInfo = GIT_ACCESS.getBranchInfo();
    final String currentBranchName = branchInfo.getBranchName();
    if (branchInfo.isDetached()) {
      detachedHeadId = currentBranchName;

      String tooltipText = TRANSLATOR.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
          + " " + currentBranchName;
      if (repo != null && repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
        tooltipText += "<br>" + TRANSLATOR.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
      }
      tooltipText = TextFormatUtil.toHTML(tooltipText);
      String finalText = tooltipText;
      SwingUtilities.invokeLater(() -> this.setToolTipText(finalText));
    } else {
      detachedHeadId = null;
      String branchTooltip = null;
      if (currentBranchName != null && !currentBranchName.isEmpty()) {
        branchTooltip = getBranchTooltip(pullsBehind, pushesAhead, currentBranchName);
      }
      String branchTooltipFinal = branchTooltip;
      SwingUtilities.invokeLater(() ->this.setToolTipText(branchTooltipFinal));
    }
  }


  /**
   * Compute the branch tooltip text.
   * 
   * @param pullsBehind          Number of pulls behind.
   * @param pushesAhead          Number of pulls ahead.
   * @param currentBranchName    The current branch name.
   * 
   * @return the branch tool tip text.
   */
  private String getBranchTooltip(int pullsBehind, int pushesAhead, String currentBranchName) {
    String branchTooltip = null;

    String upstreamBranchFromConfig = GIT_ACCESS.getUpstreamBranchShortNameFromConfig(currentBranchName);
    boolean isAnUpstreamBranchDefinedInConfig = upstreamBranchFromConfig != null;

    String upstreamShortestName =
        isAnUpstreamBranchDefinedInConfig
        ? upstreamBranchFromConfig.substring(upstreamBranchFromConfig.lastIndexOf('/') + 1)
            : null;
        Ref remoteBranchRefForUpstreamFromConfig =
            isAnUpstreamBranchDefinedInConfig
            ? RepoUtil.getRemoteBranch(upstreamShortestName)
                : null;
            boolean existsRemoteBranchForUpstreamDefinedInConfig = remoteBranchRefForUpstreamFromConfig != null;

            branchTooltip = TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH)
                + " <b>" + currentBranchName + "</b>.<br>"
                + TRANSLATOR.getTranslation(Tags.UPSTREAM_BRANCH)
                + " <b>"
                + (isAnUpstreamBranchDefinedInConfig && existsRemoteBranchForUpstreamDefinedInConfig
                    ? upstreamBranchFromConfig
                        : TRANSLATOR.getTranslation(Tags.NO_UPSTREAM_BRANCH))
                + "</b>.<br>";

            String commitsBehindMessage = "";
            String commitsAheadMessage = "";
            if (isAnUpstreamBranchDefinedInConfig && existsRemoteBranchForUpstreamDefinedInConfig) {
              if (pullsBehind == 0) {
                commitsBehindMessage = TRANSLATOR.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE);
              } else if (pullsBehind == 1) {
                commitsBehindMessage = TRANSLATOR.getTranslation(Tags.ONE_COMMIT_BEHIND);
              } else {
                commitsBehindMessage = MessageFormat.format(TRANSLATOR.getTranslation(Tags.COMMITS_BEHIND), pullsBehind);
              }
              branchTooltip += commitsBehindMessage + "<br>";

              if (pushesAhead == 0) {
                commitsAheadMessage = TRANSLATOR.getTranslation(Tags.NOTHING_TO_PUSH);
              } else if (pushesAhead == 1) {
                commitsAheadMessage = TRANSLATOR.getTranslation(Tags.ONE_COMMIT_AHEAD);
              } else {
                commitsAheadMessage = MessageFormat.format(TRANSLATOR.getTranslation(Tags.COMMITS_AHEAD), pushesAhead);
              }
              branchTooltip += commitsAheadMessage;
            }

            branchTooltip = TextFormatUtil.toHTML(branchTooltip);

            return branchTooltip;
  }


  /**
   * Adds the branches given as a parameter to the branchSplitMenuButton.
   * 
   * @param branches A list with the branches to be added.
   */
  private void addBranchesToCombo(final List<String> branches) {
    inhibitBranchSelectionListener = true;
    branches.forEach(this::addItem);
    inhibitBranchSelectionListener = false;

    if (detachedHeadId != null) {
      this.addItem(detachedHeadId);
    }

    final String currentBranchName = GIT_ACCESS.getBranchInfo().getBranchName();
    this.setSelectedItem(currentBranchName);
  }


  /**
   * Updates the local branches in the combo popup.
   */
  private void updateBranchesPopup() {
    final boolean isVisible = isComboPopupShowing;
    final List<String> branches = getBranches();

    SwingUtilities.invokeLater(() -> {
      this.hidePopup();
      this.removeAllItems();
      addBranchesToCombo(branches);
      this.revalidate();
      if (isVisible) {
        this.showPopup();
      }
    });
  }


  /**
   * Gets all the local branches from the current repository.
   * 
   * @return The list of local branches.
   */
  private List<String> getBranches() {
    List<String> localBranches = new ArrayList<>();
    try {
      localBranches = BranchesUtil.getLocalBranches();
    } catch (NoRepositorySelected e1) {
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e1.getMessage(), e1);
    }
    return localBranches;
  }


  /**
   * The action performed for this Abstract Action
   * 
   * @param oldBranchInfo Old branch info.
   * @param newBranchName New branch name.
   */
  private void tryCheckingOutBranch(final BranchInfo oldBranchInfo, final String newBranchName) {
    final RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
    if (oldBranchInfo.isDetached() && !RepoUtil.isRepoRebasing(repoState)) {
      detachedHeadId = null;
      this.removeItem(oldBranchInfo.getBranchName());
    }

    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        final Optional<IGitViewProgressMonitor> progMon = Optional.of(
            new GitOperationProgressMonitor(new ProgressDialog(TRANSLATOR.getTranslation(Tags.SWITCH_BRANCH), true)));
        GIT_ACCESS.setBranch(newBranchName, progMon);
        BranchesUtil.fixupFetchInConfig(GIT_ACCESS.getRepository().getConfig());
      } catch (CheckoutConflictException ex) {
        restoreCurrentBranchSelectionInMenu();
        BranchesUtil.showBranchSwitchErrorMessage();
      } catch (GitAPIException | JGitInternalException | IOException | NoRepositorySelected ex) {
        restoreCurrentBranchSelectionInMenu();
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
      }
    });
  }


  /**
   * Restore current branch selection in branches menu.
   */
  private void restoreCurrentBranchSelectionInMenu() {
    final String currentBranchName = GIT_ACCESS.getBranchInfo().getBranchName();
    final int itemCount = this.getItemCount();
    for (int i = 0; i < itemCount; i++) {
      String branch = this.getItemAt(i);
      if (branch.equals(currentBranchName)) {
        this.setSelectedItem(branch);
        break;
      }
    }
  }


  /**
   * Returns the instance of JToolTip that should be used to display the tooltip. 
   * Components typically would not override this method,but it can be used to cause different tooltips
   * to be displayed differently.
   * <br>
   * Overrides: createToolTip() in JComponent
   * 
   * @return the JToolTip used to display this toolTip
   */
  @Override
  public JToolTip createToolTip() {
    return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
  }


  @Override
  public Dimension getMinimumSize() {
    return new Dimension(UIUtil.DUMMY_MIN_WIDTH, getPreferredSize().height);
  }
}
