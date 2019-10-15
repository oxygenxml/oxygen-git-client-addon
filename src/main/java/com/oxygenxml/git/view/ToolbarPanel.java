package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepoNotInitializedException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitRefreshSupport;
import com.oxygenxml.git.view.dialog.BranchSelectDialog;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.LoginDialog;
import com.oxygenxml.git.view.dialog.PassphraseDialog;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
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
          pushPullController.pull(pullType);
          pullsBehind = 0;
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
	 * Status presenting on which branch the user is and whether the repository is
	 * up to date or not
	 */
	private JLabel remoteAndBranchInfoLabel;

	/**
	 * Used to execute the push and pull commands
	 */
	private PushPullController pushPullController;

	/**
	 * Button for push
	 */
	private ToolbarButton pushButton;

	/**
	 * Button with menu for pull (with merge or rebase).
	 */
	private SplitMenuButton pullMenuButton;

	/**
	 * Button for selecting the submodules
	 */
	private ToolbarButton submoduleSelectButton;

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
	 * Image utilities.
	 */
	private ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();

	/**
	 * Branch selection button.
	 */
  private ToolbarButton branchSelectButton;

  /**
   * Constructor.
   * @param pushPullController Push/pull controller.
   * @param refreshSupport     The refresh support.
   */
	public ToolbarPanel(
	    PushPullController pushPullController, 
	    GitRefreshSupport refreshSupport) {
	  this.pushPullController = pushPullController;
	  this.remoteAndBranchInfoLabel = new JLabel();
	  this.refreshSupport = refreshSupport;

	  createGUI();

	  GitAccess.getInstance().addGitListener(new GitEventAdapter() {
	    @Override
      public void repositoryChanged() {
	      // Repository changed. Update the toolbar buttons.
	      if (gitRepoHasSubmodules()) {
	        submoduleSelectButton.setEnabled(true);
	      } else {
	        submoduleSelectButton.setEnabled(false);
	      }

	      // Update the toobars.
	      // calculate how many pushes ahead and pulls behind the current
	      // selected working copy is from the base. It is on thread because
	      // the fetch command takes a longer time
	      // TODO This might stay well in the Refresh support... When a new repository is 
	      // selected this is triggered.
	      // TODO Maybe the change of repository should triggered a fetch and a notification should
	      // be fired when the fetch information is brought. It might make sense to use a coalescing for the fetch.
	      new Thread(new Runnable() {
	        @Override
	        public void run() {
	          fetch(true);

	          // After the fetch is done, update the toolbar icons.
	          updateStatus();
	        }
	      }).start();
	    }
	    
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        GitCommand cmd = changeEvent.getCommand();
        if (cmd == GitCommand.ABORT_REBASE || cmd == GitCommand.CONTINUE_REBASE) {
          updateStatus();
        }
      }
	  });
	}
	
	/**
	 * @return <code>true</code> if we have submodules.
	 */
	boolean gitRepoHasSubmodules() {
    return !GitAccess.getInstance().getSubmodules().isEmpty();
  }
	
	/**
	 * Fetch.
	 */
  private void fetch(boolean firstRun) {
    try {
      GitAccess.getInstance().fetch();
    } catch (SSHPassphraseRequiredException e) {
      String message = null;
      if (firstRun) {
        message = translator.getTranslation(Tags.ENTER_SSH_PASSPHRASE);
      } else {
        message = translator.getTranslation(Tags.PREVIOUS_PASSPHRASE_INVALID) 
            + " " 
            + translator.getTranslation(Tags.ENTER_SSH_PASSPHRASE);
      }

      String passphrase = new PassphraseDialog(message).getPassphrase();
      if(passphrase != null){
        // A new pass phase was given. Try again.
        fetch(false);
      }
    } catch (PrivateRepositoryException e) {
      String loginMessage = null;
      if (firstRun) {
        loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_PRIVATE_REPOSITORY_MESSAGE);
      } else {
        UserCredentials gitCredentials = OptionsManager.getInstance().getGitCredentials(GitAccess.getInstance().getHostName());
        loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
            + gitCredentials.getUsername();
      }

      UserCredentials userCredentials = new LoginDialog(
          GitAccess.getInstance().getHostName(), 
          loginMessage).getUserCredentials();
      if (userCredentials != null) {
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
    
  }
  
  public ToolbarButton getSubmoduleSelectButton() {
    return submoduleSelectButton;
  }

	/**
	 * Sets the panel layout and creates all the buttons with their functionality
	 * making them visible
	 */
	public void createGUI() {
		gitToolbar = new JToolBar();
		gitToolbar.setOpaque(false);
		gitToolbar.setFloatable(false);
		this.setLayout(new GridBagLayout());
		try {
      this.pushesAhead = GitAccess.getInstance().getPushesAhead();
    } catch (RepoNotInitializedException e) {
      this.pushesAhead = -1;
      logger.debug(e, e);
    }
		this.pullsBehind = GitAccess.getInstance().getPullsBehind();

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
		addBranchSelectButton();
		addSubmoduleSelectButton();
		if (gitRepoHasSubmodules()) {
			submoduleSelectButton.setEnabled(true);
		} else {
			submoduleSelectButton.setEnabled(false);
		}
		this.add(gitToolbar, gbc);

		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		updateStatus();
		this.add(remoteAndBranchInfoLabel, gbc);

		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.TOOLBAR_PANEL_HEIGHT));
	}

	private void addCloneRepositoryButton() {
		Action cloneRepositoryAction = new AbstractAction() {
		  /**
		   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		   */
			@Override
      public void actionPerformed(ActionEvent e) {
				new CloneRepositoryDialog();
			}
		};

		cloneRepositoryButton = new ToolbarButton(cloneRepositoryAction, false);
		URL resource = getClass().getResource(ImageConstants.GIT_CLONE_REPOSITORY_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  cloneRepositoryButton.setIcon(icon);
		}
		cloneRepositoryButton.setToolTipText(translator.getTranslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP));
		setDefaultToolbarButtonWidth(cloneRepositoryButton);

		gitToolbar.add(cloneRepositoryButton);
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
		URL resource = getClass().getResource(ImageConstants.GIT_SUBMODULE_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  submoduleSelectButton.setIcon(icon);
		}
		submoduleSelectButton.setToolTipText(translator.getTranslation(Tags.SELECT_SUBMODULE_BUTTON_TOOLTIP));
		setDefaultToolbarButtonWidth(submoduleSelectButton);

		gitToolbar.add(submoduleSelectButton);
	}

	/**
	 * Adds to the tool bar a button for selecting branches. When clicked, a new
	 * dialog appears that shows all the branches for the current repository and
	 * allows the user to select one of them
	 */
	private void addBranchSelectButton() {
		Action branchSelectAction = new AbstractAction() {

			@Override
      public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						new BranchSelectDialog(refreshSupport);
					}
				} catch (NoRepositorySelected e1) {
          if(logger.isDebugEnabled()) {
            logger.debug(e1, e1);
          }
				}
			}
		};
		branchSelectButton = new ToolbarButton(branchSelectAction, false);
		URL resource = getClass().getResource(ImageConstants.GIT_BRANCH_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  branchSelectButton.setIcon(icon);
		}
		branchSelectButton.setToolTipText(translator.getTranslation(Tags.CHANGE_BRANCH_BUTTON_TOOLTIP));
		setDefaultToolbarButtonWidth(branchSelectButton);

		gitToolbar.add(branchSelectButton);
	}

	/**
	 * Updates the presented information, like the Pull-behind, Pushes-ahead
	 * and branch status.
	 */
	public void updateStatus() {
    this.pullsBehind = GitAccess.getInstance().getPullsBehind();
    pullMenuButton.repaint();
    
    try {
      this.pushesAhead = GitAccess.getInstance().getPushesAhead();
    } catch (RepoNotInitializedException e) {
      this.pushesAhead = -1;
      logger.debug(e, e);
    }
    pushButton.repaint();
    
		BranchInfo branchInfo = GitAccess.getInstance().getBranchInfo();
		String branchInfoText = "";
		if (branchInfo.isDetached()) {
		  Repository repo = null;
		  try {
		    repo = GitAccess.getInstance().getRepository();
		  } catch (NoRepositorySelected e) {
		    logger.debug(e, e);
		  }
      branchInfoText += "<html><b>" + branchInfo.getShortBranchName() + "</b></html>";
		  String tooltipText = "<html>"
		      + translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD)
		      + " "
		      + branchInfo.getBranchName();
		  if (repo.getRepositoryState() == RepositoryState.REBASING_MERGE) {
		    tooltipText += "<br>" + translator.getTranslation(Tags.REBASE_IN_PROGRESS) + ".";
		  }
		  tooltipText += "</html>";
		  remoteAndBranchInfoLabel.setToolTipText(tooltipText);
		  pushButton.setToolTipText(translator.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
		  pullMenuButton.setToolTipText(translator.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
		} else {
			String currentBranch = branchInfo.getBranchName();
			branchInfoText = "<html><b>" + currentBranch + "</b></html>";
			String remoteAndBranchTooltipMessage = null;
			if (!"".equals(currentBranch)) {
			  String remoteName = null;
        try {
          remoteName = GitAccess.getInstance().getRemote(currentBranch);
        } catch (NoRepositorySelected e) {
          logger.debug(e, e);
        }
				String remoteAndBranchInfo = (remoteName == null ? "" : remoteName + "/") + currentBranch;
        remoteAndBranchTooltipMessage = "<html>"
				    + (remoteName == null ? "" : translator.getTranslation(Tags.REMOTE) + "/")
				    + translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_BRANCH).toLowerCase()
				    + " <b>"
				    + remoteAndBranchInfo
				    + "</b> - ";
				if (pullsBehind == 0) {
				  remoteAndBranchTooltipMessage += translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE);
				} else if (pullsBehind == 1) {
				  remoteAndBranchTooltipMessage += pullsBehind + " "
				      + translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_SINGLE_COMMIT);
				} else {
				  remoteAndBranchTooltipMessage += pullsBehind + " "
							+ translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_MULTIPLE_COMMITS);
				}
				remoteAndBranchTooltipMessage += "</html>";
				
				// Push tooltip
				pushButton.setToolTipText(
            MessageFormat.format(
                translator.getTranslation(Tags.PUSH_TO),
                remoteAndBranchInfo));
				
				// Pull tooltip
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
        pullMenuButton.setToolTipText(
            MessageFormat.format(
                translator.getTranslation(pullFromTag),
                remoteAndBranchInfo));
			}
			remoteAndBranchInfoLabel.setToolTipText(remoteAndBranchTooltipMessage);
		}
		
		remoteAndBranchInfoLabel.setText(branchInfoText);
	}

	/**
	 * Adds to the tool bar the Push and Pull Buttons
	 */
	private void addPushAndPullButtons() {
		// PUSH
		pushButton = createPushButton();
		URL resource = getClass().getResource(ImageConstants.GIT_PUSH_ICON);
		ImageIcon icon = null;
		if (resource != null) {
		  icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  pushButton.setIcon(icon);
		}
		setDefaultToolbarButtonWidth(pushButton);

		// PULL
		pullMenuButton = createPullButton();
    Dimension d = pullMenuButton.getPreferredSize();
    d.width = 42;
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
	  SplitMenuButton pullSplitMenuButton = new SplitMenuButton(
	      null,
	      getPullIcon(),
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
        g.setFont(g.getFont().deriveFont(Font.BOLD, 8.5f));
        FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
        int stringWidth = fontMetrics.stringWidth(noOfPullsBehindString);
        g.setColor(getForeground());
        g.drawString(noOfPullsBehindString,
            // X TODO: use scaling factor for that magic number (13)
            getWidth() - stringWidth - 13,
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
	 * Get pull icon.
	 * 
	 * @return The pull icon or <code>null</code>.
	 */
  private ImageIcon getPullIcon() {
    URL resource = getClass().getResource(ImageConstants.GIT_PULL_ICON);
	  ImageIcon icon = null;
	  if (resource != null) {
	    icon = (ImageIcon) imageUtilities.loadIcon(resource);
	  }
    return icon;
  }
	
	/**
	 * Create the "Push" button.
	 * 
	 * @return the "Push" button.
	 */
  private ToolbarButton createPushButton() {
    return new ToolbarButton(createPushAction(), false) {
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
				g.setFont(g.getFont().deriveFont(Font.BOLD, 8.5f));
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

						pushPullController.push();
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
		d.width = 30;
		button.setPreferredSize(d);
		button.setMinimumSize(d);
		button.setMaximumSize(d);
	}
}
