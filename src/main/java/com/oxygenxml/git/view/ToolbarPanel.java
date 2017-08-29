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

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.view.dialog.BranchSelectDialog;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * Contains additional support buttons like push and pull
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel {

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
	private Refresh refresh;

	public ToolbarPanel(PushPullController pushPullController, Translator translator, Refresh refresh) {
		this.pushPullController = pushPullController;
		this.statusInformationLabel = new JLabel();
		this.translator = translator;
		this.refresh = refresh;
	}

	public JButton getPushButton() {
		return pushButton;
	}

	public JButton getPullButton() {
		return pullButton;
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
		this.setLayout(new GridBagLayout());
		this.pushesAhead = GitAccess.getInstance().getPushesAhead();
		this.pullsBehind = GitAccess.getInstance().getPullsBehind();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		addPushAndPullButtons();
		addBranchSelectButton();
		addSubmoduleSelectButton();
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

	private void addSubmoduleSelectButton() {
		Action branchSelectAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						new SubmoduleSelectDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								translator.getTraslation(Tags.SUBMODULE_DIALOG_TITLE), true, refresh, translator);
					}
				} catch (NoRepositorySelected e1) {
				}
			}
		};
		submoduleSelectButton = new ToolbarButton(branchSelectAction, false);
		submoduleSelectButton.setIcon(Icons.getIcon(ImageConstants.GIT_SUBMODULE_ICON));
		submoduleSelectButton.setToolTipText(translator.getTraslation(Tags.SELECT_SUBMODULE_BUTTON_TOOLTIP));
		setCustomWidthOn(submoduleSelectButton);
		
		gitToolbar.add(submoduleSelectButton);
	}

	private void addBranchSelectButton() {
		Action branchSelectAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
						new BranchSelectDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								translator.getTraslation(Tags.BRANCH_SELECTION_DIALOG_TITLE), true, refresh, translator);
					}
				} catch (NoRepositorySelected e1) {
				}
			}
		};
		branchSelectButton = new ToolbarButton(branchSelectAction, false);
		branchSelectButton.setIcon(Icons.getIcon(ImageConstants.GIT_BRANCH_ICON));
		branchSelectButton.setToolTipText(translator.getTraslation(Tags.CHANGE_BRANCH_BUTTON_TOOLTIP));
		setCustomWidthOn(branchSelectButton);

		gitToolbar.add(branchSelectButton);
	}

	public void updateInformationLabel() {
		String currentBranch = GitAccess.getInstance().getCurrentBranch();
		String message = "";
		if (!"".equals(currentBranch)) {
			message += "<html>";
			message += "Branch: " + "<b>" + currentBranch + "</b> - ";
			if (pullsBehind == 0) {
				message += "Up to date";
			} else {
				message += pullsBehind + " pulls behind";
			}
			message += "</html>";
		}
		statusInformationLabel.setText(message);
	}

	/**
	 * Adds to the tool bar the Push and Pull Buttons
	 */
	private void addPushAndPullButtons() {
		gitToolbar = new JToolBar();
		gitToolbar.setOpaque(false);
		gitToolbar.setFloatable(false);

		// PUSH button
		Action pushAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null) {
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
		pushButton.setToolTipText(translator.getTraslation(Tags.PUSH_BUTTON_TOOLTIP));
		setCustomWidthOn(pushButton);

		// PULL button
		Action pullAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (GitAccess.getInstance().getRepository() != null)
						pushPullController.execute(Command.PULL);
					pullsBehind = 0;
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
		pullButton.setToolTipText(translator.getTraslation(Tags.PULL_BUTTON_TOOLTIP));
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
