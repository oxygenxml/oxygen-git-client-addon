package com.oxygenxml.git.view.event;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
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

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.LoginDialog;
import com.oxygenxml.git.view.dialog.PassphraseDialog;
import com.oxygenxml.git.view.dialog.PullWithConflictsDialog;

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

	volatile boolean commandExecuted = true;

	private Translator translator;

	public PushPullController(GitAccess gitAccess, Translator translator) {
		this.translator = translator;
		this.gitAccess = gitAccess;
	}

	/**
	 * Opens a login dialog to update the credentials
	 * 
	 * @param loginMessage
	 * 
	 * @return the new credentials
	 */
	public UserCredentials loadNewCredentials(String loginMessage) {
		return new LoginDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				translator.getTraslation(Tags.LOGIN_DIALOG_TITLE), true, gitAccess.getHostName(), loginMessage, translator)
						.getUserCredentials();
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
		final UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
		String message = "";
		if (command == Command.PUSH) {
			message = translator.getTraslation(Tags.PUSH_IN_PROGRESS);
		} else {
			message = translator.getTraslation(Tags.PULL_IN_PROGRESS);
		}
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED, message);
		notifyObservers(pushPullEvent);
		new Thread(new Runnable() {

			public void run() {

				String message = "";
				try {
					commandExecuted = true;

					if (logger.isDebugEnabled()) {
						logger.debug("Preapring for push/pull command");
					}
					if (command == Command.PUSH) {
						message = push(userCredentials);
					} else {
						message = pull(userCredentials);
					}
				} catch (GitAPIException e) {

					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
					if (e instanceof CheckoutConflictException) {
						new PullWithConflictsDialog(
								(JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getParentFrame(),
								"Pull Status", true, ((CheckoutConflictException) e).getConflictingPaths(), translator,
								translator.getTraslation(Tags.PULL_CHECKOUT_CONFLICT_MESSAGE));
						System.out.println(((CheckoutConflictException) e).getConflictingPaths());
					}
					if (e.getMessage().contains("not authorized")) {
						String loginMessage = "";
						if ("".equals(userCredentials.getUsername())) {
							loginMessage = translator.getTraslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE);
						} else {
							loginMessage = translator.getTraslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
									+ userCredentials.getUsername();
						}
						UserCredentials loadNewCredentials = loadNewCredentials(loginMessage);
						if (loadNewCredentials != null) {
							commandExecuted = false;
							execute(command);
						}
						return;
					}
					if (e.getMessage().contains("not permitted")) {
						((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.showWarningMessage(translator.getTraslation(Tags.NO_RIGHTS_TO_PUSH_MESSAGE));
						UserCredentials loadNewCredentials = loadNewCredentials(
								translator.getTraslation(Tags.LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS) + " "
										+ userCredentials.getUsername());
						if (loadNewCredentials != null) {
							commandExecuted = false;
							execute(command);
						}
						return;
					}
					if (e.getMessage().contains("origin: not found")
							|| e.getMessage().contains("No value for key remote.origin.url found in configuration")) {
						new AddRemoteDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								translator.getTraslation(Tags.ADD_REMOTE_DIALOG_TITLE), true, translator);
						return;
					}

					if (e.getMessage().contains("Auth fail")) {
						String passPhraseMessage = "Please enter your SSH passphrase";
						String passphrase = new PassphraseDialog(passPhraseMessage).getPassphrase();
						if (passphrase != null) {
							commandExecuted = false;
							execute(command);
						} else {
							message = "Command aborted";
							return;
						}
					}
				} catch (IOException e) {
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
				} finally {
					if (commandExecuted) {
						PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.FINISHED, message);
						notifyObservers(pushPullEvent);
					}
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
			private String pull(final UserCredentials userCredentials)
					throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
					InvalidRemoteException, CanceledException, RefNotFoundException, RefNotAdvertisedException, NoHeadException,
					TransportException, GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
				PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
				String message = "";
				if (PullStatus.OK == response.getStatus()) {
					message = translator.getTraslation(Tags.PULL_SUCCESSFUL);
				} else if (PullStatus.CONFLICTS == response.getStatus()) {
					new PullWithConflictsDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
							translator.getTraslation(Tags.PULL_WITH_CONFLICTS_DIALOG_TITLE), true, response.getConflictingFiles(),
							translator, translator.getTraslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
				} else if (PullStatus.UP_TO_DATE == response.getStatus()) {
					message = translator.getTraslation(Tags.PULL_UP_TO_DATE);
				} else if (PullStatus.REPOSITORY_HAS_CONFLICTS == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showWarningMessage(translator.getTraslation(Tags.PULL_WITH_CONFLICTS));
				}
				return message;
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
			private String push(final UserCredentials userCredentials)
					throws InvalidRemoteException, TransportException, GitAPIException, IOException {
				PushResponse response = gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
				String message = "";
				if (Status.OK == response.getStatus()) {
					message = translator.getTraslation(Tags.PUSH_SUCCESSFUL);
				} else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showWarningMessage("Push failed, please get your repository up to date(PULL)");
				} else if (Status.UP_TO_DATE == response.getStatus()) {
					message = translator.getTraslation(Tags.PUSH_UP_TO_DATE);
				} else if (Status.REJECTED_OTHER_REASON == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showWarningMessage(response.getMessage());
				}
				return message;
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
