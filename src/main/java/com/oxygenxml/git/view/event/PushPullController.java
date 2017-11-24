package com.oxygenxml.git.view.event;

import java.io.IOException;

import javax.swing.JFrame;

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
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
	 * @return the new credentials or <code>null</code> if the user canceled.
	 */
	public UserCredentials requestNewCredentials(String loginMessage) {
		return 
		    new LoginDialog(
		        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		        translator.getTranslation(Tags.LOGIN_DIALOG_TITLE), 
		        true, 
		        gitAccess.getHostName(), 
		        loginMessage, 
		        translator).getUserCredentials();
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
			message = translator.getTranslation(Tags.PUSH_IN_PROGRESS);
		} else {
			message = translator.getTranslation(Tags.PULL_IN_PROGRESS);
		}
		
		// Notify push about to start.
		// TODO This method recursively calls itself. This means we could fire this event multiple times.
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED, message);
		notifyObservers(pushPullEvent);
		
		new Thread(new Runnable() {

			public void run() {

				String message = "";
				boolean notifyFinish = true;
        try {
					if (logger.isDebugEnabled()) {
						logger.debug("Preapring for push/pull command");
					}
					if (command == Command.PUSH) {
						message = push(userCredentials);
					} else {
						message = pull(userCredentials);
					}
				} catch (GitAPIException e) {
				  // Exception handling.
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
					if (e instanceof CheckoutConflictException) {
					  // Notify that there are conflicts that should be resolved in the staging area.
						new PullWithConflictsDialog(
								(JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getParentFrame(),
								// TODO i18n.
								"Pull Status", 
								true, 
								((CheckoutConflictException) e).getConflictingPaths(), 
								translator,
								translator.getTranslation(Tags.PULL_CHECKOUT_CONFLICT_MESSAGE));
						
						if (logger.isDebugEnabled()) {
						  logger.info(((CheckoutConflictException) e).getConflictingPaths());
						}
					} else if (e.getMessage().contains("not authorized")) {
					  // Authorization problems.
						String loginMessage = "";
						if ("".equals(userCredentials.getUsername())) {
						  // No credentials were used but they are required.
							loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE);
						} else {
						  // Invalid credentails.
							loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
									+ userCredentials.getUsername();
						}
						// Request new credentials.
						UserCredentials loadNewCredentials = requestNewCredentials(loginMessage);
						
						if (loadNewCredentials != null) {
						  // Skip notification now. We try again.
							notifyFinish = false;
							// Try again.
							execute(command);
						}
					} else if (e.getMessage().contains("not permitted")) {
					  // The user doesn't have permissions.
						((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.showWarningMessage(translator.getTranslation(Tags.NO_RIGHTS_TO_PUSH_MESSAGE));
						// Request new credentials.
						UserCredentials loadNewCredentials = requestNewCredentials(
								translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS) + " "
										+ userCredentials.getUsername());
						if (loadNewCredentials != null) {
						  // Avoid notification now. We try again.
							notifyFinish = false;
							// Try again.
							execute(command);
						}
					} else if (e.getMessage().contains("origin: not found")
							|| e.getMessage().contains("No value for key remote.origin.url found in configuration")) {
					  // No remote.
						new AddRemoteDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								translator.getTranslation(Tags.ADD_REMOTE_DIALOG_TITLE), true, translator);
					} else if (e.getMessage().contains("Auth fail")) {
					  // This message is thrown for SSH.
					  // TODO i18n.
						String passPhraseMessage = "Please enter your SSH passphrase";
						String passphrase = new PassphraseDialog(passPhraseMessage).getPassphrase();
						if (passphrase != null) {
						  // Avoid notification now. We try again.
							notifyFinish = false;
							// Try again.
							execute(command);
						} else {
						  // TODO i18n
							message = "Command aborted";
						}
					}
				} catch (IOException e) {
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
				} finally {
					if (notifyFinish) {
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
					message = translator.getTranslation(Tags.PULL_SUCCESSFUL);
				} else if (PullStatus.CONFLICTS == response.getStatus()) {
					new PullWithConflictsDialog((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
							translator.getTranslation(Tags.PULL_WITH_CONFLICTS_DIALOG_TITLE), true, response.getConflictingFiles(),
							translator, translator.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
				} else if (PullStatus.UP_TO_DATE == response.getStatus()) {
					message = translator.getTranslation(Tags.PULL_UP_TO_DATE);
				} else if (PullStatus.REPOSITORY_HAS_CONFLICTS == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showWarningMessage(translator.getTranslation(Tags.PULL_WITH_CONFLICTS));
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
					message = translator.getTranslation(Tags.PUSH_SUCCESSFUL);
				} else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
					((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.showWarningMessage("Push failed, please get your repository up to date(PULL)");
				} else if (Status.UP_TO_DATE == response.getStatus()) {
					message = translator.getTranslation(Tags.PUSH_UP_TO_DATE);
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
