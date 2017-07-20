package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.oxygenxml.sdksamples.workspace.git.constants.ImageConstants;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.utils.UserCredentials;

public class ToolbarPanel extends JPanel {

	private JToolBar gitToolbar;
	private GitAccess gitAccess;

	public ToolbarPanel(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public void createGUI() {
		this.setLayout(new BorderLayout());
		addPushAndPullButtons();
	}

	private void addPushAndPullButtons() {
		gitToolbar = new JToolBar();

		JButton pushButton = new JButton(new ImageIcon(ImageConstants.GIT_PUSH_ICON));
		pushButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials();

				Thread thread = new Thread(new PushPullWorker(userCredentials, gitAccess, true));
				thread.start();
			}

		});
		pushButton.setToolTipText("Push");
		gitToolbar.add(pushButton);

		JButton pullButton = new JButton(new ImageIcon(ImageConstants.GIT_PULL_ICON));
		pullButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LoginDialog loginDialog = new LoginDialog(gitAccess);
			}
		});
		pullButton.setToolTipText("Pull");
		gitToolbar.add(pullButton);

		JButton storeCredentials = new JButton(new ImageIcon(ImageConstants.STORE_CREDENTIALS_ICON));
		storeCredentials.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LoginDialog loginDialog = new LoginDialog(gitAccess);
			}
		});
		storeCredentials.setToolTipText("Update Credentials");
		gitToolbar.add(storeCredentials);

		this.add(gitToolbar, BorderLayout.PAGE_START);
	}

}
