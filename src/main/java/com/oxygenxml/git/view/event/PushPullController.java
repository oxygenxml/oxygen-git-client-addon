package com.oxygenxml.git.view.event;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
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
		
		Thread th = new Thread(new ExecuteCommandRunnable(command));
		
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

  /**
   * Execute command runnable.
   */
  private class ExecuteCommandRunnable implements Runnable {
    /**
     * The command to execute (push or pull).
     */
    private Command command;

    /**
     * Constructor.
     * 
     * @param command The command to execute (push or pull).
     */
    private ExecuteCommandRunnable(Command command) {
      this.command = command;
    }

    @Override
    public void run() {
      executeCommand(command);
    }

    /**
     * Executes the given command.
     * 
     * @param command Command to execute.
     */
    private void executeCommand(final Command command) {
      String hostName = gitAccess.getHostName();
      UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
      String message = "";
      boolean notifyFinish = true;
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Preparing for push/pull command");
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
        boolean shouldTryAgain = AuthUtil.handleAuthException(
            e,
            hostName,
            userCredentials,
            exMessage -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(exMessage),
            true);
        if (shouldTryAgain) {
          // Skip notification now. We try again.
          notifyFinish = false;
          // Try again.
          executeCommand(command);
        } else {
          message = (command == Command.PUSH ? "Push failed: " : "Pull failed: ") + e.getMessage();
        }
      } finally {
        if (notifyFinish) {
          PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.FINISHED, message);
          notifyObservers(pushPullEvent);
        }
      }
    }

    /**
     * Pull the changes and inform the user with messages depending on the result status.
     * 
     * @param userCredentials The credentials used to pull the new changes made by others.
     *          
     * @throws GitAPIException
     */
    private String pull(final UserCredentials userCredentials) throws GitAPIException {
      PullResponse response = gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
      String message = "";
      
      switch (response.getStatus()) {
        case OK:
          message = translator.getTranslation(Tags.PULL_SUCCESSFUL);
          break;
        case CONFLICTS:
          showPullConflicts(response);
          break;
        case REPOSITORY_HAS_CONFLICTS:
          ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
              .showWarningMessage(translator.getTranslation(Tags.PULL_WITH_CONFLICTS));
          break;
        case UP_TO_DATE:
          message = translator.getTranslation(Tags.PULL_UP_TO_DATE);
          break;
        case LOCK_FAILED:
          message = translator.getTranslation(Tags.LOCK_FAILED);
          break;
        default:
          // Nothing
          break;
      }
      return message;
    }

    /**
     * Push the changes and inform the user with messages depending on the result status.
     * 
     * @param userCredentials The credentials used to push the changes.

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
  }
}
