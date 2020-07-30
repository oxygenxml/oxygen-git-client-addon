package com.oxygenxml.git.view.event;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.RebaseConflictsException;
import com.oxygenxml.git.service.RebaseUncommittedChangesException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.dialog.PullStatusAndFilesDialog;
import com.oxygenxml.git.view.dialog.RebaseInProgressDialog;

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
	 * Execute an push or pull action, depending on the given command.
	 * 
	 * @param command The command runnable to execute.
	 * 
	 * @return The result of the operation execution.
	 */
	private Future<?> execute(String message, ExecuteCommandRunnable command) {
		// Notify push about to start.
		PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.STARTED, message);
		notifyObservers(pushPullEvent);
		
		return GitOperationScheduler.getInstance().schedule(command);
	}
	
	/**
	 * Push.
	 */
	public Future<?> push() {
	  return execute(translator.getTranslation(Tags.PUSH_IN_PROGRESS), new ExecutePushRunnable());
	}
	
	/**
	 * Pull.
	 */
	public Future<?> pull() {
	 return	pull(PullType.MERGE_FF);
	}
	
	/**
	 * Pull and choose the merging strategy.
	 * 
	 * @param pullType The pull type / merging strategy.
	 */
	public Future<?> pull(PullType pullType) {
    return execute(translator.getTranslation(Tags.PULL_IN_PROGRESS), new ExecutePullRunnable(pullType));
  }
	
	/**
	 * Notifies the observer to update it's state with the given Event fired from
	 * a Push or Pull action
	 * 
	 * @param pushPullEvent
	 *          - the Event fired
	 */
	private void notifyObservers(PushPullEvent pushPullEvent) {
	  if (observer != null) {
	    observer.stateChanged(pushPullEvent);
	  }
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
	protected void showPullSuccessfulWithConflicts(PullResponse response) {
    new PullStatusAndFilesDialog(
        translator.getTranslation(Tags.PULL_WITH_CONFLICTS_DIALOG_TITLE),
        response.getConflictingFiles(),
        translator.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
  }
	

  /**
   * Pull failed because there are uncommitted files that would be overwritten.
   * 
   * @param e Exception.
   */
  protected void showPullFailedBecauseOfCertainChanges(List<String> filesWithChanges, String message) {
    if (logger.isDebugEnabled()) {
      logger.info("Pull failed with the following message: " + message + ". Resources: " + filesWithChanges);
    }
    new PullStatusAndFilesDialog(
        translator.getTranslation(Tags.PULL_STATUS), 
        filesWithChanges, 
        message);
  }
  
  /**
   * Show the "Rebase in progress" dialog. It allows the user to continue or abort the rebase.
   */
  protected void showRebaseInProgressDialog() {
    new RebaseInProgressDialog().setVisible(true);
  }

  /**
   * Execute push / pull.
   */
  private abstract class ExecuteCommandRunnable implements Runnable {

    @Override
    public void run() {
      executeCommand();
    }

    /**
     * Executes the command. If authentication is to be tried again, the method will be called recursively.
     */
    private void executeCommand() {
      String hostName = gitAccess.getHostName();
      UserCredentials userCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
      String message = "";
      boolean notifyFinish = true;
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Preparing for push/pull command");
        }
        message = doOperation(userCredentials);
      } catch (JGitInternalException e) {
        logger.debug(e, e);
        
        Throwable cause = e.getCause();
        if (cause instanceof org.eclipse.jgit.errors.CheckoutConflictException) {
          String[] conflictingFile = ((org.eclipse.jgit.errors.CheckoutConflictException) cause).getConflictingFiles();
          showPullFailedBecauseOfCertainChanges(
              Arrays.asList(conflictingFile),
              translator.getTranslation(Tags.PULL_REBASE_FAILED_BECAUSE_CONFLICTING_PATHS));
        } else if (cause instanceof org.eclipse.jgit.errors.LockFailedException) {
          // It's a pretty serious exception. Present it in a dialog so that the user takes measures.
          LockFailedException lockFailedException = (org.eclipse.jgit.errors.LockFailedException) cause;
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(lockFailedException.getMessage(), cause);
          
          // This message gets presented in a status, at the bottom of the staging view.
          message = composeAndReturnFailureMessage(lockFailedException.getMessage());
        } else {
          // It's a pretty serious exception. Present it in a dialog so that the user takes measures.
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
          message = composeAndReturnFailureMessage(e.getMessage());
        }
      } catch (RebaseUncommittedChangesException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getUncommittedChanges(),
            translator.getTranslation(Tags.PULL_REBASE_FAILED_BECAUSE_UNCOMMITTED));
      } catch (RebaseConflictsException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
            translator.getTranslation(Tags.PULL_REBASE_FAILED_BECAUSE_CONFLICTING_PATHS));
      } catch (CheckoutConflictException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
            translator.getTranslation(Tags.PULL_WOULD_OVERWRITE_UNCOMMITTED_CHANGES));
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
          executeCommand();
        } else {
          message = composeAndReturnFailureMessage(e.getMessage());
        }
      } catch (Exception e) {
        message = composeAndReturnFailureMessage(e.getMessage());
        logger.error(e, e);
      } finally {
        if (notifyFinish) {
          PushPullEvent pushPullEvent = new PushPullEvent(ActionStatus.FINISHED, message);
          notifyObservers(pushPullEvent);
        }
      }
    }

    /**
     * Compose and return failure message.
     */
    protected abstract String composeAndReturnFailureMessage(String message);

    /**
     * Push or pull, depending on the implementation.
     */
    protected abstract String doOperation(UserCredentials userCredentials) throws GitAPIException;
    
    
  }

  /**
   * Execute PUSH.
   */
  private class ExecutePushRunnable extends ExecuteCommandRunnable {
  
    /**
     * Push the changes and inform the user with messages depending on the result status.
     * 
     * @param userCredentials The credentials used to push the changes.
  
     * @throws GitAPIException
     */
    @Override
    protected String doOperation(UserCredentials userCredentials)
        throws  GitAPIException {
      PushResponse response = gitAccess.push(userCredentials.getUsername(), userCredentials.getPassword());
      String message = "";
      if (Status.OK == response.getStatus()) {
        message = translator.getTranslation(Tags.PUSH_SUCCESSFUL);
      } else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
            .showWarningMessage(translator.getTranslation(Tags.BRANCH_BEHIND));
      } else if (Status.UP_TO_DATE == response.getStatus()) {
        message = translator.getTranslation(Tags.PUSH_UP_TO_DATE);
      } else if (Status.REJECTED_OTHER_REASON == response.getStatus()) {
        String warnMess = "The operation was rejected.";
        if (response.getMessage() != null) {
          warnMess += " Details: " + response.getMessage();
        } else {
          warnMess += " No details available.";
        }
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showWarningMessage(warnMess);
      }
      return message;
    }

    @Override
    protected String composeAndReturnFailureMessage(String message) {
      return translator.getTranslation(Tags.PUSH_FAILED) + ": " + message;
    }
  }

  /**
   * Execute command runnable.
   */
  private class ExecutePullRunnable extends ExecuteCommandRunnable {
    
    private PullType pullType;

    public ExecutePullRunnable(PullType pullType) {
      this.pullType = pullType;
    }
    /**
     * Pull the changes and inform the user with messages depending on the result status.
     * 
     * @param userCredentials The credentials used to pull the new changes made by others.
     *          
     * @return The display message.
     * @throws GitAPIException
     */
    @Override
    protected String doOperation(UserCredentials userCredentials) throws GitAPIException {
      String message = "";

      RepositoryState repositoryState = null;
      try {
        repositoryState = gitAccess.getRepository().getRepositoryState();
      } catch (NoRepositorySelected e) {
        logger.debug(e, e);
      }
      
      if (logger.isDebugEnabled()) {
        logger.debug("Do pull. Pull type: " + pullType);
        logger.debug("Repo state: " + repositoryState);
      }
      
      if(repositoryState != null) {
    	  if (repositoryState == RepositoryState.MERGING_RESOLVED) {
    		  ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
    		  .showWarningMessage(translator.getTranslation(Tags.CONCLUDE_MERGE_MESSAGE));
    	  } else if (repositoryState == RepositoryState.REBASING_MERGE
    			  || repositoryState == RepositoryState.REBASING_REBASING) {
    		  showRebaseInProgressDialog();
    	  } else {
    		  PullResponse response = gitAccess.pull(
    				  userCredentials.getUsername(),
    				  userCredentials.getPassword(),
    				  pullType);
    		  switch (response.getStatus()) {
    		  case OK:
    			  message = translator.getTranslation(Tags.PULL_SUCCESSFUL);
    			  break;
    		  case CONFLICTS:
    			  showPullSuccessfulWithConflicts(response);
    			  if (pullType == PullType.REBASE) {
    				  PushPullEvent pushPullEvent = new PushPullEvent(
    						  ActionStatus.PULL_REBASE_CONFLICT_GENERATED, "");
    				  notifyObservers(pushPullEvent);
    			  }
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
    	  }
      }
      return message;
    }

    @Override
    protected String composeAndReturnFailureMessage(String message) {
      return translator.getTranslation(Tags.PULL_FAILED) + ": " + message;
    }
  }
}
