package com.oxygenxml.git.view.event;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.LoginDialog;
import com.oxygenxml.git.view.PullDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * 
 * Executes the push and pull commands and update the observer state after
 * initiating those commands. Prompts the user to enter new credentials if
 * necessary.
 * 
 * @author Beniamin Savu
 *
 */
public class PushPullController implements Subject<PushPullEvent> {

	private static Logger logger = Logger.getLogger(PushPullController.class);

	/**
	 * After a pull or push this will chage it's state
	 */
	private Observer<PushPullEvent> observer;

	/**
	 * The Git API
	 */
	private GitAccess gitAccess;

	/**
	 * The command to execute (Push or Pull)
	 */
	private Command command;

	public PushPullController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	/**
	 * Opens a login dialog to update the credentials
	 * 
	 * @return the new credentials
	 */
	public UserCredentials loadNewCredentials() {
		return new LoginDialog(gitAccess.getHostName()).getUserCredentials();
	}

	/**
	 * Creates a new Thread to do the action depending on the given command(Push
	 * or Pull) so that the application will not freeze.
	 * 
	 * 
	 * @param command
	 *          - The command to execute
	 */
	public void execute(final Command command) {
		this.command = command;
		final UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED);
		notifyObservers(pushPullEvent);
		new Thread(new Runnable() {

			public void run() {
				try {
					if (command == Command.PUSH) {
						push(userCredentials);
					} else {
						pull(userCredentials);
					}
				} catch (GitAPIException e) {
					if (e.getMessage().contains("not authorized")) {
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"Invalid credentials for " + userCredentials.getUsername());
						UserCredentials loadNewCredentials = loadNewCredentials();
						if (loadNewCredentials != null) {
							execute(command);
						}
					}
					if (e.getMessage().contains("not permitted")) {
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"You have no rights to push in this repository " + userCredentials.getUsername());
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

			/**
			 * Pull the changes and inform the user with messages depending on the
			 * result status
			 * 
			 * @param userCredentials
			 *          - credentials used to push the changes
			 * @throws WrongRepositoryStateException
			 * @throws InvalidConfigurationException
			 * @throws DetachedHeadException
			 * @throws InvalidRemoteException
			 * @throws CanceledException
			 * @throws RefNotFoundException
			 * @throws RefNotAdvertisedException
			 * @throws NoHeadException
			 * @throws TransportException
			 * @throws GitAPIException
			 * @throws AmbiguousObjectException
			 * @throws IncorrectObjectTypeException
			 * @throws IOException
			 */
			private void pull(final UserCredentials userCredentials)
					throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
					InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException,
					TransportException, GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
				PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
				if (PullStatus.OK == response.getStatus()) {
					JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
							"Pull successful");
				} else if (PullStatus.UNCOMITED_FILES == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Cannot pull with uncommited changes");

				} else if (PullStatus.CONFLICTS == response.getStatus()) {
					// prompts a dialog showing the files in conflict
					new PullDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(), "Information", true,
							response.getConflictingFiles());

				} else if (PullStatus.UP_TO_DATE == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Repository is already up to date");
				}
			}

			/**
			 * Pushes the changes and inform the user user depending on the result
			 * status
			 * 
			 * @param userCredentials
			 *          - credentials used to pull the changes
			 * @throws InvalidRemoteException
			 * @throws TransportException
			 * @throws GitAPIException
			 * @throws IOException
			 */
			private void push(final UserCredentials userCredentials)
					throws InvalidRemoteException, TransportException, GitAPIException, IOException {
				Status status = gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());

				if (Status.OK == status) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Push successful");
				} else if (Status.REJECTED_NONFASTFORWARD == status) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("Push failed, please get your repository up to date(PULL)");
				} else if (Status.UP_TO_DATE == status) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showInformationMessage("There was nothing to push");
				}
			}
		}).start();
	}

	/**
	 * Notifies the observer to update it's state with the given Event fired from
	 * a Push or Pull action
	 * 
	 * @param pushPullEvent
	 *          - the Event fired
	 */
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
