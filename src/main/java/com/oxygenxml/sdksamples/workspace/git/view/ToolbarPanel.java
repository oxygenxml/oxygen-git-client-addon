package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
				UserCredentials userCredentials = OptionsManager.getInstance().getCredentials();
				if (userCredentials.getUsername() == null || userCredentials.getPassword() == null) {
					LoginDialog loginDialog = new LoginDialog(gitAccess, true);
				} else {
					try {
						gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
					} catch (GitAPIException e1) {
						LoginDialog loginDialog = new LoginDialog(gitAccess, true);
						e1.printStackTrace();
					}
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
