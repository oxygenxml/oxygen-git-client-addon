package com.oxygenxml.git.view.staging;

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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
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
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RepoNotInitializedException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.actions.internal.PullAction;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.BranchSwitchConfirmationDialog;
import com.oxygenxml.git.view.dialog.OKOtherAndCancelDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.CommitsAheadAndBehind;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;
import com.oxygenxml.git.view.refresh.IRefreshable;
import com.oxygenxml.git.view.stash.StashUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Contains additional support buttons like push, pull, branch select, submodule
 * select
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel implements IRefreshable {

  /**
   * The git access.
   */
  private static final GitAccess GIT_ACCESS = GitAccess.getInstance();
  
  /**
   * The value starting from which the numbers will be aligned
   * to the left in the toolbar buttons, instead of centered.
   */
  private static final int MAX_SINGLE_DIGIT_NUMBER = 9;

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
  private static final int STASH_DECORATION_DISPLACEMENT = 10;
  
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
  private final SimpleDateFormat commitDateFormat = new SimpleDateFormat(UIUtil.DATE_FORMAT_WITH_COMMA_PATTERN);

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(ToolbarPanel.class);

  /**
   * Toolbar in which the button will be placed
   */
  private JToolBar gitToolbar;

  /**
   * Button for push
   */
  private ToolbarButton pushButton;
  
  /**
   * Button for show branches.
   */
  private ToolbarButton showBranchesButton;

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
   * Button for history
   */
  private ToolbarButton historyButton;

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
  private SplitMenuButton branchSelectButton;

  /**
   * List of branches.
   */
  private final List<String> branches = new ArrayList<>();

  /**
   * The ID of the commit on which a detached HEAD is set.
   */
  private String detachedHeadId;
  
  /**
   * <code>true</code> if a repository is selected.
   */
  private boolean isRepoSelected;
  
  /**
   * The git actions manager.
   */
  private final GitActionsManager gitActionsManager;
  
  /**
   * Used to show buttons in WEST not CENTER when branches widget is not visible.
   */
  private JPanel emptyPanel; 



 /**
   * Constructor.
   * 
   * @param gitController     Git controller.
   * @param gitActionsManager The git actions manager.
   * @param refreshSupport    The refresh support.
   */
  public ToolbarPanel(
	  GitController     gitController,
	  GitActionsManager gitActionsManager,
      GitRefreshSupport refreshSupport) {
	  
	this.gitActionsManager = gitActionsManager;
    this.refreshSupport    = refreshSupport;
    
    gitActionsManager.addRefreshable(this);
    createGUI();
    
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.DELETE_BRANCH ||
            operation == GitOperation.CREATE_BRANCH
            ) {
          refresh();
        }
      }
    });
  }

  
  /**
   * Sets the panel layout and creates all the buttons with their functionality
   * making them visible
   * 
   */
  public void createGUI() {
    gitToolbar = new JToolBar();
    gitToolbar.setOpaque(false);
    gitToolbar.setFloatable(false);
    this.setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weighty = 0;
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 0, 0);

    addPushAndPullButtons();
    addShowBranchesButton();
    addStashButton();
    addHistoryButton();
    addSettingsButton();
    this.add(gitToolbar, gbc);
    
    emptyPanel = new JPanel();
    gbc.weightx = 1;
    gbc.gridx++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    this.add(emptyPanel, gbc);
    
    createBranchSelectButton();
    gbc.gridx++;
    this.add(branchSelectButton, gbc);

  }


  /**
   * Updates the presented information, like the Pull-behind, Pushes-ahead
   * and branch status.
   */
  @Override
  public void refresh() {

    this.pullsBehind = GIT_ACCESS.getPullsBehind();
    try {
      this.pushesAhead = GIT_ACCESS.getPushesAhead();
    } catch (RepoNotInitializedException e) {
      this.pushesAhead = -1;
      LOGGER.debug(e, e);
    }
    
    Repository repo = null;
    try {
      repo = GIT_ACCESS.getRepository();
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e, e);
    }
    
    isRepoSelected = repo != null;

    pullMenuButton.setEnabled(gitActionsManager.getPullMergeAction().isEnabled() || 
    		gitActionsManager.getPullRebaseAction().isEnabled());
    
    stashButton.setEnabled(gitActionsManager.getListStashesAction().isEnabled() || 
    		gitActionsManager.getStashChangesAction().isEnabled());
    
    branchSelectButton.setVisible(repo != null);
    
    emptyPanel.setVisible(!branchSelectButton.isVisible());
    
    updateBranches();

    SwingUtilities.invokeLater(() -> {
      pullMenuButton.repaint();
      pushButton.repaint();
      stashButton.repaint();
    });

    BranchInfo branchInfo = GIT_ACCESS.getBranchInfo();
    String currentBranchName = branchInfo.getBranchName();
    int selectedBranchIndex = getBranchIndex(currentBranchName);
    if (branchInfo.isDetached()) {
      SwingUtilities.invokeLater(() -> {
        pushButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
        pullMenuButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
      });
      detachedHeadId = currentBranchName;

      String tooltipText = TRANSLATOR.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
              + " " + currentBranchName;
      if (repo != null && repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
        tooltipText += "<br>" + TRANSLATOR.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
      }
      tooltipText = TextFormatUtil.toHTML(tooltipText);
      String finalText = tooltipText;

      if(selectedBranchIndex >= 0) {
        // Although the pop-up actions have tooltip, on createBranchMenuItem(), at this point 
        // we can have push-ahead and pull-behind information for the active branch.
        SwingUtilities.invokeLater(() -> branchSelectButton.getItem(selectedBranchIndex).setToolTipText(finalText));
      }
    } else {
      detachedHeadId = null;
      String branchTooltip = null;
      
      if (currentBranchName != null && !currentBranchName.isEmpty()) {
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
        branchSelectButton.setToolTipText(updateBranchSelectionToolTip(currentBranchName, isAnUpstreamBranchDefinedInConfig,
                existsRemoteBranchForUpstreamDefinedInConfig, upstreamBranchFromConfig));

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

          if (pushesAhead == 0) {
            commitsAheadMessage = TRANSLATOR.getTranslation(Tags.NOTHING_TO_PUSH);
          } else if (pushesAhead == 1) {
            commitsAheadMessage = TRANSLATOR.getTranslation(Tags.ONE_COMMIT_AHEAD);
          } else {
            commitsAheadMessage = MessageFormat.format(TRANSLATOR.getTranslation(Tags.COMMITS_AHEAD), pushesAhead);
          }
        }

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

        branchTooltip = getBranchTooltip(pullsBehind, pushesAhead, currentBranchName);
        
        SwingUtilities.invokeLater(() -> branchSelectButton.setText(currentBranchName));
      }
      
      String branchTooltipFinal = branchTooltip;
     
      if(selectedBranchIndex >= 0) {
        // Although the pop-up actions have tooltip, on createBranchMenuItem(), at this point 
        // we can have push-ahead and pull-behind information for the active branch.
        SwingUtilities.invokeLater(() -> branchSelectButton.setToolTipText(branchTooltipFinal));
      }

    }

    if(selectedBranchIndex >= 0) {
      SwingUtilities.invokeLater(() -> branchSelectButton.getItem(selectedBranchIndex).setSelected(true));
      
    }
    

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


  /**
   * Enables/Disables the buttons.
   *
   * @param enabled <code>true</code> to enable the buttons. <code>false</code> to disable them.
   */
  public void updateButtonState(boolean enabled) {
    pushButton.setEnabled(enabled);
    pullMenuButton.setEnabled(enabled);
    showBranchesButton.setEnabled(enabled);
    stashButton.setEnabled(enabled);
    historyButton.setEnabled(enabled);
    settingsMenuButton.setEnabled(enabled);
  }



  // ==========  PUSH AND PULL  ==========
  
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
    
    pullMenuButton.setEnabled(gitActionsManager.getPullMergeAction().isEnabled() 
    		|| gitActionsManager.getPullRebaseAction().isEnabled());
    
  }
  
  
  /**
   * Create the "Push" button.
   * 
   * @return the "Push" button.
   */
  private ToolbarButton createPushButton() {
    return new ToolbarButton(gitActionsManager.getPushAction(), false) { // NOSONAR (java:S110)

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
        if (pushesAhead > MAX_SINGLE_DIGIT_NUMBER) {
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
        if (pullsBehind > MAX_SINGLE_DIGIT_NUMBER) {
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
    Ref remoteBranchWithLocalBranchName = RepoUtil.getRemoteBranch(currentBranchName);
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
    Ref remoteBranchWithLocalBranchName = RepoUtil.getRemoteBranch(currentBranchName);
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
   * @return The push button.
   */
  public ToolbarButton getPushButton() {
    return pushButton;
  }


  /**
   * @return The pull button.
   */
  public SplitMenuButton getPullMenuButton() {
    return pullMenuButton;
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
    final JRadioButtonMenuItem pullMergeMenuItem = new JRadioButtonMenuItem(
    		gitActionsManager.getPullMergeAction());
    pullMergeMenuItem.addActionListener(radioMenuItemActionListener);
    splitMenuButton.add(pullMergeMenuItem);
    pullActionsGroup.add(pullMergeMenuItem);

    // Pull (rebase)
    final JRadioButtonMenuItem pullRebaseMenuItem = new JRadioButtonMenuItem(
    		gitActionsManager.getPullRebaseAction());
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

  

  // ========== BRANCHES ==========
  
  /**
   * Add show branches button to toolbar.
   */
  private void addShowBranchesButton() {
    
    showBranchesButton = new ToolbarButton(gitActionsManager.getShowBranchesAction(), false);
    showBranchesButton.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
    
    setDefaultToolbarButtonWidth(showBranchesButton);
    
    final Dimension d = showBranchesButton.getPreferredSize();
    showBranchesButton.setPreferredSize(d);
    showBranchesButton.setMinimumSize(d);
    showBranchesButton.setMaximumSize(d);

    showBranchesButton.setToolTipText(TRANSLATOR.getTranslation(Tags.BRANCH_MANAGER_BUTTON_TOOL_TIP));
    
    gitToolbar.add(showBranchesButton);

  }
  

  /**
   * Adds to the tool bar a button for selecting branches. When clicked, a new
   * dialog appears that shows all the branches for the current repository and
   * allows the user to select one of them.
   *
   * @param branchManagementViewPresenter Branches presenter.
   */
  private void createBranchSelectButton() {

    branchSelectButton = new SplitMenuButton( // NOSONAR (java:S110)
            null,
            null,
            true,
            false,
            true,
            false) {

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintBranchesComponent(g);
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
      private void paintBranchesComponent(Graphics g) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, PUSH_PULL_COUNTERS_FONT_SIZE));
        FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
        int stringWidth = fontMetrics.stringWidth("");
        g.setColor(getForeground());
        g.drawString("",
                // X
                getWidth() - stringWidth - DECORATION_DISPLACEMENT,
                // Y
                fontMetrics.getHeight() - fontMetrics.getDescent() - fontMetrics.getLeading());
      }

    };

    Dimension d = pushButton.getPreferredSize();
    branchSelectButton.setPreferredSize(d);
    branchSelectButton.setMinimumSize(d);
    branchSelectButton.setMaximumSize(d);

    branchSelectButton.setText(GIT_ACCESS.getBranchInfo().getBranchName());
      
    branchSelectButton.setVisible(gitActionsManager.getShowBranchesAction().isEnabled());
    emptyPanel.setVisible(!branchSelectButton.isVisible());
    
  }


  /**
   * Update the branches.
   */
  private void updateBranches() {
    branches.clear();
    branches.addAll(getBranches());
    
    if (detachedHeadId != null) {
      branches.add(detachedHeadId);
    }
    
    boolean isShowing = branchSelectButton.getPopupMenu().isShowing();
    branchSelectButton.getPopupMenu().setVisible(false);
    
    while(branchSelectButton.getItemCount() != 0) {
      branchSelectButton.remove(branchSelectButton.getItem(0));
    }
    
    ButtonGroup branchesActionsGroup = new ButtonGroup();
   
    for(String branch: branches) {
      JMenuItem branchesMenuItem = createBranchMenuItem(branch); 
      branchSelectButton.add(branchesMenuItem);
      branchesActionsGroup.add(branchesMenuItem);
    }

    branchSelectButton.revalidate();

    if(isShowing) {
      branchSelectButton.getPopupMenu().setVisible(true);
    }
    
    branchSelectButton.setEnabled(isRepoSelected);
    
  }


  /**
   * Create a menu item for the given branch.
   * 
   * @param branch the branch. 
   * 
   * @return the created menu item.
   */
  private JMenuItem createBranchMenuItem(String branch) {
    
    AbstractAction branchAction = new AbstractAction(branch) {
      @Override
      public void actionPerformed(ActionEvent e) {
        String branchName = branch;
        BranchInfo currentBranchInfo = GIT_ACCESS.getBranchInfo();
        String currentBranchName = currentBranchInfo.getBranchName();
        if (branchName.equals(currentBranchName)) {
          return;
        }

        RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
        if (RepoUtil.isNonConflictualRepoWithUncommittedChanges(repoState)) {
          SwingUtilities.invokeLater(() -> {
            BranchSwitchConfirmationDialog dialog = new BranchSwitchConfirmationDialog(branchName);

            dialog.setVisible(true);

            int answer = dialog.getResult();

            if (answer == OKOtherAndCancelDialog.RESULT_OTHER) {
              tryCheckingOutBranch(currentBranchInfo, branchName);
            } else if (answer == OKOtherAndCancelDialog.RESULT_OK) {
              boolean wasStashCreated = StashUtil.stashChanges();
              if (wasStashCreated) {
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

    };

    JRadioButtonMenuItem branchesMenuItem = new JRadioButtonMenuItem(branchAction) {
      
      @Override
      public JToolTip createToolTip() {
        return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip); 
      }
      
    };
    
    branchesMenuItem.setToolTipText(getBranchTooltip(0, 0, branch));
    
    return branchesMenuItem;
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

    String branchTooltip = getBranchTooltip(
        pullsBehind,
        pushesAhead,
        currentBranchName,
        upstreamBranchFromConfig,
        isAnUpstreamBranchDefinedInConfig,
        remoteBranchRefForUpstreamFromConfig != null);

    return TextFormatUtil.toHTML(branchTooltip);
  }

  /**
   * Get branch tooltip.
   * 
   * @param pullsBehind                                   Number of pulls/commits behind.
   * @param pushesAhead                                   Number of pushes/commits ahead.
   * @param currentBranchName                             The name of the current branch.
   * @param upstreamBranchFromConfig                      The upstream branch.
   * @param isAnUpstreamBranchDefinedInConfig             <code>true</code> an upstream branch is defined in config.
   * @param existsRemoteBranchForUpstreamDefinedInConfig  <code>true</code> if a remote branch for upstream is defined in config.
   * 
   * @return The tooltip message.
   */
  private String getBranchTooltip(
      int pullsBehind,
      int pushesAhead,
      String currentBranchName,
      String upstreamBranchFromConfig,
      boolean isAnUpstreamBranchDefinedInConfig,
      boolean existsRemoteBranchForUpstreamDefinedInConfig) {
    String branchTooltip = TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH)
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
    
    return branchTooltip;
  }


  /**
   * The action performed for this Abstract Action
   *
   * @param oldBranchInfo Old branch info.
   * @param newBranchName New branch name.
   */
  private void tryCheckingOutBranch(BranchInfo oldBranchInfo, String newBranchName) {
    RepositoryState repoState = RepoUtil.getRepoState().orElse(null);
    if (oldBranchInfo.isDetached() && !RepoUtil.isRepoRebasing(repoState)) {
      detachedHeadId = null;
      branches.remove(oldBranchInfo.getBranchName());
      int noBranches = branchSelectButton.getItemCount();
      JMenuItem searchedBranch = null;
      for(int i = 0; i < noBranches; i++) {
        if(branchSelectButton.getItem(i).getText().equals(oldBranchInfo.getBranchName())) {
          searchedBranch = branchSelectButton.getItem(i);
          break;
        }
      }
      if(searchedBranch != null) {
        branchSelectButton.remove(searchedBranch);
      }

    }

    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        GIT_ACCESS.setBranch(newBranchName);
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
    String currentBranchName = GIT_ACCESS.getBranchInfo().getBranchName();
    if(currentBranchName != null) {
      int itemCount = branchSelectButton.getItemCount();
      for (int i = 0; i < itemCount; i++) {
        String branch = branchSelectButton.getItem(i).getText();
        if (currentBranchName.equals(branch)) {
          branchSelectButton.getItem(i).setSelected(true);
          break;
        }
      }
    }

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
   * @param branchName  The searched branch.
   *
   * @return The index of the branch or -1 if the branch is not found.
   */
  private int getBranchIndex(String branchName) {

    int index = -1;

    if(branchName != null) {
      for(int i = 0; i < branchSelectButton.getItemCount(); i++) {
        if(branchName.equals(branchSelectButton.getItem(i).getText())) {
          index = i;
          break;
        }
      }
    }

    return index;
  }


  /**
   * Compute the new toolTip text for branch selection button.
   *
   * @param currentBranchName                             The current branch name.
   * @param isAnUpstreamBranchDefinedInConfig             <code>true</code> if is am upstream branch defined in config.
   * @param existsRemoteBranchForUpstreamDefinedInConfig  <code>true</code> if exists remote branch for upstream defined in config.
   * @param upstreamBranchFromConfig                      The upstream branch defined in config.
   *
   * @return The tooltip text.
   */
  private String updateBranchSelectionToolTip(
      String currentBranchName,
      boolean isAnUpstreamBranchDefinedInConfig ,
      boolean existsRemoteBranchForUpstreamDefinedInConfig,
      String upstreamBranchFromConfig) {

    String branchTooltip = getBranchTooltip(
        pullsBehind,
        pushesAhead,
        currentBranchName,
        upstreamBranchFromConfig,
        isAnUpstreamBranchDefinedInConfig,
        existsRemoteBranchForUpstreamDefinedInConfig);
    
    if (isAnUpstreamBranchDefinedInConfig && existsRemoteBranchForUpstreamDefinedInConfig) {
      branchTooltip += "<br>";
    }

    branchTooltip += "<br>" + TRANSLATOR.getTranslation(Tags.BRANCH_MANAGER_BUTTON_TOOL_TIP);

    return TextFormatUtil.toHTML(branchTooltip);
  }
  
  
  /**
   * @return The branches button.
   */
  public SplitMenuButton getBranchSelectButton() {
    return branchSelectButton;
  }

  

  // ========== STASH ==========

  /**
   * Adds to the tool bar the Stash Button.
   */
  private void addStashButton() {
    stashButton = createStashButton();
    Dimension d = stashButton.getPreferredSize();
    d.width += PULL_BUTTON_EXTRA_WIDTH;
    stashButton.setPreferredSize(d);
    stashButton.setMinimumSize(d);
    stashButton.setMaximumSize(d);
    gitToolbar.add(stashButton);
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
       * Paint the number of stashes.
       *
       * @param g Graphics.
       */
      private void paintStashes(Graphics g) {
        String noOfStashesString = "";
        
        if (noOfStashes > 0) {
          noOfStashesString = Integer.toString(noOfStashes);
        }
        if (noOfStashes > MAX_SINGLE_DIGIT_NUMBER) {
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
                stashButton.getWidth() - stringWidth - STASH_DECORATION_DISPLACEMENT,
                // Y
                stashButton.getHeight() - fontMetrics.getDescent());
      }
    };

    stashLocalButton.setToolTipText(TRANSLATOR.getTranslation(Tags.STASH));

    stashLocalButton.addActionToMenu(gitActionsManager.getStashChangesAction(), false);
    stashLocalButton.addActionToMenu(gitActionsManager.getListStashesAction(),  false);
    
    stashLocalButton.setEnabled(gitActionsManager.getStashChangesAction().isEnabled() 
    		|| gitActionsManager.getListStashesAction().isEnabled());

    return stashLocalButton;
  }


  /**
   * @return the stash button.
   */
  public SplitMenuButton getStashButton() {
    return stashButton;
  }



  // ========== HISTORY ==========

  /**
   * @param historyController History interface.
   */
  private void addHistoryButton() {
    
    historyButton = new ToolbarButton(gitActionsManager.getShowHistoryAction(), false);
    historyButton.setIcon(Icons.getIcon(Icons.GIT_HISTORY));
    historyButton.setToolTipText(TRANSLATOR.getTranslation(Tags.SHOW_CURRENT_BRANCH_HISTORY));
    setDefaultToolbarButtonWidth(historyButton);

    gitToolbar.add(historyButton);

  }
  
  

  // ========== SETTINGS ==========

  /**
   * Add the settings button.
   */
  private void addSettingsButton() {
    settingsMenuButton = SettingsMenuBuilder.build(refreshSupport);

    gitToolbar.add(settingsMenuButton);
  }


  /**
   * @return the setting button.
   */
  public SplitMenuButton getSettingsMenuButton() {
    return settingsMenuButton;
  }
  
  /**
   * @return The current Git Actions Manager.
   */
  public GitActionsManager getGitActionsManager() {
	return gitActionsManager;
  }


}
