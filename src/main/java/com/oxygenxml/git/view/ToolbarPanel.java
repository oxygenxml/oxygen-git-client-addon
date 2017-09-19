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
import java.util.ArrayList;
import java.util.List;

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
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Refresh;
import com.oxygenxml.git.view.dialog.BranchSelectDialog;
import com.oxygenxml.git.view.dialog.CloneRepositoryDialog;
import com.oxygenxml.git.view.dialog.SubmoduleSelectDialog;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.Subject;

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
public class ToolbarPanel extends JPanel implements Observer<ChangeEvent> {

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
	private Refresh refresh;

	private List<Subject<ChangeEvent>> subjects = new ArrayList<Subject<ChangeEvent>>();

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

	public JButton getCloneRepositoryButton(){
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
			
			public void actionPerformed(ActionEvent e) {
				new CloneRepositoryDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
						translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_TITLE), true, translator, refresh);
			}
		};
		
		cloneRepositoryButton = new ToolbarButton(cloneRepositoryAction, false); 
		cloneRepositoryButton.setIcon(Icons.getIcon(ImageConstants.GIT_CLONE_REPOSITORY_ICON));
		cloneRepositoryButton.setToolTipText(translator.getTraslation(Tags.CLONE_REPOSITORY_BUTTON_TOOLTIP));
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
		BranchInfo branchInfo = GitAccess.getInstance().getBranchInfo();
		String message = "";
		if (branchInfo.isDetached()) {
			message += "<html>";
			message += "Commit: <b>" + branchInfo.getShortBranchName() + "</b></html>";
			statusInformationLabel
					.setToolTipText(translator.getTraslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_DETACHED_HEAD) + " "
							+ branchInfo.getBranchName());
		} else {
			String currentBranch = branchInfo.getBranchName();
			if (!"".equals(currentBranch)) {
				message += "<html>";
				message += translator.getTraslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_BRANCH) + "<b>" + currentBranch
						+ "</b> - ";
				if (pullsBehind == 0) {
					message += translator.getTraslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_UP_TO_DATE);
				} else if (pullsBehind == 1) {
					message += pullsBehind + " " + translator.getTraslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_SINGLE_COMMIT);
				} else {
					message += pullsBehind + " "
							+ translator.getTraslation(Tags.TOOLBAR_PANEL_INFORMATION_STATUS_MULTIPLE_COMMITS);
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
					if (GitAccess.getInstance().getRepository() != null) {
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

	public void stateChanged(ChangeEvent changeEvent) {
		if (GitAccess.getInstance().getSubmodules().size() > 0) {
			submoduleSelectButton.setEnabled(true);
		} else {
			submoduleSelectButton.setEnabled(false);
		}
	}

	public void registerSubject(Subject<ChangeEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	public void unregisterSubject(Subject<ChangeEvent> subject) {
		subjects.remove(subject);

		subject.removeObserver(this);
	}
}
