package com.oxygenxml.git.view.event;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.jaxb.entities.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.LoginDialog;
import com.oxygenxml.git.view.PullDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class PushPullController implements Subject<PushPullEvent> {

	private static Logger logger = Logger.getLogger(PushPullController.class);

	private Observer<PushPullEvent> observer;
	private GitAccess gitAccess;
	private Command command;

	public PushPullController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public UserCredentials loadNewCredentials() {
		return new LoginDialog(gitAccess.getHostName()).getUserCredentials();
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
						Status status = gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());

						if (Status.OK == status) {
							((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Push successful");
						} else if (Status.REJECTED_NONFASTFORWARD == status) {
							((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Push failed, please get your repository up to date(PULL)");
						}
					} else {
						PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
						if (PullStatus.OK == response.getStatus()) {
							JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
									"Pull successful");
						} else if (PullStatus.UNCOMITED_FILES == response.getStatus()) {
							((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
									.showInformationMessage("Cannot push with uncommited changes");

						} else if (PullStatus.CONFLICTS == response.getStatus()) {
							new PullDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(), "Information",
									true, response.getConflictingFiles());

						}
					}
				} catch (GitAPIException e) {
					if (e.getMessage().contains("not authorized") || e.getMessage().contains("not permitted")) {
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"Invalid credentials for " + userCredentials.getUsername());
						UserCredentials loadNewCredentials = loadNewCredentials();
						if (loadNewCredentials != null) {
							execute(command);
						}
					}
					e.printStackTrace();
				} catch (RevisionSyntaxException e) {
					e.printStackTrace();
				} catch (AmbiguousObjectException e) {
					e.printStackTrace();
				} catch (IncorrectObjectTypeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
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
