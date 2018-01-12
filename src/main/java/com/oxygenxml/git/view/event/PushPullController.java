package com.oxygenxml.git.view.event;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
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
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * Translator for i18n.
	 */
	private Translator translator = Translator.getInstance();

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
		        gitAccess.getHostName(), 
		        loginMessage).getUserCredentials();
	}

	/**
	 * Creates a new Thread to do the action depending on the given command(Push
	 * or Pull) so that the application will not freeze.
	 * 
	 * 
	 * @param command
	 *          - The command to execute
	 * @return The thread that executes the command.
	 */
	public Thread execute(final Command command) {
		String message = "";
		if (command == Command.PUSH) {
			message = translator.getTranslation(Tags.PUSH_IN_PROGRESS);
		} else {
			message = translator.getTranslation(Tags.PULL_IN_PROGRESS);
		}
		
		// Notify push about to start.
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED, message);
		notifyObservers(pushPullEvent);
		
		Thread th = new Thread(new Runnable() {

			@Override
      public void run() {
				executeInternal(command);
			}

			/**
			 * Executes the given command.
			 * 
			 * @param command Command to execute.
			 */
      private void executeInternal(final Command command) {
        UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(gitAccess.getHostName());
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
				} catch (CheckoutConflictException e) {
          // Notify that there are conflicts that should be resolved in the staging area.
          showPullFailedBecauseofConflict(e);
          
          if (logger.isDebugEnabled()) {
            logger.info(e.getConflictingPaths());
          }
        } catch (GitAPIException e) {
				  // Exception handling.
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
					
					String lowercaseMsg = e.getMessage().toLowerCase();
					
					if (lowercaseMsg.contains("not authorized") 
					    || lowercaseMsg.contains("authentication not supported")) {
					  // Authorization problems.
						String loginMessage = "";
						if (userCredentials.getUsername() == null) {
						  // No credentials were used but they are required.
							loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE);
						} else {
						  // Invalid credentails.
							loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
									+ " " + userCredentials.getUsername();
						}
						// Request new credentials.
						UserCredentials loadNewCredentials = requestNewCredentials(loginMessage);
						
						if (loadNewCredentials != null) {
						  // Skip notification now. We try again.
							notifyFinish = false;
							// Try again.
							executeInternal(command);
						}
					} else if (lowercaseMsg.contains("not permitted")) {
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
							executeInternal(command);
						}
					} else if (lowercaseMsg.contains("origin: not found")
							|| lowercaseMsg.contains("no value for key remote.origin.url found in configuration")) {
					  // No remote.
						boolean linkRemote = new AddRemoteDialog().linkRemote();
						if (linkRemote) {
						  // Avoid notification now. We try again.
              notifyFinish = false;
              // Try again.
              executeInternal(command);
						}
					} else if (lowercaseMsg.contains("auth fail")) {
					  // This message is thrown for SSH.
						String passPhraseMessage = translator.getTranslation(Tags.ENTER_SSH_PASSPHRASE);
						String passphrase = new PassphraseDialog(passPhraseMessage).getPassphrase();
						if (passphrase != null) {
						  // Avoid notification now. We try again.
							notifyFinish = false;
							// Try again.
							executeInternal(command);
						} else {
							message = translator.getTranslation(Tags.COMMAND_ABORTED);
						}
					} else {
					  // Un-handled exception.
					  message = translator.getTranslation(Tags.PUSH_FAILED_UNKNOWN);
					  PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getClass().getName() + "\n" + e.getMessage());
					  logger.error(e, e);
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
			 *          
			 * @throws GitAPIException
			 */
			private String pull(final UserCredentials userCredentials) throws GitAPIException {
				PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
				String message = "";
				if (PullStatus.OK == response.getStatus()) {
					message = translator.getTranslation(Tags.PULL_SUCCESSFUL);
				} else if (PullStatus.CONFLICTS == response.getStatus()) {
					showPullConflicts(response);
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

			 * @throws GitAPIException
			 */
			private String push(final UserCredentials userCredentials) throws GitAPIException {
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
		});
		
		th.start();
		
		return th;
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

	@Override
  public void addObserver(Observer<PushPullEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	@Override
  public void removeObserver(Observer<PushPullEvent> obj) {
		observer = null;
	}
	
	/**
	 * Informs the user that pull was successful with conflicts.
	 *  
	 * @param response Pull response.
	 */
	protected void showPullConflicts(PullResponse response) {
    new PullWithConflictsDialog(translator.getTranslation(Tags.PULL_WITH_CONFLICTS_DIALOG_TITLE),
        response.getConflictingFiles(), translator.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
  }
	

  /**
   * Pull failed because there are uncommitted files that would be in conflict
   * after the pull.
   * 
   * @param e Exception.
   */
  protected void showPullFailedBecauseofConflict(CheckoutConflictException e) {
    new PullWithConflictsDialog(
        translator.getTranslation(Tags.PULL_STATUS), 
        e.getConflictingPaths(), 
        translator.getTranslation(Tags.PULL_CHECKOUT_CONFLICT_MESSAGE));
  }

}
