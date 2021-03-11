package com.oxygenxml.git.view;

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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
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
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.branches.BranchManagementViewPresenter;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.LoginDialog;
import com.oxygenxml.git.view.dialog.PassphraseDialog;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.HistoryController;

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
public class ToolbarPanel extends JPanel {
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
   * Pull action.
   */
  private final class PullAction extends AbstractAction {
    
    private static final String PULL_TYPE_ACTION_PROP = "pullType";
    
    private PullType pullType;

    public PullAction(String name, PullType pullType) {
      super(name);
      this.pullType = pullType;
      putValue(PULL_TYPE_ACTION_PROP, pullType);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        if (GitAccess.getInstance().getRepository() != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Pull action invoked");
          }
          gitController.pull(pullType);
          OptionsManager.getInstance().saveDefaultPullType(pullType);
        }
      } catch (NoRepositorySelected e1) {
        if(logger.isDebugEnabled()) {
          logger.debug(e1, e1);
        }
      }
    }
  }

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(ToolbarPanel.class);

	/**
	 * Toolbar in which the button will be placed
	 */
	private JToolBar gitToolbar;

	/**
	 * SplitMenuButton for selecting the local branch.
	 */
	private SplitMenuButton branchesSplitMenuButton;
	
	/**
	 * Used to execute the push and pull commands
	 */
	private GitController gitController;

	/**
	 * Button for push
	 */
	private ToolbarButton pushButton;

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
	 * Counter for how many pulls the local copy is behind the base
	 */
	private int pullsBehind = 0;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator = Translator.getInstance();

	/**
	 * Main panel refresh
	 */
	private GitRefreshSupport refreshSupport;
	
	/**
	 * Branch selection button.
	 */
  private ToolbarButton branchSelectButton;
  
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
        message = translator.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      } else {
        message = translator.getTranslation(Tags.PREVIOUS_PASS_PHRASE_INVALID) 
            + " " 
            + translator.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
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
        loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_PRIVATE_REPOSITORY_MESSAGE);
      } else {
        loginMessage = translator.getTranslation(Tags.AUTHENTICATION_FAILED) + " ";
        CredentialsBase gitCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
        if (gitCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
          loginMessage += translator.getTranslation(Tags.CHECK_CREDENTIALS);
        } else {
          loginMessage += translator.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS);
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
        GitOperationScheduler.getInstance().schedule(() -> {
          try {
            GitAccess.getInstance().setBranch(branchName);
          } catch (CheckoutConflictException ex) {
            logger.debug(ex, ex);
            restoreCurrentBranchSelectionInMenu();
            BranchesUtil.showBranchSwitchErrorMessage();
          } catch (GitAPIException | JGitInternalException ex) {
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
    };
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
		cloneRepositoryButton.setToolTipText(translator.getTranslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP));
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
		historyButton.setToolTipText(translator.getTranslation(Tags.SHOW_CURRENT_BRANCH_HISTORY));
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
	        if(logger.isDebugEnabled()) {
	          logger.debug(e1, e1);
	        }
	      }
	    }
	  };
		submoduleSelectButton = new ToolbarButton(branchSelectAction, false);
		submoduleSelectButton.setIcon(Icons.getIcon(Icons.GIT_SUBMODULE_ICON));
		submoduleSelectButton.setToolTipText(translator.getTranslation(Tags.SELECT_SUBMODULE_BUTTON_TOOLTIP));
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
          if(logger.isDebugEnabled()) {
            logger.debug(e1, e1);
          }
				}
			}
		};
		branchSelectButton = new ToolbarButton(branchSelectAction, false);
		branchSelectButton.setIcon(Icons.getIcon(Icons.GIT_BRANCH_ICON));
		branchSelectButton.setToolTipText(translator.getTranslation(Tags.BRANCH_MANAGER_BUTTON_TOOL_TIP));
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
      logger.debug(e, e);
    }
    
    SwingUtilities.invokeLater(() -> {
      updateBranchesMenu();
      pullMenuButton.repaint();
      pushButton.repaint();
    });
    
    Repository repo = null;
    try {
      repo = gitAccess.getRepository();
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
    
		BranchInfo branchInfo = gitAccess.getBranchInfo();
		String currentBranchName = branchInfo.getBranchName();
		String branchInfoText = "";
		if (branchInfo.isDetached()) {
      branchInfoText += "<html><b>" + branchInfo.getShortBranchName() + "</b></html>";
		  String tooltipText = "<html>"
		      + translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
		      + " "
		      + currentBranchName;
		  if (repo != null && repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
		    tooltipText += "<br>" + translator.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
		  }
		  tooltipText += "</html>";
		  String finalText = tooltipText;
		  SwingUtilities.invokeLater(() -> {
		    branchesSplitMenuButton.setToolTipText(finalText);
		    pushButton.setToolTipText(translator.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
		    pullMenuButton.setToolTipText(translator.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
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
				    + translator.getTranslation(Tags.LOCAL_BRANCH) 
				    + " <b>" + currentBranchName + "</b>.<br>"
				    + translator.getTranslation(Tags.UPSTREAM_BRANCH) 
				    + " <b>" 
				    + (isAnUpstreamBranchDefinedInConfig && existsRemoteBranchForUpstreamDefinedInConfig 
				        ? upstreamBranchFromConfig 
				        : translator.getTranslation(Tags.NO_UPSTREAM_BRANCH)) 
				    + "</b>.<br>";
        
        String commitsBehindMessage = "";
        String commitsAheadMessage = "";
        if (isAnUpstreamBranchDefinedInConfig && existsRemoteBranchForUpstreamDefinedInConfig) {
          if (pullsBehind == 0) {
            commitsBehindMessage = translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE);
          } else if (pullsBehind == 1) {
            commitsBehindMessage = translator.getTranslation(Tags.ONE_COMMIT_BEHIND);
          } else {
            commitsBehindMessage = MessageFormat.format(translator.getTranslation(Tags.COMMITS_BEHIND), pullsBehind);
          }
          branchTooltip += commitsBehindMessage + "<br>";
          
          if (pushesAhead == 0) {
            commitsAheadMessage = translator.getTranslation(Tags.NOTHING_TO_PUSH);
          } else if (pushesAhead == 1) {
            commitsAheadMessage = translator.getTranslation(Tags.ONE_COMMIT_AHEAD);
          } else {
            commitsAheadMessage = MessageFormat.format(translator.getTranslation(Tags.COMMITS_AHEAD), pushesAhead);
          }
          branchTooltip += commitsAheadMessage;
        }
        
				branchTooltip += "</html>";

				// ===================== Push button tooltip =====================
				String pushButtonTooltip = "";
				if (isAnUpstreamBranchDefinedInConfig) {
				  if (existsRemoteBranchForUpstreamDefinedInConfig) {
				    // The "normal" case. The upstream branch defined in "config" exists in the remote repository.
				    pushButtonTooltip = MessageFormat.format(
				        translator.getTranslation(Tags.PUSH_TO),
				        upstreamBranchFromConfig) + ".\n";
				    pushButtonTooltip += commitsAheadMessage;
				  } else {
				    // There is an upstream branch defined in "config",
				    // but that branch does not exist in the remote repository.
				    pushButtonTooltip = MessageFormat.format(
				        translator.getTranslation(Tags.PUSH_TO_CREATE_AND_TRACK_REMOTE_BRANCH),
				        currentBranchName);
				  }
				} else {
				  Ref remoteBranchWithLocalBranchName = getRemoteBranch(currentBranchName);
				  if (remoteBranchWithLocalBranchName != null) {
				    // No upstream branch defined in "config", but there is a remote branch
				    // that has the same name as the local branch.
				    pushButtonTooltip = MessageFormat.format(
				        translator.getTranslation(Tags.PUSH_TO_TRACK_REMOTE_BRANCH),
				        currentBranchName);
				  } else {
				    // No upstream branch defined in "config" and no remote branch
				    // that has the same name as the local branch.
				    pushButtonTooltip = MessageFormat.format(
				        translator.getTranslation(Tags.PUSH_TO_CREATE_AND_TRACK_REMOTE_BRANCH),
				        currentBranchName);
				  }
				}
				String pushButtonTooltipFinal = pushButtonTooltip;
				SwingUtilities.invokeLater(() -> pushButton.setToolTipText(pushButtonTooltipFinal));

				//  ===================== Pull button tooltip =====================
				String pullButtonTooltip = "";
				final String pullFromTag = getPullFromTranslationTag();
				if (isAnUpstreamBranchDefinedInConfig) {
				  if (existsRemoteBranchForUpstreamDefinedInConfig) {
				    // The "normal" case. The upstream branch defined in "config" exists in the remote repository.
				    pullButtonTooltip = MessageFormat.format(
				        translator.getTranslation(pullFromTag),
				        Repository.shortenRefName(remoteBranchRefForUpstreamFromConfig.getName())) + ".\n";
				    pullButtonTooltip += commitsBehindMessage;
				  } else {
				    // The upstream branch defined in "config" does not exists in the remote repository.
            pullButtonTooltip = translator.getTranslation(Tags.CANNOT_PULL) + "\n" 
                + MessageFormat.format(
                    StringUtils.capitalize(translator.getTranslation(Tags.UPSTREAM_BRANCH_DOES_NOT_EXIST)),
                    upstreamBranchFromConfig);
				  }
				} else {
				  Ref remoteBranchWithLocalBranchName = getRemoteBranch(currentBranchName);
				  if (remoteBranchWithLocalBranchName != null) {
				    // No upstream defined in config, but there is a remote branch
				    // that has the same name as the local branch
				    pullButtonTooltip = MessageFormat.format(
				        translator.getTranslation(pullFromTag),
				        Repository.shortenRefName(remoteBranchWithLocalBranchName.getName())) + ".\n";
				  } else {
				    // No upstream branch defined in "config" and no remote branch
            // that has the same name as the local branch.
				    pullButtonTooltip = translator.getTranslation(Tags.CANNOT_PULL) + "\n" 
				        + MessageFormat.format(
				            StringUtils.capitalize(translator.getTranslation(Tags.NO_REMOTE_BRANCH)),
				            currentBranchName) 
				        + ".";
				  }
				}
				String pullButtonTooltipFinal = pullButtonTooltip;
				SwingUtilities.invokeLater(() -> pullMenuButton.setToolTipText(pullButtonTooltipFinal));
        
			}
			String branchTooltipFinal = branchTooltip;
			SwingUtilities.invokeLater(() ->branchesSplitMenuButton.setToolTipText(branchTooltipFinal));
		}
		String branchInfoTextFinal = branchInfoText;
		SwingUtilities.invokeLater(() ->branchesSplitMenuButton.setText(branchInfoTextFinal));
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
        translator.getTranslation(Tags.PULL_MERGE),
        PullType.MERGE_FF);
    JRadioButtonMenuItem pullMergeMenuItem = new JRadioButtonMenuItem(pullMergeAction);
    pullMergeMenuItem.addActionListener(radioMenuItemActionListener);
    splitMenuButton.add(pullMergeMenuItem);
    pullActionsGroup.add(pullMergeMenuItem);
    
    // Pull (rebase)
    PullAction pullRebaseAction = new PullAction(
        translator.getTranslation(Tags.PULL_REBASE),
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
						if (logger.isDebugEnabled()) {
							logger.debug("Push Button Clicked");
						}

						gitController.push();
						if (pullsBehind == 0) {
							pushesAhead = 0;
						}
					}
				} catch (NoRepositorySelected e1) {
				  if(logger.isDebugEnabled()) {
            logger.debug(e1, e1);
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
