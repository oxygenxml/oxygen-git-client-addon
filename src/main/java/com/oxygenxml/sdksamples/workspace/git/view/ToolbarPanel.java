package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.eclipse.jgit.transport.PushConnection;

import com.oxygenxml.sdksamples.workspace.git.constants.ImageConstants;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.UserCredentials;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.view.event.Command;
import com.oxygenxml.sdksamples.workspace.git.view.event.PushPullController;

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

	public void createGUI() {
		this.setLayout(new BorderLayout());
		addPushAndPullButtons();
	}

	private void addPushAndPullButtons() {
		gitToolbar = new JToolBar();

		pushButton = new JButton(new ImageIcon(ImageConstants.GIT_PUSH_ICON));
		pushButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PUSH);
			}
		});
		pushButton.setToolTipText("Push");
		gitToolbar.add(pushButton);

		pullButton = new JButton(new ImageIcon(ImageConstants.GIT_PULL_ICON));
		pullButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pushPullController.execute(Command.PUSH);
			}
		});
		pullButton.setToolTipText("Pull");
		gitToolbar.add(pullButton);

		storeCredentials = new JButton(new ImageIcon(ImageConstants.STORE_CREDENTIALS_ICON));
		storeCredentials.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
		storeCredentials.setToolTipText("Update Credentials");
		gitToolbar.add(storeCredentials);

		this.add(gitToolbar, BorderLayout.PAGE_START);
	}

}
