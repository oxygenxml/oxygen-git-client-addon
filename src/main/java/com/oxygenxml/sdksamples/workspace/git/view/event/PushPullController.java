package com.oxygenxml.sdksamples.workspace.git.view.event;

import javax.swing.JOptionPane;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.UserCredentials;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.view.LoginDialog;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;

public class PushPullController {

	private StagingPanel stagingPanel;
	private GitAccess gitAccess;
	private Command command;
	
	public PushPullController(GitAccess gitAccess ) {
		this.gitAccess = gitAccess;
	}

	public void loadNewCredentials() {
		new LoginDialog(this, gitAccess.getHostName());
	}

	public void updateCredentials() {
		execute(command);
	}

	public void execute(Command command) {
		this.command = command;
		UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
		stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setEnabled(false);
		stagingPanel.getWorkingCopySelectionPanel().getBrowseButton().setEnabled(false);
		stagingPanel.getCommitPanel().getCommitButton().setEnabled(false);
		stagingPanel.getToolbarPanel().getPushButton().setEnabled(false);
		stagingPanel.getToolbarPanel().getPullButton().setEnabled(false);
			new Thread(new Runnable() {
				
				@Override
				public void run() {
				try {
					if (command == Command.PUSH) {
						gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
						JOptionPane.showMessageDialog(null, "Push successful");
					} else {
						gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
						JOptionPane.showMessageDialog(null, "Pull successful");
					}
				} catch (GitAPIException e) {
					if (e.getMessage().contains("not authorized")) {
						JOptionPane.showMessageDialog(null, "Invalid credentials");
						loadNewCredentials();
						return;
					}
					e.printStackTrace();
				}
				
				stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setEnabled(true);
				stagingPanel.getWorkingCopySelectionPanel().getBrowseButton().setEnabled(true);
				stagingPanel.getCommitPanel().getCommitButton().setEnabled(true);
				stagingPanel.getToolbarPanel().getPushButton().setEnabled(true);
				stagingPanel.getToolbarPanel().getPullButton().setEnabled(true);
				
				
				}
			}).start();
	}

	public void setContainerPanel(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
	}

}
