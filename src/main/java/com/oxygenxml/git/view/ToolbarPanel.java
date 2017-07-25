package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;

/**
 * Contains additional support buttons like push and pull
 * 
 * @author intern2
 *
 */
public class ToolbarPanel extends JPanel {

	private JToolBar gitToolbar;
	private PushPullController pushPullController;
	private JButton pushButton;
	private JButton pullButton;
	private JButton storeCredentials;

	public ToolbarPanel(PushPullController pushPullController) {
		this.pushPullController = pushPullController;
	}

	public JButton getPushButton() {
		return pushButton;
	}

	public void setPushButton(JButton pushButton) {
		this.pushButton = pushButton;
	}

	public JButton getPullButton() {
		return pullButton;
	}

	public void setPullButton(JButton pullButton) {
		this.pullButton = pullButton;
	}

	/**
	 * Sets the panel layout and creates all the buttons with their functionality
	 * making the visible
	 */
	public void createGUI() {
		this.setLayout(new BorderLayout());
		addPushAndPullButtons();
		addUndefinedButton();

		this.add(gitToolbar, BorderLayout.PAGE_START);
	}

	/**
	 * Adds to the tool bar the Push and Pull Buttons
	 */
	private void addPushAndPullButtons() {
		gitToolbar = new JToolBar();
		gitToolbar.setFloatable(false);
		pushButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_PUSH_ICON)));
		pushButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PUSH);
			}
		});
		pushButton.setToolTipText("Push");
		gitToolbar.add(pushButton);

		pullButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.GIT_PULL_ICON)));
		pullButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PULL);
			}
		});
		pullButton.setToolTipText("Pull");
		gitToolbar.add(pullButton);

	}

	/**
	 * Remains to be done
	 */
	private void addUndefinedButton() {
		storeCredentials = new JButton(
				new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.STORE_CREDENTIALS_ICON)));
		storeCredentials.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
			}

		});
		storeCredentials.setToolTipText("Update Credentials");
		gitToolbar.add(storeCredentials);
	}

}
