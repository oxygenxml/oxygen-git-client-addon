package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.eclipse.jgit.api.errors.GitAPIException;

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
				if (userCredentials.getUsername() == null || userCredentials.getPassword() == null) {
					// TODO I would prefer to leave the dialog with just the task to give you the credentials.
					// The push/pull logic should stay in dedicated actions.
					LoginDialog loginDialog = new LoginDialog(gitAccess, true);
				} else {
					Thread thread = new Thread(new AppWorker(userCredentials, gitAccess, true));
					thread.start();
				}
			}
		});
		gitToolbar.add(pushButton);

		JButton pullButton = new JButton(new ImageIcon(ImageConstants.GIT_PULL_ICON));
		pullButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LoginDialog loginDialog = new LoginDialog(gitAccess, false);
			}
		});
		gitToolbar.add(pullButton);

		this.add(gitToolbar, BorderLayout.PAGE_START);
	}

}
