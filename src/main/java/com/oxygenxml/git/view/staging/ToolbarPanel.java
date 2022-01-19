package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;

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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
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
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.CommitsAheadAndBehind;
import com.oxygenxml.git.view.util.UIUtil;

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
   * The git actions manager.
   */
  private final GitActionsManager gitActionsManager;

 /**
   * Constructor.
   * 
   * @param gitController     Git controller.
   * @param gitActionsManager The git actions manager.
   * 
   */
  public ToolbarPanel(GitController gitController, GitActionsManager gitActionsManager) {
    this.gitActionsManager = gitActionsManager;
    createGUI();
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
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0;
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 0, 0);

    addPushAndPullButtons();
    addStashButton();
    addHistoryButton();
    addShowBranchesButton();
    
    this.add(gitToolbar, gbc);
  }


  /**
   * Updates the presented information, like the Pull-behind, Pushes-ahead
   * and branch status.
   */
  public void updateButtonsStates() {

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
    
    
    /* for @pushButton, @historyButton, @showBranchesButton ---> 
     * it is necessary to refresh, even if it has a single action, 
     * because there are other code sequences that directly change their state by calling the setEnabled(false) method
     */
    pushButton.setEnabled(gitActionsManager.getPushAction().isEnabled());
    pullMenuButton.setEnabled(isPullButtonEnabled());
    stashButton.setEnabled(isStashButtonEnabled());
    historyButton.setEnabled(gitActionsManager.getShowHistoryAction().isEnabled());
    showBranchesButton.setEnabled(gitActionsManager.getShowBranchesAction().isEnabled());
    
    SwingUtilities.invokeLater(() -> {
      pullMenuButton.repaint();
      pushButton.repaint();
      stashButton.repaint();
    });

    BranchInfo branchInfo = GIT_ACCESS.getBranchInfo();
    String currentBranchName = branchInfo.getBranchName();
    if (branchInfo.isDetached()) {
      SwingUtilities.invokeLater(() -> {
        pushButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
        pullMenuButton.setToolTipText(TRANSLATOR.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
      });

      String tooltipText = TRANSLATOR.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
              + " " + currentBranchName;
      if (repo != null && repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
        tooltipText += "<br>" + TRANSLATOR.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
      }
      tooltipText = TextFormatUtil.toHTML(tooltipText);

    } else {
      
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
      }
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
   * @param force <code>true</code> to force enable/disable the buttons even actions state may differ.
   */
  public void setButtonsEnabledState(boolean enabled, boolean force) {
    pushButton.setEnabled(enabled);
    pullMenuButton.setEnabled(force? enabled : isPullButtonEnabled());
    showBranchesButton.setEnabled(enabled);
    stashButton.setEnabled(force ? enabled : isStashButtonEnabled());
    historyButton.setEnabled(enabled);
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
    
    pullMenuButton.setEnabled(isPullButtonEnabled());
    
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
    
    final Dimension d = showBranchesButton.getPreferredSize();
    showBranchesButton.setPreferredSize(d);
    showBranchesButton.setMinimumSize(d);
    showBranchesButton.setMaximumSize(d);

    showBranchesButton.setToolTipText(TRANSLATOR.getTranslation(Tags.BRANCH_MANAGER_BUTTON_TOOL_TIP));
    
    gitToolbar.add(showBranchesButton);

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
    stashLocalButton.setEnabled(isStashButtonEnabled());

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
  
  /**
   * @return <code>true</code> is the pull button is enabled. 
   * Any of the inner actions of the button are enabled. 
   */
  private boolean isPullButtonEnabled() {
    return gitActionsManager.getPullMergeAction().isEnabled() || 
        gitActionsManager.getPullRebaseAction().isEnabled();
  }
  
  /**
   * @return <code>true</code> if the stash button is enabled.
   * Any of the inner actions of the button are enabled.
   */
  private boolean isStashButtonEnabled() {
    return gitActionsManager.getListStashesAction().isEnabled() || 
        gitActionsManager.getStashChangesAction().isEnabled();
  }
}
