package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * Contains additional support buttons like push and pull
 * 
 * @author Beniamin Savu
 *
 */
public class ToolbarPanel extends JPanel {

	private JToolBar gitToolbar;
	private JLabel statusInformationLabel;
	private PushPullController pushPullController;
	private ToolbarButton pushButton;
	private ToolbarButton pullButton;
	private int pushesAhead = 0;
	private int pullsBehind = 0;
	private Translator translator;

	public ToolbarPanel(PushPullController pushPullController, Translator translator) {
		this.pushPullController = pushPullController;
		this.statusInformationLabel = new JLabel();
		this.translator = translator;
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
	 * making the visible
	 */
	public void createGUI() {
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
		addPushAndPullButtons();
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
		gitToolbar.setFloatable(false);

		// PUSH button
		Action pushAction = new Action() {

			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PUSH);
				if (pullsBehind == 0) {
					pushesAhead = 0;
				}
			}

			public void setEnabled(boolean b) {
			}

			public void removePropertyChangeListener(PropertyChangeListener listener) {
			}

			public void putValue(String key, Object value) {
			}

			public boolean isEnabled() {
				return true;
			}

			public Object getValue(String key) {
				return null;
			}

			public void addPropertyChangeListener(PropertyChangeListener listener) {

			} 
		};
		pushButton = new ToolbarButton(pushAction, false) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				// TODO Paint the counter
				String str = "";
				if (pushesAhead > 0) {
					str = "" + pushesAhead;
				}
				g.setFont(g.getFont().deriveFont(Font.BOLD));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				g.drawString(str, pushButton.getWidth() - stringWidth, pushButton.getHeight() - fontMetrics.getDescent());
			}
		};
		
		pushButton.setIcon(Icons.getIcon(ImageConstants.GIT_PUSH_ICON));
		pushButton.setToolTipText(translator.getTraslation(Tags.PUSH_BUTTON_TOOLTIP));

		// PULL button
		Action pullAction = new Action() {

			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PULL);
				pullsBehind = 0;
			}

			public void setEnabled(boolean b) {
			}

			public void removePropertyChangeListener(PropertyChangeListener listener) {
			}

			public void putValue(String key, Object value) {
			}

			public boolean isEnabled() {
				return true;
			}

			public Object getValue(String key) {
				return null;
			}

			public void addPropertyChangeListener(PropertyChangeListener listener) {
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
				g.setFont(g.getFont().deriveFont(Font.BOLD));
				FontMetrics fontMetrics = g.getFontMetrics(g.getFont());
				int stringWidth = fontMetrics.stringWidth(str);
				//g.drawString(str, pushButton.getWidth() - stringWidth, pushButton.getHeight() - fontMetrics.getDescent());
				g.drawString(str, pullButton.getWidth() - stringWidth, fontMetrics.getHeight() - fontMetrics.getDescent());
			}

		};
		pullButton.setIcon(Icons.getIcon(ImageConstants.GIT_PULL_ICON));
		pullButton.setToolTipText(translator.getTraslation(Tags.PULL_BUTTON_TOOLTIP));

		gitToolbar.add(pushButton);
		gitToolbar.add(pullButton);

	}
}
