package com.oxygenxml.git.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
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
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * Contains additional support buttons like push, pull, branch select, submodule
 * select
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel {

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
	private JLabel statusInformationLabel;

	/**
	 * Used to execute the push and pull commands
	 */
	private PushPullController pushPullController;

	/**
	 * Button for push
	 */
	private ToolbarButton pushButton;

	/**
	 * Button for pull
	 */
	private ToolbarButton pullButton;

	/**
	 * Button for selecting a branch
	 */
	private ToolbarButton branchSelectButton;

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
	private Translator translator;

	/**
	 * Main panel refresh
	 */
	private GitRefreshSupport refresh;

	public ToolbarPanel(
	    PushPullController pushPullController, 
	    final Translator translator, 
	    GitRefreshSupport refresh) {
		this.pushPullController = pushPullController;
		this.statusInformationLabel = new JLabel();
		this.translator = translator;
		this.refresh = refresh;
		
		createGUI();
		
		GitAccess.getInstance().addGitListener(new GitEventListener() {
      public void repositoryChanged() {
        // Repository changed. Update the toolbar buttons.
        if (GitAccess.getInstance().getSubmodules().size() > 0) {
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
                  loginMessage,
                  translator).getUserCredentials();
              if (userCredentials != null) {
                // New credentials were specified. Try again.
                fetch(false);
              }
            } catch (RepositoryUnavailableException e) {
              // Nothing we can do about it...
            }
          }

          public void run() {
            fetch(true);

            // After the fetch is done, update the toolbar icons.
            setPullsBehind(GitAccess.getInstance().getPullsBehind());
            setPushesAhead(GitAccess.getInstance().getPushesAhead());
            updateInformationLabel();
          }
        }).start();
      }
    });
	}

	public JButton getPushButton() {
		return pushButton;
	}

	public JButton getPullButton() {
		return pullButton;
	}

	public JButton getCloneRepositoryButton() {
		return cloneRepositoryButton;
	}

	public void setPullsBehind(int pullsBehind) {
		this.pullsBehind = pullsBehind;
		pullButton.repaint();
	}

	public void setPushesAhead(int pushesAhead) {
		this.pushesAhead = pushesAhead;
		pushButton.repaint();
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
		this.pushesAhead = GitAccess.getInstance().getPushesAhead();
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
		if (GitAccess.getInstance().getSubmodules().size() > 0) {
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
		updateInformationLabel();
		this.add(statusInformationLabel, gbc);

		this.setMinimumSize(new Dimension(Constants.PANEL_WIDTH, Constants.TOOLBAR_PANEL_HEIGHT));
	}

	private void addCloneRepositoryButton() {
		Action cloneRepositoryAction = new AbstractAction() {
		  /**
		   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		   */
			public void actionPerformed(ActionEvent e) {
				new CloneRepositoryDialog(translator);
			}
		};

		cloneRepositoryButton = new ToolbarButton(cloneRepositoryAction, false);
		cloneRepositoryButton.setIcon(Icons.getIcon(ImageConstants.GIT_CLONE_REPOSITORY_ICON));
		cloneRepositoryButton.setToolTipText(translator.getTranslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP));
		setCustomWidthOn(cloneRepositoryButton);

		gitToolbar.add(cloneRepositoryButton);
	}

	/**
	 * Adds to the tool bar a button for selecting submodules. When clicked, a new
	 * dialog appears that shows all the submodules for the current repository and
	 * allows the user to select one of them
	 */
	private void addSubmoduleSelectButton() {
		Action branchSelectAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						new SubmoduleSelectDialog(translator);
					}
				} catch (NoRepositorySelected e1) {
				}
			}
		};
		submoduleSelectButton = new ToolbarButton(branchSelectAction, false);
		submoduleSelectButton.setIcon(Icons.getIcon(ImageConstants.GIT_SUBMODULE_ICON));
		submoduleSelectButton.setToolTipText(translator.getTranslation(Tags.SELECT_SUBMODULE_BUTTON_TOOLTIP));
		setCustomWidthOn(submoduleSelectButton);

		gitToolbar.add(submoduleSelectButton);
	}

	/**
	 * Adds to the tool bar a button for selecting branches. When clicked, a new
	 * dialog appears that shows all the branches for the current repository and
	 * allows the user to select one of them
	 */
	private void addBranchSelectButton() {
		Action branchSelectAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						new BranchSelectDialog(refresh, translator);
					}
				} catch (NoRepositorySelected e1) {
				}
			}
		};
		branchSelectButton = new ToolbarButton(branchSelectAction, false);
		branchSelectButton.setIcon(Icons.getIcon(ImageConstants.GIT_BRANCH_ICON));
		branchSelectButton.setToolTipText(translator.getTranslation(Tags.CHANGE_BRANCH_BUTTON_TOOLTIP));
		setCustomWidthOn(branchSelectButton);

		gitToolbar.add(branchSelectButton);
	}

	public void updateInformationLabel() {
		BranchInfo branchInfo = GitAccess.getInstance().getBranchInfo();
		String message = "";
		if (branchInfo.isDetached()) {
			message += "<html>";
			message += "Commit: <b>" + branchInfo.getShortBranchName() + "</b></html>";
			statusInformationLabel
					.setToolTipText(translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD) + " "
							+ branchInfo.getBranchName());
		} else {
			String currentBranch = branchInfo.getBranchName();
			if (!"".equals(currentBranch)) {
				message += "<html>";
				message += translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_BRANCH) + "<b>" + currentBranch
						+ "</b> - ";
				if (pullsBehind == 0) {
					message += translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE);
				} else if (pullsBehind == 1) {
					message += pullsBehind + " " + translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_SINGLE_COMMIT);
				} else {
					message += pullsBehind + " "
							+ translator.getTranslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_MULTIPLE_COMMITS);
				}
				message += "</html>";
			}
			statusInformationLabel.setToolTipText("");
		}
		statusInformationLabel.setText(message);
	}

	/**
	 * Adds to the tool bar the Push and Pull Buttons
	 */
	private void addPushAndPullButtons() {

		// PUSH button
		Action pushAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Push Button Clicked");
						}

						pushPullController.execute(Command.PUSH);
						if (pullsBehind == 0) {
							pushesAhead = 0;
						}
					}
				} catch (NoRepositorySelected e1) {
				}
			}

		};
		pushButton = new ToolbarButton(pushAction, false) {

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				String str = "";
				if (pushesAhead > 0) {
					str = "" + pushesAhead;
				}
				if (pushesAhead > 9) {
					pushButton.setHorizontalAlignment(SwingConstants.LEFT);
				} else {
					pushButton.setHorizontalAlignment(SwingConstants.CENTER);
				}
				g.setFont(g.getFont().deriveFont(Font.BOLD, 8.5f));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				int stringHeight = fontMetrics.getHeight();
				g.setColor(new Color(255, 255, 255, 100));
				g.fillRect(pushButton.getWidth() - stringWidth - 1, pushButton.getHeight() - stringHeight, stringWidth,
						stringHeight);
				g.setColor(Color.BLACK);
				g.drawString(str, pushButton.getWidth() - stringWidth, pushButton.getHeight() - fontMetrics.getDescent());
			}
		};

		pushButton.setIcon(Icons.getIcon(ImageConstants.GIT_PUSH_ICON));
		pushButton.setToolTipText(translator.getTranslation(Tags.PUSH_BUTTON_TOOLTIP));
		setCustomWidthOn(pushButton);

		// PULL button
		Action pullAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Pull Button Clicked");
						}
						pushPullController.execute(Command.PULL);
						pullsBehind = 0;
					}
				} catch (NoRepositorySelected e1) {

				}
			}
		};
		pullButton = new ToolbarButton(pullAction, false) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				String str = "";
				if (pullsBehind > 0) {
					str = "" + pullsBehind;
				}
				if (pullsBehind > 9) {
					pullButton.setHorizontalAlignment(SwingConstants.LEFT);
				} else {
					pullButton.setHorizontalAlignment(SwingConstants.CENTER);
				}
				g.setFont(g.getFont().deriveFont(Font.BOLD, 8.5f));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				int stringHeight = fontMetrics.getHeight();
				g.setColor(new Color(255, 255, 255, 100));
				g.fillRect(pullButton.getWidth() - stringWidth, 0, stringWidth, stringHeight);
				g.setColor(Color.BLACK);
				g.drawString(str, pullButton.getWidth() - stringWidth,
						fontMetrics.getHeight() - fontMetrics.getDescent() - fontMetrics.getLeading());
			}

		};
		pullButton.setIcon(Icons.getIcon(ImageConstants.GIT_PULL_ICON));
		pullButton.setToolTipText(translator.getTranslation(Tags.PULL_BUTTON_TOOLTIP));
		setCustomWidthOn(pullButton);

		gitToolbar.add(pushButton);
		gitToolbar.add(pullButton);
	}

	/**
	 * Sets a custom width on the given button
	 * 
	 * @param button
	 *          - the button to set the width
	 */
	private void setCustomWidthOn(ToolbarButton button) {
		Dimension d = button.getPreferredSize();
		d.width = 30;
		button.setPreferredSize(d);
		button.setMinimumSize(d);
		button.setMaximumSize(d);
	}
}
