package com.oxygenxml.sdksamples.workspace.git.view.event;

import javax.swing.JOptionPane;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.UserCredentials;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.view.LoginDialog;
import com.oxygenxml.sdksamples.workspace.git.view.StagingPanel;

public class PushPullController implements Subject<PushPullEvent> {

	private Observer<PushPullEvent> observer;
	private GitAccess gitAccess;
	private Command command;

	public PushPullController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public void loadNewCredentials() {
		new LoginDialog(this, gitAccess.getHostName());
	}

	public void updateCredentials() {
		execute(command);
	}

	public void execute(final Command command) {
		this.command = command;
		final UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED);
		notifyObservers(pushPullEvent);
		new Thread(new Runnable() {

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
					}
					e.printStackTrace();
				} finally{
					PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.FINISHED);
					notifyObservers(pushPullEvent);
				}

			}
		}).start();
	}

	private void notifyObservers(PushPullEvent pushPullEvent) {
		observer.stateChanged(pushPullEvent);
	}

	public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	public void removeObserver(Observer<PushPullEvent> obj) {
		observer = null;
	}

}
