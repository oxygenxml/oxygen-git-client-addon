package com.oxygenxml.git.view.staging;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.CredentialsBase;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepoNotInitializedException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.branches.BranchTreeMenuActionsProvider;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.BranchStatusDialog;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.LoginDialog;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.dialog.PassphraseDialog;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.CommitsAheadAndBehind;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;
import com.oxygenxml.git.view.stash.ListStashesDialog;
import com.oxygenxml.git.view.stash.StashChangesDialog;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Contains additional support buttons like push, pull, branch select, submodule
 * select
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel {

  /**
   * Pull action.
   */
  private final class PullAction extends AbstractAction {

    private static final String PULL_TYPE_ACTION_PROP = "pullType";

    private final PullType pullType;

    public PullAction(String name, PullType pullType) {
      super(name);
      this.pullType = pullType;
      putValue(PULL_TYPE_ACTION_PROP, pullType);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        if (GitAccess.getInstance().getRepository() != null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pull action invoked");
          }
          gitController.pull(pullType);
          OptionsManager.getInstance().saveDefaultPullType(pullType);
        }
      } catch (NoRepositorySelected e1) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e1, e1);
        }
      }
    }
  }

  /**
   * The font size of the push/pull counters.
   */
  private static final float PUSH_PULL_COUNTERS_FONT_SIZE = 8.5f;

  /**
   * Pull button extra width, for beautifying reasons.
   */
  private static final int PULL_BUTTON_EXTRA_WIDTH = 4;

  /**
   * Toolbar button default extra width, for beautifying reasons.
   */
  private static final int TOOLBAR_BUTTON_DEFAULT_EXTRA_WIDTH = 8;

  /**
   * Distance between text and decoration
   */
  private static final int DECORATION_DISPLACEMENT = 13;

  /**
   * Maximum number of commits to be displayed in pull/push buttons tooltips.
   */
  private static final int MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS = 5;

  /**
   * Maximum commit message length in tooltip.
   */
  private static final int MAXIMUM_COMMIT_MESSAGE_LENGTH = 60;

  /**
   * The date format for the commits in the push/pull tooltips.
   */
  private final SimpleDateFormat commitDateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm");

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(ToolbarPanel.class);

  /**
   * Toolbar in which the button will be placed
   */
  private JToolBar gitToolbar;

  /**
   * SplitMenuButton for selecting the local branch.
   */
  private final SplitMenuButton branchesSplitMenuButton;

  /**
   * Used to execute the push and pull commands
   */
  private final GitController gitController;

  /**
   * Button for push
   */
  private ToolbarButton pushButton;

  /**
   * Button for stash
   */
  private SplitMenuButton stashButton;

  /**
   * Button with menu for pull (with merge or rebase).
   */
  private SplitMenuButton pullMenuButton;

  /**
   * Settings button.
   */
  private SplitMenuButton settingsMenuButton;

  /**
   * Button for selecting the submodules
   */
  private ToolbarButton submoduleSelectButton;

  /**
   * Button for history
   */
  private ToolbarButton historyButton;

  /**
   * Button for cloning a new repository
   */
  private ToolbarButton cloneRepositoryButton;

  /**
   * Counter for how many pushes the local copy is ahead of the base
   */
  private int pushesAhead = 0;

  /**
   * Counter for how many stahses has the repository.
   */
  private int noOfStashes = 0;

  /**
   * Counter for how many pulls the local copy is behind the base
   */
  private int pullsBehind = 0;

  /**
   * The translator for the messages that are displayed in this panel
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   * Main panel refresh
   */
  private final GitRefreshSupport refreshSupport;

  /**
   * Branch selection button.
   */
  private ToolbarButton branchSelectButton;

  /**
   * The stash all changes item.
   */
  private JMenuItem stashChangesMenuItem;

  /**
   * The list stahses item.
   */
  private JMenuItem listStashesMenuItem;

  /**
   * Constructor.
   * 
   * @param gitController     Git controller.
   * @param refreshSupport    The refresh support.
   * @param historyController History controller.
   * @param branchManagementViewPresenter Branch management view presenter.
   */
  public ToolbarPanel(
      GitController gitController, 
      GitRefreshSupport refreshSupport,
      HistoryController historyController,
      BranchManagementViewPresenter branchManagementViewPresenter) {
    this.gitController = gitController;
    this.refreshSupport = refreshSupport;
    this.branchesSplitMenuButton = new SplitMenuButton(null, null, true, false, true, true);

    createGUI(historyController, branchManagementViewPresenter);

    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.OPEN_WORKING_COPY) {
          // Repository changed. Update the toolbar buttons.
          submoduleSelectButton.setEnabled(gitRepoHasSubmodules());

          // Update the toobars.
          // calculate how many pushes ahead and pulls behind the current
          // selected working copy is from the base. It is on thread because
          // the fetch command takes a longer time
          // TODO This might stay well in the Refresh support... When a new repository is 
          // selected this is triggered.
          // TODO Maybe the change of repository should triggered a fetch and a notification should
          // be fired when the fetch information is brought. It might make sense to use a coalescing for the fetch.
          new Thread(() -> {
            fetch(true);
            // After the fetch is done, update the toolbar icons.
            refresh();
          }).start();
        } else if (operation == GitOperation.ABORT_REBASE || operation == GitOperation.CONTINUE_REBASE || operation == GitOperation.COMMIT) {
          // Update status because we are coming from a detached HEAD
          refresh();
        }
      }
    });
  }

  /**
   * @return <code>true</code> if we have submodules.
   */
  boolean gitRepoHasSubmodules() {
    return !GitAccess.getInstance().getSubmoduleAccess().getSubmodules().isEmpty();
  }

  /**
   * Fetch.
   * 
   * @param firstRun <code>true</code> if this the first fetch.
   */
  private void fetch(boolean firstRun) {
    try {
      GitAccess.getInstance().fetch();
    } catch (SSHPassphraseRequiredException e) {
      String message = null;
      if (firstRun) {
        message = TRANSLATOR.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      } else {
        message = TRANSLATOR.getTranslation(Tags.PREVIOUS_PASS_PHRASE_INVALID)
            + " " 
            + TRANSLATOR.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      }

      String passphrase = new PassphraseDialog(message).getPassphrase();
      if(passphrase != null){
        // A new pass phase was given. Try again.
        fetch(false);
      }
    } catch (PrivateRepositoryException e) {
      String loginMessage = null;
      String hostName = GitAccess.getInstance().getHostName();
      if (firstRun) {
        loginMessage = TRANSLATOR.getTranslation(Tags.LOGIN_DIALOG_PRIVATE_REPOSITORY_MESSAGE);
      } else {
        loginMessage = TRANSLATOR.getTranslation(Tags.AUTHENTICATION_FAILED) + " ";
        CredentialsBase gitCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
        if (gitCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
          loginMessage += TRANSLATOR.getTranslation(Tags.CHECK_CREDENTIALS);
        } else {
          loginMessage += TRANSLATOR.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS);
        }
      }

      LoginDialog loginDlg = new LoginDialog(hostName, loginMessage);
      if (loginDlg.getCredentials() != null) {
        // New credentials were specified. Try again.
        fetch(false);
      }
    } catch (RepositoryUnavailableException e) {
      // Nothing we can do about it...
    }
  }

  /**
   * Enables/Disables the buttons.
   * 
   * @param enabled <code>true</code> to enable the buttons. <code>false</code> to disable them.
   */
  public void updateButtonState(boolean enabled) {
    pushButton.setEnabled(enabled);
    pullMenuButton.setEnabled(enabled);
    cloneRepositoryButton.setEnabled(enabled);
    submoduleSelectButton.setEnabled(enabled && gitRepoHasSubmodules());
    branchSelectButton.setEnabled(enabled);
    historyButton.setEnabled(enabled);
    settingsMenuButton.setEnabled(enabled);
    stashButton.setEnabled(enabled);
  }

  public ToolbarButton getSubmoduleSelectButton() {
    return submoduleSelectButton;
  }

  /**
   * Sets the panel layout and creates all the buttons with their functionality
   * making them visible
   * 
   * @param historyController History controller.
   * @param branchManagementViewPresenter Branches presenter.
   */
  public void createGUI(
      HistoryController historyController,
      BranchManagementViewPresenter branchManagementViewPresenter) {
    gitToolbar = new JToolBar();
    gitToolbar.setOpaque(false);
    gitToolbar.setFloatable(false);
    this.setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;

    addCloneRepositoryButton();
    addPushAndPullButtons();
    addBranchSelectButton(branchManagementViewPresenter);
    addSubmoduleSelectButton();
    addStashButton();
    addHistoryButton(historyController);
    addSettingsButton();
    this.add(gitToolbar, gbc);

    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1;
    gbc.weighty = 0;
    this.add(branchesSplitMenuButton, gbc);

    this.setMinimumSize(new Dimension(UIConstants.MIN_PANEL_WIDTH, UIConstants.TOOLBAR_PANEL_HEIGHT));

  }

  /**
   * Updates the local branches in the split menu button where you can checkout them.
   */
  private void updateBranchesMenu() {
    boolean isVisible = branchesSplitMenuButton.isPopupMenuVisible();
    branchesSplitMenuButton.setPopupMenuVisible(false);

    branchesSplitMenuButton.removeAll();
    addActionsToBranchSplitMenuButton(getBranches());

    branchesSplitMenuButton.revalidate();
    branchesSplitMenuButton.setPopupMenuVisible(isVisible);
  }

  /**
   * Adds the branches given as a parameter to the branchSplitMenuButton.
   * 
   * @param branches A list with the branches to be added.
   */
  private void addActionsToBranchSplitMenuButton(List<String> branches) {
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    ButtonGroup branchActionsGroup = new ButtonGroup();

    branches.forEach((String branchName) -> {
      AbstractAction checkoutAction = createCheckoutActionForBranch(branchName);
      JRadioButtonMenuItem branchRadioButtonMenuItem = new JRadioButtonMenuItem(checkoutAction);
      branchActionsGroup.add(branchRadioButtonMenuItem);
      branchesSplitMenuButton.add(branchRadioButtonMenuItem);
      if (branchName.equals(currentBranchName)) {
        branchRadioButtonMenuItem.setSelected(true);
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
   * Creates the checkout action for a local branch.
   * 
   * @param branchName The name of the branch to checkout.
   * 
   * @return The action created.
   */
  private AbstractAction createCheckoutActionForBranch(String branchName) {
    return new AbstractAction(branchName) {
      @Override
      public void actionPerformed(ActionEvent e) {
        RepositoryState repoState = null;
        try {
          repoState = GitAccess.getInstance().getRepository().getRepositoryState();
        } catch (NoRepositorySelected e1) {
          LOGGER.error(e1, e1);
        }
        if(RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
          int answer = BranchStatusDialog.showQuestionMessage(TRANSLATOR.getTranslation(Tags.SWITCH_BRANCH),
              TRANSLATOR.getTranslation(Tags.UNCOMMITTED_CHANGES_WHEN_SWITCHING_BRANCHES),
              TRANSLATOR.getTranslation(Tags.STASH_CHANGES),
              TRANSLATOR.getTranslation(Tags.MOVE_CHANGES),
              TRANSLATOR.getTranslation(Tags.CANCEL)
              );
          if( (answer == OKOtherAndCancelDialog.RESULT_OTHER) ||
              (answer == OKOtherAndCancelDialog.RESULT_OK && 
              BranchTreeMenuActionsProvider.wasStashSuccessfullyCreated())
              ) {
            tryCheckingOutBranch(branchName);
          } else {
            restoreCurrentBranchSelectionInMenu();
          }
        } else {
          tryCheckingOutBranch(branchName);
        }
      }
    };
    
  }

  /**
   * The action performed for this Abstract Action
   * 
   * @param branchName Branch name.
   */
  private void tryCheckingOutBranch(String branchName) {
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        GitAccess.getInstance().setBranch(branchName);
        BranchesUtil.fixupFetchInConfig(GitAccess.getInstance().getRepository().getConfig());
      } catch (CheckoutConflictException ex) {
        LOGGER.debug(ex, ex);
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
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();
    Component[] menuComponents = branchesSplitMenuButton.getMenuComponents();
    for (Component component : menuComponents) {
      JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) component;
      if (menuItem.getText().equals(currentBranchName)) {
        menuItem.setSelected(true);
        break;
      }
    }
  }

  /**
   * Add the settings button.
   */
  private void addSettingsButton() {
    settingsMenuButton = SettingsMenuBuilder.build(refreshSupport);

    gitToolbar.add(settingsMenuButton);
  }

  /**
   * Add the "Clone repository" button.
   */
  private void addCloneRepositoryButton() {
    Action cloneRepositoryAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new CloneRepositoryDialog();
      }
    };

    cloneRepositoryButton = new ToolbarButton(cloneRepositoryAction, false);
    cloneRepositoryButton.setIcon(Icons.getIcon(Icons.GIT_CLONE_REPOSITORY_ICON));
    cloneRepositoryButton.setToolTipText(TRANSLATOR.getTranslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP));
    setDefaultToolbarButtonWidth(cloneRepositoryButton);

    gitToolbar.add(cloneRepositoryButton);
  }

  /**
   * @param historyController History interface.
   */
  private void addHistoryButton(HistoryController historyController) {
    Action historyAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        historyController.showRepositoryHistory();
      }
    };

    historyButton = new ToolbarButton(historyAction, false);
    historyButton.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
    historyButton.setToolTipText(TRANSLATOR.getTranslation(Tags.SHOW_CURRENT_BRANCH_HISTORY));
    setDefaultToolbarButtonWidth(historyButton);

    gitToolbar.add(historyButton);

  }

  /**
   * Adds to the tool bar a button for selecting submodules. When clicked, a new
   * dialog appears that shows all the submodules for the current repository and
   * allows the user to select one of them
   */
  private void addSubmoduleSelectButton() {
    Action branchSelectAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if (GitAccess.getInstance().getRepository() != null) {
            new SubmoduleSelectDialog();
          }
        } catch (NoRepositorySelected e1) {
          if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(e1, e1);
          }
        }
      }
    };
    submoduleSelectButton = new ToolbarButton(branchSelectAction, false);
    submoduleSelectButton.setIcon(Icons.getIcon(Icons.GIT_SUBMODULE_ICON));
    submoduleSelectButton.setToolTipText(TRANSLATOR.getTranslation(Tags.SELECT_SUBMODULE_BUTTON_TOOLTIP));
    setDefaultToolbarButtonWidth(submoduleSelectButton);

    gitToolbar.add(submoduleSelectButton);

    submoduleSelectButton.setEnabled(gitRepoHasSubmodules());
  }

  /**
   * Adds to the tool bar a button for selecting branches. When clicked, a new
   * dialog appears that shows all the branches for the current repository and
   * allows the user to select one of them.
   * 
   * @param branchManagementViewPresenter Branches presenter.
   */
  private void addBranchSelectButton(BranchManagementViewPresenter branchManagementViewPresenter) {
    Action branchSelectAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if (GitAccess.getInstance().getRepository() != null) {
            branchManagementViewPresenter.showGitBranchManager();
          }
        } catch (NoRepositorySelected e1) {
          if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(e1, e1);
          }
        }
      }
    };
    branchSelectButton = new ToolbarButton(branchSelectAction, false);
    branchSelectButton.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
    branchSelectButton.setToolTipText(TRANSLATOR.getTranslation(Tags.BRANCH_MANAGER_BUTTON_TOOL_TIP));
    setDefaultToolbarButtonWidth(branchSelectButton);

    gitToolbar.add(branchSelectButton);
  }

  /**
   * Updates the presented information, like the Pull-behind, Pushes-ahead
   * and branch status.
   */
  public void refresh() {
    GitAccess gitAccess = GitAccess.getInstance();

    this.pullsBehind = gitAccess.getPullsBehind();
    try {
      this.pushesAhead = gitAccess.getPushesAhead();
    } catch (RepoNotInitializedException e) {
      this.pushesAhead = -1;
      LOGGER.debug(e, e);
    }

    refreshStashButton();

    SwingUtilities.invokeLater(() -> {
      updateBranchesMenu();
      pullMenuButton.repaint();
      pushButton.repaint();
      stashButton.repaint();
    });

    Repository repo = null;
    try {
      repo = gitAccess.getRepository();
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e, e);
    }

    BranchInfo branchInfo = gitAccess.getBranchInfo();
    String currentBranchName = branchInfo.getBranchName();
    String branchInfoText = "";
    if (branchInfo.isDetached()) {
      branchInfoText += "<html><b>" + branchInfo.getShortBranchName() + "</b></html>";
      String tooltipText = "<html>"
          + TRANSLATOR.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
          + " "
          + currentBranchName;
      if (repo != null && repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
        tooltipText += "<br>" + TRANSLATOR.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
      }
      tooltipText += "</html>";
      String finalText = tooltipText;
      SwingUtilities.invokeLater(() -> {
        branchesSplitMenuButton.setToolTipText(finalText);
        pushButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
        pullMenuButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
      });
    } else {
      String branchTooltip = null;
      if (currentBranchName != null && !currentBranchName.isEmpty()) {
        branchInfoText = "<html><b>" + currentBranchName + "</b></html>";

        String upstreamBranchFromConfig = gitAccess.getUpstreamBranchShortNameFromConfig(currentBranchName);
        boolean isAnUpstreamBranchDefinedInConfig = upstreamBranchFromConfig != null;

        String upstreamShortestName = 
            isAnUpstreamBranchDefinedInConfig 
            ? upstreamBranchFromConfig.substring(upstreamBranchFromConfig.lastIndexOf('/') + 1)
                : null;
        Ref remoteBranchRefForUpstreamFromConfig = 
            isAnUpstreamBranchDefinedInConfig 
            ? getRemoteBranch(upstreamShortestName) 
                : null;
        boolean existsRemoteBranchForUpstreamDefinedInConfig = remoteBranchRefForUpstreamFromConfig != null;

        branchTooltip = "<html>"
            + TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH)
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

        branchTooltip += "</html>";

        // ===================== Push button tooltip =====================
        String pushButtonTooltipFinal = updatePushToolTip(
            isAnUpstreamBranchDefinedInConfig, 
            existsRemoteBranchForUpstreamDefinedInConfig,
            upstreamBranchFromConfig, 
            commitsAheadMessage, 
            currentBranchName,
            repo);
        SwingUtilities.invokeLater(() -> pushButton.setToolTipText(pushButtonTooltipFinal));

        //  ===================== Pull button tooltip =====================
        String pullButtonTooltipFinal = updatePullToolTip(
            isAnUpstreamBranchDefinedInConfig,
            existsRemoteBranchForUpstreamDefinedInConfig, 
            upstreamBranchFromConfig,
            commitsBehindMessage,
            remoteBranchRefForUpstreamFromConfig,
            repo);
        SwingUtilities.invokeLater(() -> pullMenuButton.setToolTipText(pullButtonTooltipFinal));

      }
      String branchTooltipFinal = branchTooltip;
      SwingUtilities.invokeLater(() -> branchesSplitMenuButton.setToolTipText(branchTooltipFinal));
    }
    String branchInfoTextFinal = branchInfoText;
    SwingUtilities.invokeLater(() -> branchesSplitMenuButton.setText(branchInfoTextFinal));

  }

  /**
   * Refresh the status for stash button.
   */
  public void refreshStashButton() {
    GitAccess gitAccess = GitAccess.getInstance();
    Collection<RevCommit> stashes = gitAccess.listStashes();
    noOfStashes = stashes == null ? 0 : stashes.size();

    List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
    boolean existsLocalFiles = unstagedFiles != null && !unstagedFiles.isEmpty();
    if(!existsLocalFiles) {
      List<FileStatus> stagedFiles = gitAccess.getStagedFiles();
      existsLocalFiles = stagedFiles != null && !stagedFiles.isEmpty();
    }

    stashChangesMenuItem.setEnabled(existsLocalFiles);

    Collection<RevCommit> stashesList = GitAccess.getInstance().listStashes();
    boolean existsStashes = stashesList != null && !stashesList.isEmpty();

    listStashesMenuItem.setEnabled(existsStashes);

    stashButton.setEnabled(existsLocalFiles || existsStashes);
  }


  /**
   * Updates the tool tip of "Push" Button.
   * 
   * @param isAnUpstreamBranchDefinedInConfig              <code>true</code> if is an upstream branch defined in configurations
   * @param existsRemoteBranchForUpstreamDefinedInConfig   <code>true</code> if exists remote branch for upstream defined in configurations
   * @param upstreamBranchFromConfig                       The upstream branch from configurations
   * @param commitsAheadMessage                            The commits ahead message
   * @param currentBranchName                              The name of the current branch
   * @param repo                                           The current repository
   * 
   * @return updated "Push" button tool tip text.
   */
  private String updatePushToolTip(boolean isAnUpstreamBranchDefinedInConfig, 
      boolean existsRemoteBranchForUpstreamDefinedInConfig, 
      String upstreamBranchFromConfig, 
      String commitsAheadMessage, 
      String currentBranchName, 
      Repository repo) {
    StringBuilder pushButtonTooltip = new StringBuilder();
    pushButtonTooltip.append("<html>");
    if (isAnUpstreamBranchDefinedInConfig) {
      if (existsRemoteBranchForUpstreamDefinedInConfig) {
        // The "normal" case. The upstream branch defined in "config" exists in the remote repository.
        String pushToMsg = MessageFormat.format(TRANSLATOR.getTranslation(Tags.PUSH_TO), upstreamBranchFromConfig);
        pushButtonTooltip.append(pushToMsg)
        .append(".<br>")
        .append(commitsAheadMessage);
        try {
          CommitsAheadAndBehind commitsAheadAndBehind = 
              RevCommitUtil.getCommitsAheadAndBehind(repo, currentBranchName);
          if (commitsAheadAndBehind != null && commitsAheadAndBehind.getCommitsAhead() != null) {
            List<RevCommit> commitsAhead = commitsAheadAndBehind.getCommitsAhead();
            pushButtonTooltip.append("<br><br>");
            addCommitsToTooltip(commitsAhead, pushButtonTooltip);
            if(commitsAhead.size() > MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS) {
              pushButtonTooltip.append("<br>").append(TRANSLATOR.getTranslation(Tags.SEE_ALL_COMMITS_IN_GIT_HISTORY));
            }
          }
        } catch (IOException | GitAPIException e) {
          LOGGER.error(e, e);
        } 
      } else {
        // There is an upstream branch defined in "config",
        // but that branch does not exist in the remote repository.
        pushButtonTooltip.append(MessageFormat.format(
            TRANSLATOR.getTranslation(Tags.PUSH_TO_CREATE_AND_TRACK_REMOTE_BRANCH),
            currentBranchName));
      }
    } else {
      updatePushTooltipWhenNoUpstream(currentBranchName, pushButtonTooltip);
    }
    pushButtonTooltip.append("</html>");

    return pushButtonTooltip.toString();

  }

  /**
   * Update the push button tooltip when no upstream defined.
   * 
   * @param currentBranchName Current branch.
   * @param tooltipBuilder    Tooltip builder.
   */
  private void updatePushTooltipWhenNoUpstream(String currentBranchName, StringBuilder tooltipBuilder) {
    Ref remoteBranchWithLocalBranchName = getRemoteBranch(currentBranchName);
    if (remoteBranchWithLocalBranchName != null) {
      // No upstream branch defined in "config", but there is a remote branch
      // that has the same name as the local branch.
      tooltipBuilder.append(MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.PUSH_TO_TRACK_REMOTE_BRANCH),
          currentBranchName));
    } else {
      // No upstream branch defined in "config" and no remote branch
      // that has the same name as the local branch.
      tooltipBuilder.append(MessageFormat.format(
          TRANSLATOR.getTranslation(Tags.PUSH_TO_CREATE_AND_TRACK_REMOTE_BRANCH),
          currentBranchName));
    }
  }



  /**
   * Updates the tool tip of "Push" Button.
   * 
   * @param isAnUpstreamBranchDefinedInConfig              <code>true</code> if is an upstream branch defined in configurations
   * @param existsRemoteBranchForUpstreamDefinedInConfig   <code>true</code> if exists remote branch for upstream defined in configurations
   * @param upstreamBranchFromConfig                       The upstream branch from configurations
   * @param commitsBehindMessage                           The commits behind message
   * @param remoteBranchRefForUpstreamFromConfig           The remote branch reference for upstream from configurations.
   * @param repo                                           Current repo.
   * 
   * @return updated "Push" button tool tip text.
   */
  private String updatePullToolTip(boolean isAnUpstreamBranchDefinedInConfig, 
      boolean existsRemoteBranchForUpstreamDefinedInConfig, 
      String upstreamBranchFromConfig, 
      String commitsBehindMessage, 
      Ref remoteBranchRefForUpstreamFromConfig,
      Repository repo) {

    StringBuilder pullButtonTooltip = new StringBuilder();
    pullButtonTooltip.append("<html>");
    String currentBranchName = GitAccess.getInstance().getBranchInfo().getBranchName();

    if (isAnUpstreamBranchDefinedInConfig) {
      if (existsRemoteBranchForUpstreamDefinedInConfig) {
        // The "normal" case. The upstream branch defined in "config" exists in the remote repository.
        String pullFromMsg = MessageFormat.format(
            TRANSLATOR.getTranslation(getPullFromTranslationTag()),
            Repository.shortenRefName(remoteBranchRefForUpstreamFromConfig.getName()));
        pullButtonTooltip.append(pullFromMsg)
        .append(".<br>")
        .append(commitsBehindMessage);
        try {
          CommitsAheadAndBehind commitsAheadAndBehind = 
              RevCommitUtil.getCommitsAheadAndBehind(repo, currentBranchName);
          if(commitsAheadAndBehind != null && commitsAheadAndBehind.getCommitsBehind() != null) {
            List<RevCommit> commitsBehind = commitsAheadAndBehind.getCommitsBehind();
            pullButtonTooltip.append("<br><br>");
            addCommitsToTooltip(commitsBehind, pullButtonTooltip);
            if(commitsBehind.size() > MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS) {
              pullButtonTooltip.append("<br>").append(TRANSLATOR.getTranslation(Tags.SEE_ALL_COMMITS_IN_GIT_HISTORY));
            }
          }
        } catch (IOException | GitAPIException e) {
          LOGGER.error(e, e);
        }

      } else {
        // The upstream branch defined in "config" does not exists in the remote repository.
        String upstreamDoesNotExistMsg = MessageFormat.format(
            StringUtils.capitalize(TRANSLATOR.getTranslation(Tags.UPSTREAM_BRANCH_DOES_NOT_EXIST)),
            upstreamBranchFromConfig);
        pullButtonTooltip.append(TRANSLATOR.getTranslation(Tags.CANNOT_PULL))
        .append("<br>")
        .append(upstreamDoesNotExistMsg);
      }
    } else {
      updatePullTooltipWhenNoUpstream(pullButtonTooltip, currentBranchName);
    }

    pullButtonTooltip.append("</html>");
    return pullButtonTooltip.toString();
  }

  /**
   * Update pull button tooltip when no upstream branch is defined.
   * 
   * @param tooltipBuilder    Tooltip builder.
   * @param currentBranchName Current branch name.
   */
  private void updatePullTooltipWhenNoUpstream(
      StringBuilder tooltipBuilder,
      String currentBranchName) {
    Ref remoteBranchWithLocalBranchName = getRemoteBranch(currentBranchName);
    if (remoteBranchWithLocalBranchName != null) {
      // No upstream defined in config, but there is a remote branch
      // that has the same name as the local branch
      tooltipBuilder.append(MessageFormat.format(
          TRANSLATOR.getTranslation(getPullFromTranslationTag()),
          Repository.shortenRefName(remoteBranchWithLocalBranchName.getName()))).append(".<br>");
    } else {
      // No upstream branch defined in "config" and no remote branch
      // that has the same name as the local branch.
      tooltipBuilder.append(TRANSLATOR.getTranslation(Tags.CANNOT_PULL)).append("<br>").append(MessageFormat.format(
          StringUtils.capitalize(TRANSLATOR.getTranslation(Tags.NO_REMOTE_BRANCH)),
          currentBranchName)).append(".");
    }
  }


  /**
   * Update the tooltip text with info about the incoming/outgoing commits.
   * 
   * @param commits The list with new commits.
   * @param text    The text of the message.
   * 
   * @throws IOException
   * @throws GitAPIException
   */
  void addCommitsToTooltip(List<RevCommit> commits, StringBuilder text) throws IOException, GitAPIException {
    List<FileStatus> changedFiles;
    int noOfCommits = commits.size();
    int i = 0;
    while(i < noOfCommits) {
      RevCommit currentCommit = commits.get(i);
      String commitMessage = currentCommit.getShortMessage();
      if(commitMessage.length() > MAXIMUM_COMMIT_MESSAGE_LENGTH) {
        commitMessage = commitMessage.substring(0, MAXIMUM_COMMIT_MESSAGE_LENGTH).trim() + "...";
      }
      changedFiles = RevCommitUtil.getChangedFiles(currentCommit.getId().getName());
      text.append("&#x25AA; ")
      .append(commitDateFormat.format(currentCommit.getAuthorIdent().getWhen()))
      .append(" &ndash; ")
      .append(currentCommit.getAuthorIdent().getName())
      .append(" ")
      .append("(")
      .append(changedFiles.size())
      .append((changedFiles.size() > 1) ? " files" : " file")
      .append(")");
      if(commitMessage.length() > 0) {
        text.append("<br>")
        .append("&nbsp;&nbsp;&nbsp;")
        .append(commitMessage);
      }
      text.append("<br>");     
      if(i + 1 == MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS - 1 
          && noOfCommits > MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS) {

        int noOfSkippedCommits = getNoOfSkippedCommits(noOfCommits);

        text.append("&#x25AA; ")
        .append("[")
        .append("...")
        .append("]")
        .append(" &ndash; ")  
        .append(noOfSkippedCommits == 1 ? TRANSLATOR.getTranslation(Tags.ONE_MORE_COMMIT) 
            : MessageFormat.format(TRANSLATOR.getTranslation(Tags.N_MORE_COMMITS), noOfSkippedCommits))    
        .append("<br>");
        // Prepare to get the last commit
        i = noOfCommits - 2;
      } 
      i++;
    } 
  }

  /**
   * Get the number of skipped commits.
   * 
   * @param noOfCommits The total number of commits.
   * 
   * @return the number of skipped commits.
   */
  int getNoOfSkippedCommits(int noOfCommits) {
    return noOfCommits - MAX_NO_OF_COMMITS_IN_PUSH_AND_PULL_TOOLTIPS;
  }


  /**
   * @return The translation tag for the "Pull" button tooltip text.
   */
  private String getPullFromTranslationTag() {
    String pullFromTag = Tags.PULL_FROM;
    Object value = pullMenuButton.getAction().getValue(PullAction.PULL_TYPE_ACTION_PROP);
    if (value instanceof PullType) {
      PullType pt = (PullType) value;
      if (pt == PullType.REBASE) {
        pullFromTag = Tags.PULL_REBASE_FROM;
      } else if (pt != PullType.UKNOWN) {
        pullFromTag = Tags.PULL_MERGE_FROM;
      }
    }
    return pullFromTag;
  }

  /**
   * Get the remote branch that has the given name.
   * This seems to look in ".git\refs\remotes\origin" for the necessary information.
   * 
   * @param branchName Local branch name.
   * 
   * @return The remote branch or <code>null</code>;
   */
  private Ref getRemoteBranch(String branchName) {
    Ref remoteBranchWithLocalBranchName = null;
    if (branchName != null) {
      List<Ref> remoteBrachListForCurrentRepo = GitAccess.getInstance().getRemoteBrachListForCurrentRepo();
      for (Ref remoteBranchRef : remoteBrachListForCurrentRepo) {
        String remoteBranchName = Repository.shortenRefName(remoteBranchRef.getName());
        remoteBranchName = remoteBranchName.substring(remoteBranchName.lastIndexOf('/') + 1);
        if (remoteBranchName.equals(branchName)) {
          remoteBranchWithLocalBranchName = remoteBranchRef;
          break;
        }
      }
    }
    return remoteBranchWithLocalBranchName;
  }

  /**
   * Adds to the tool bar the Push and Pull Buttons
   */
  private void addPushAndPullButtons() {
    // PUSH
    pushButton = createPushButton();
    pushButton.setIcon(Icons.getIcon(Icons.GIT_PUSH_ICON));
    setDefaultToolbarButtonWidth(pushButton);

    // PULL
    pullMenuButton = createPullButton();
    Dimension d = pullMenuButton.getPreferredSize();
    d.width += PULL_BUTTON_EXTRA_WIDTH;
    pullMenuButton.setPreferredSize(d);
    pullMenuButton.setMinimumSize(d);
    pullMenuButton.setMaximumSize(d);

    gitToolbar.add(pushButton);
    gitToolbar.add(pullMenuButton);
  }

  /**
   * Adds to the tool bar the Stash Button.
   */
  private void addStashButton() {
    stashButton = createStashButton();
    refreshStashButton();
    Dimension d = stashButton.getPreferredSize();
    d.width += PULL_BUTTON_EXTRA_WIDTH;
    stashButton.setPreferredSize(d);
    stashButton.setMinimumSize(d);
    stashButton.setMaximumSize(d);

    gitToolbar.add(stashButton);
  }

  /**
   * Create "Pull" button.
   * 
   * @return the "Pull" button.
   */
  private SplitMenuButton createPullButton() {
    SplitMenuButton pullSplitMenuButton = new SplitMenuButton( // NOSONAR (java:S110)
        null,
        Icons.getIcon(Icons.GIT_PULL_ICON),
        false,
        false,
        false,
        true) {

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintPullsBehind(g);
      }

      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }

      /**
       * Paint the number of pulls behind.
       * 
       * @param g Graphics.
       */
      private void paintPullsBehind(Graphics g) {
        String noOfPullsBehindString = "";
        if (pullsBehind > 0) {
          noOfPullsBehindString += pullsBehind;
        }
        if (pullsBehind > 9) {
          setHorizontalAlignment(SwingConstants.LEFT);
        } else {
          setHorizontalAlignment(SwingConstants.CENTER);
        }
        g.setFont(g.getFont().deriveFont(Font.BOLD, PUSH_PULL_COUNTERS_FONT_SIZE));
        FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
        int stringWidth = fontMetrics.stringWidth(noOfPullsBehindString);
        g.setColor(getForeground());
        g.drawString(noOfPullsBehindString,
            // X
            getWidth() - stringWidth - DECORATION_DISPLACEMENT,
            // Y
            fontMetrics.getHeight() - fontMetrics.getDescent() - fontMetrics.getLeading());
      }
    };

    addPullActionsToMenu(pullSplitMenuButton);

    return pullSplitMenuButton;
  }

  /**
   * Add the pull actions (pull + merge, pull + rebase, etc) to the pull menu.
   * 
   * @param splitMenuButton The menu button to add to.
   */
  private void addPullActionsToMenu(SplitMenuButton splitMenuButton) {
    ButtonGroup pullActionsGroup = new ButtonGroup();

    ActionListener radioMenuItemActionListener = e -> {
      if (e.getSource() instanceof JMenuItem) {
        splitMenuButton.setAction(((JMenuItem) e.getSource()).getAction());
      }
    };

    // Pull (merge)
    PullAction pullMergeAction = new PullAction(
        TRANSLATOR.getTranslation(Tags.PULL_MERGE),
        PullType.MERGE_FF);
    JRadioButtonMenuItem pullMergeMenuItem = new JRadioButtonMenuItem(pullMergeAction);
    pullMergeMenuItem.addActionListener(radioMenuItemActionListener);
    splitMenuButton.add(pullMergeMenuItem);
    pullActionsGroup.add(pullMergeMenuItem);

    // Pull (rebase)
    PullAction pullRebaseAction = new PullAction(
        TRANSLATOR.getTranslation(Tags.PULL_REBASE),
        PullType.REBASE);
    JRadioButtonMenuItem pullRebaseMenuItem = new JRadioButtonMenuItem(pullRebaseAction);
    pullRebaseMenuItem.addActionListener(radioMenuItemActionListener);
    splitMenuButton.add(pullRebaseMenuItem);
    pullActionsGroup.add(pullRebaseMenuItem);

    PullType defaultPullType = OptionsManager.getInstance().getDefaultPullType();
    if (defaultPullType == PullType.REBASE) {
      splitMenuButton.setAction(pullRebaseMenuItem.getAction());
      pullRebaseMenuItem.setSelected(true);
    } else if (defaultPullType != PullType.UKNOWN) {
      splitMenuButton.setAction(pullMergeMenuItem.getAction());
      pullMergeMenuItem.setSelected(true);
    }
  }

  /**
   * Create the "Stash" button.
   * 
   * @return the "Stash" button.
   */
  private SplitMenuButton createStashButton() {
    SplitMenuButton stashLocalButton = new SplitMenuButton( // NOSONAR (java:S110)
        null,
        Icons.getIcon(Icons.STASH_ICON),
        false,
        false,
        true,
        true) {

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintStashes(g);
      }

      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }

      @Override
      public void setToolTipText(String text) {
        super.setToolTipText(TRANSLATOR.getTranslation(Tags.STASH));
      }

      /**
       * Paint the number pushes ahead.
       * 
       * @param g Graphics.
       */
      private void paintStashes(Graphics g) {
        String noOfStashesString = "";
        Collection<RevCommit> stashes = GitAccess.getInstance().listStashes();

        noOfStashes = stashes == null ? 0 : stashes.size();

        if (noOfStashes > 0) {
          noOfStashesString = "" + noOfStashes;
        }
        if (noOfStashes > 9) {
          stashButton.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
          stashButton.setHorizontalAlignment(SwingConstants.CENTER);
        }
        g.setFont(g.getFont().deriveFont(Font.BOLD, PUSH_PULL_COUNTERS_FONT_SIZE));
        FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
        int stringWidth = fontMetrics.stringWidth(noOfStashesString);
        g.setColor(getForeground());
        g.drawString(
            noOfStashesString,
            // X
            stashButton.getWidth() - stringWidth,
            // Y
            stashButton.getHeight() - fontMetrics.getDescent());
      }
    }; 

    stashLocalButton.setToolTipText(TRANSLATOR.getTranslation(Tags.STASH));

    addStashActionsToMenu(stashLocalButton);

    return stashLocalButton;
  }


  /**
   * Add the stash actions (stashes) to the Stash menu.
   * 
   * @param splitMenuButton The menu button to add to.
   * 
   * @return <code>true</code> if the button has at least an action.
   */
  private void addStashActionsToMenu(SplitMenuButton splitMenuButton) {
    ButtonGroup stashActionsGroup = new ButtonGroup();

    ActionListener menuItemActionListener = e -> {
      if (e.getSource() instanceof JMenuItem) {
        splitMenuButton.setAction(((JMenuItem) e.getSource()).getAction());
      }
    };

    Action stashAChangesAction = createStashChangesAction();
    stashChangesMenuItem = new JMenuItem(stashAChangesAction);

    stashChangesMenuItem.addActionListener(menuItemActionListener);
    splitMenuButton.add(stashChangesMenuItem);
    stashActionsGroup.add(stashChangesMenuItem);  

    Action stashesAction = new AbstractAction(TRANSLATOR.getTranslation(Tags.LIST_STASHES)) {

      @Override
      public void actionPerformed(ActionEvent e) {
        ListStashesDialog stashesDialog = new ListStashesDialog();
        stashesDialog.setVisible(true);   
      }

    };

    listStashesMenuItem = new JMenuItem(stashesAction);
    listStashesMenuItem.addActionListener(menuItemActionListener);


    splitMenuButton.add(listStashesMenuItem);
    stashActionsGroup.add(listStashesMenuItem);

  }


  /**
   * Create the "Push" button.
   * 
   * @return the "Push" button.
   */
  private ToolbarButton createPushButton() {
    return new ToolbarButton(createPushAction(), false) { // NOSONAR (java:S110)

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintPushesAhead(g);
      }

      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
      }

      /**
       * Paint the number pushes ahead.
       * 
       * @param g Graphics.
       */
      private void paintPushesAhead(Graphics g) {
        String noOfPushesAheadString = "";
        if (pushesAhead > 0) {
          noOfPushesAheadString = "" + pushesAhead;
        }
        if (pushesAhead > 9) {
          pushButton.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
          pushButton.setHorizontalAlignment(SwingConstants.CENTER);
        }
        g.setFont(g.getFont().deriveFont(Font.BOLD, PUSH_PULL_COUNTERS_FONT_SIZE));
        FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
        int stringWidth = fontMetrics.stringWidth(noOfPushesAheadString);
        g.setColor(getForeground());
        g.drawString(
            noOfPushesAheadString,
            // X
            pushButton.getWidth() - stringWidth,
            // Y
            pushButton.getHeight() - fontMetrics.getDescent());
      }
    };
  }


  /**
   * Create the "Stash changes" action.
   * 
   * @return the "Stash changes" action
   */
  private Action createStashChangesAction() {
    return new AbstractAction(TRANSLATOR.getTranslation(Tags.STASH_CHANGES)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(stashCanBeCreated()) {
          StashChangesDialog dialog = new StashChangesDialog();
          if(dialog.getResult() == OKCancelDialog.RESULT_OK) {
            String description = dialog.getStashMessage();

            if("".compareTo(description) == 0) {
              description = "WIP on " 
                  + GitAccess.getInstance().getBranchInfo().getBranchName() 
                  + " [" 
                  + commitDateFormat.format(new Date())
                  + "]";
            } 

            GitAccess.getInstance().createStash(dialog.shouldIncludeUntracked(), description);
            Collection<RevCommit> stashes = GitAccess.getInstance().listStashes();
            noOfStashes = stashes != null ? stashes.size() : 0;
          }
        } else {
          PluginWorkspaceProvider.getPluginWorkspace()
          .showErrorMessage(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICTS_FIRST));
        }
      }
    };
  }


  /**
   * @return <code>true</code> if the stash can be created.
   */
  private boolean stashCanBeCreated() {
    List<FileStatus> unstagedFiles = GitAccess.getInstance().getUnstagedFiles();
    boolean isConflict =  unstagedFiles != null 
        && !unstagedFiles.isEmpty() 
        && unstagedFiles.stream().anyMatch(file -> file.getChangeType() == GitChangeType.CONFLICT);

    return !isConflict;
  }


  /**
   * Create the "Push" action.
   * 
   * @return the "Push" action
   */
  private Action createPushAction() {
    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if (GitAccess.getInstance().getRepository() != null) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Push Button Clicked");
            }

            gitController.push();
            if (pullsBehind == 0) {
              pushesAhead = 0;
            }
          }
        } catch (NoRepositorySelected e1) {
          if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(e1, e1);
          }
        }
      }
    };
  }

  /**
   * Sets a custom width on the given button
   * 
   * @param button the button to set the width to.
   */
  private void setDefaultToolbarButtonWidth(AbstractButton button) {
    Dimension d = button.getPreferredSize();
    d.width += TOOLBAR_BUTTON_DEFAULT_EXTRA_WIDTH;
    button.setPreferredSize(d);
    button.setMinimumSize(d);
    button.setMaximumSize(d);
  }

  public SplitMenuButton getStashButton() {
    return stashButton;
  }

  public ToolbarButton getPushButton() {
    return pushButton;
  }

  public SplitMenuButton getPullMenuButton() {
    return pullMenuButton;
  }

  public SplitMenuButton getBranchSplitMenuButton() {
    return branchesSplitMenuButton;
  }

  public SplitMenuButton getSettingsMenuButton() {
    return settingsMenuButton;
  }
}
