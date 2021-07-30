package com.oxygenxml.git.view.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.RebaseConflictsException;
import com.oxygenxml.git.service.RebaseUncommittedChangesException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.dialog.RebaseInProgressDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Used for executing Git operations.
 */
public class GitController extends GitControllerBase {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitController.class);

  /**
   * Translator for i18n.
   */
  private Translator translator = Translator.getInstance();

  /**
   * Public constructor
   * 
   * @param gitAccess Low level operation performer.
   */
  public GitController(GitAccess gitAccess) {
    super(gitAccess);
  }

  /**
   * Execute an push or pull action, depending on the given command.
   * 
   * @param message An optional message about the operation.
   * @param command The command runnable to execute.
   * 
   * @return The result of the operation execution.
   */
  private Future<?> execute(String message, ExecuteCommandRunnable command) {
    // Notify push about to start.
    PushPullEvent pushPullEvent = new PushPullEvent(command.getOperation(), message);
    listeners.fireOperationAboutToStart(pushPullEvent);

    return GitOperationScheduler.getInstance().schedule(command);
  }

  /**
   * Push.
   * 
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> push() {
    return execute(translator.getTranslation(Tags.PUSH_IN_PROGRESS), new ExecutePushRunnable());
  }

  /**
   * Pull.
   * 
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> pull() {
    return	pull(PullType.MERGE_FF);
  }

  /**
   * Pull and choose the merging strategy.
   * 
   * @param pullType The pull type / merging strategy.
   * 
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> pull(PullType pullType) {
    return execute(translator.getTranslation(Tags.PULL_IN_PROGRESS), new ExecutePullRunnable(pullType));
  }

  /**
   * Informs the user that pull was successful with conflicts.
   *  
   * @param response Pull response.
   */
  protected void showPullSuccessfulWithConflicts(PullResponse response) {
    List<String> conflictingFilesList = new ArrayList<>();
    conflictingFilesList.addAll(response.getConflictingFiles());
    FileStatusDialog.showWarningMessage(
        translator.getTranslation(Tags.PULL_STATUS),
        conflictingFilesList,
        translator.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS));
  }


  /**
   * Pull failed because there are uncommitted files that would be overwritten.
   * 
   * @param filesWithChanges Files with changes.
   * @param message The message to show.
   */
  protected void showPullFailedBecauseOfCertainChanges(List<String> filesWithChanges, String message) {
    if (logger.isDebugEnabled()) {
      logger.info("Pull failed with the following message: " + message + ". Resources: " + filesWithChanges);
    }
    FileStatusDialog.showWarningMessage(
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
     * @return The git operation performed by this command.
     */
    protected abstract GitOperation getOperation();

    /**
     * Executes the command. If authentication is to be tried again, the method will be called recursively.
     * 
     */
    private void executeCommand(){
      String hostName = gitAccess.getHostName();
      CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(hostName);
      Optional<PushPullEvent> event = Optional.empty();
      boolean notifyFinish = true;
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Preparing for push/pull command");
        }
        event = doOperation(credentialsProvider);
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
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(lockFailedException.getMessage(), lockFailedException);

          // This message gets presented in a status, at the bottom of the staging view.
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(lockFailedException.getMessage()), e));
        } else {
          // It's a pretty serious exception. Present it in a dialog so that the user takes measures.
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(e.getMessage(), e);
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(e.getMessage()), e));
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
      } catch (TransportException e) {
        try {
         
          if(getOperation().compareTo(GitOperation.PUSH)==0)
          {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(translator.getTranslation(Tags.PUSH_FAILED)
                +translator.getTranslation(Tags.PUSH_FAILED_TRANSPORT_EXCEPTION)+gitAccess.getRemoteURLFromConfig(), e);

          }
          else
          {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(translator.getTranslation(Tags.PULL_FAILED)
                +translator.getTranslation(Tags.PUSH_FAILED_TRANSPORT_EXCEPTION)+gitAccess.getRemoteURLFromConfig(), e);

          }
        } catch (NoRepositorySelected e1) {

          logger.error(e1, e1);
        }
      } catch (GitAPIException e) {
        // Exception handling.
        boolean shouldTryAgain = AuthUtil.handleAuthException(
            e,
            hostName,
            exMessage -> PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(exMessage),
            true);
        if (shouldTryAgain) {
          // Skip notification now. We try again.
          notifyFinish = false;
          // Try again.
          executeCommand();
        } else {
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(e.getMessage()), e));
        }
      } catch (Exception e) {
        event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(e.getMessage()), e));
        logger.error(e, e);
      } finally {
        if (notifyFinish) {
          PushPullEvent toFire = null;
          if (event.isPresent()) {
            toFire = event.get();
          } else {
            toFire = new PushPullEvent(getOperation(), "");
          }

          if (toFire.getCause() != null) {
            listeners.fireOperationFailed(toFire, toFire.getCause());
          } else {
            listeners.fireOperationSuccessfullyEnded(toFire);
          }
        }
      }
    }

    /**
     * Compose and return failure message.
     * 
     * @param message The initial message.
     * 
     * @return the failure message.
     */
    protected abstract String composeAndReturnFailureMessage(String message);

    /**
     * Push or pull, depending on the implementation.
     * 
     * @param credentialsProvider The credentials provider.
     * 
     * @return an optional response.
     * 
     * @throws GitAPIException
     */
    protected abstract Optional<PushPullEvent> doOperation(CredentialsProvider credentialsProvider) throws GitAPIException;
  }

  /**
   * Execute PUSH.
   */
  private class ExecutePushRunnable extends ExecuteCommandRunnable {

    @Override
    protected GitOperation getOperation() {
      return GitOperation.PUSH;
    }

    /**
     * Push the changes and inform the user with messages depending on the result status.
     * 
     * @param credentialsProvider The credentials provider.
     * 
     * @return An event with the operation result, if it was executed.

     * @throws GitAPIException
     */
    @Override
    protected Optional<PushPullEvent> doOperation(CredentialsProvider credentialsProvider)
        throws  GitAPIException {
      PushResponse response = gitAccess.push(credentialsProvider);
      PushPullEvent event = null;
      if (Status.OK == response.getStatus()) {
        event = new PushPullEvent(GitOperation.PUSH, translator.getTranslation(Tags.PUSH_SUCCESSFUL));
      } else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
        .showErrorMessage(translator.getTranslation(Tags.BRANCH_BEHIND));
      } else if (Status.UP_TO_DATE == response.getStatus()) {
        event = new PushPullEvent(GitOperation.PUSH, translator.getTranslation(Tags.PUSH_UP_TO_DATE));
      } else if (Status.REJECTED_OTHER_REASON == response.getStatus()) {
        String errMess = translator.getTranslation(Tags.PUSH_FAILED_UNKNOWN);
        if (response.getMessage() != null) {
          String details = response.getMessage();
          if (details.contains("pre-receive hook declined")) {
            details = translator.getTranslation(Tags.PRE_RECEIVE_HOOK_DECLINED_CUSTOM_MESSAGE);
          }
          errMess += " " + details;
        } else {
          errMess += " " + translator.getTranslation(Tags.NO_DETAILS_AVAILABLE);
        }
        ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showErrorMessage(errMess);
      }

      return Optional.ofNullable(event);
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
    @Override
    protected GitOperation getOperation() {
      return GitOperation.PULL;
    }

    /**
     * Pull the changes and inform the user with messages depending on the result status.
     * 
     * @param credentialsProvider The credentials provider.
     *          
     * @return An optional event describing the result.
     * 
     * @throws GitAPIException
     */
    @Override
    protected Optional<PushPullEvent> doOperation(CredentialsProvider credentialsProvider) throws GitAPIException {
      PushPullEvent event = null;

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
              credentialsProvider,
              pullType,
              OptionsManager.getInstance().getUpdateSubmodulesOnPull());
          event = treatPullResponse(response);
        }
      }
      return Optional.ofNullable(event);
    }

    /**
     * Treat push response.
     * 
     * @param response The pull response to treat.
     * 
     * @return an event describing the result of the pull operation.
     */
    private PushPullEvent treatPullResponse(PullResponse response) {
      PushPullEvent event = null;
      switch (response.getStatus()) {
        case OK:
          event = new PushPullEvent(GitOperation.PULL, translator.getTranslation(Tags.PULL_SUCCESSFUL));
          break;
        case CONFLICTS:
          showPullSuccessfulWithConflicts(response);
          if (pullType == PullType.REBASE) {
            event = new PushPullEvent(getOperation(), ActionStatus.PULL_REBASE_CONFLICT_GENERATED);
          } else if (pullType == PullType.MERGE_FF) {
            event = new PushPullEvent(getOperation(), ActionStatus.PULL_MERGE_CONFLICT_GENERATED);
          }
          break;
        case REPOSITORY_HAS_CONFLICTS:
          ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
              .showErrorMessage(translator.getTranslation(Tags.PULL_WHEN_REPO_IN_CONFLICT));
          break;
        case UP_TO_DATE:
          event = new PushPullEvent(GitOperation.PULL, translator.getTranslation(Tags.PULL_UP_TO_DATE));
          break;
        case LOCK_FAILED:
          event = new PushPullEvent(GitOperation.PULL, translator.getTranslation(Tags.LOCK_FAILED));
          break;
        default:
          // Nothing
          break;
      }
      return event;
    }

    @Override
    protected String composeAndReturnFailureMessage(String message) {
      return translator.getTranslation(Tags.PULL_FAILED) + ": " + message;
    }
  }
}
