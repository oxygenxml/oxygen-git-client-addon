package com.oxygenxml.git.view.event;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.RebaseInProgressDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Used for executing Git operations.
 */
public class GitController extends GitControllerBase {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GitController.class);

  /**
   * Translator for i18n.
   */
  private Translator translator = Translator.getInstance();
  
  /**
   * Creates the GIT controller using the default GitAccess instance
   */
  public GitController() {
    this(GitAccess.getInstance());
  }

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
    MessagePresenterProvider.getBuilder(
        translator.getTranslation(Tags.PULL_STATUS), DialogType.WARNING)
        .setTargetFiles(conflictingFilesList)
        .setMessage(translator.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS))
        .setCancelButtonVisible(false)
        .buildAndShow();       
  }


  /**
   * Pull failed because there are uncommitted files that would be overwritten.
   * 
   * @param filesWithChanges Files with changes.
   * @param message The message to show.
   */
  protected void showPullFailedBecauseOfCertainChanges(List<String> filesWithChanges, String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Pull failed with the following message: " + message + ". Resources: " + filesWithChanges);
    }
    MessagePresenterProvider.getBuilder(
        translator.getTranslation(Tags.PULL_STATUS), DialogType.WARNING)
        .setTargetFiles(filesWithChanges)
        .setMessage(message)
        .setCancelButtonVisible(false)
        .buildAndShow();      
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
      PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
      try {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Preparing for push/pull command");
        }
        event = doOperation(credentialsProvider);
      } catch (JGitInternalException e) {
        LOGGER.debug(e.getMessage(), e);

        Throwable cause = e.getCause();
        if (cause instanceof org.eclipse.jgit.errors.CheckoutConflictException) {
          String[] conflictingFile = ((org.eclipse.jgit.errors.CheckoutConflictException) cause).getConflictingFiles();
          showPullFailedBecauseOfCertainChanges(
              Arrays.asList(conflictingFile),
              MessageFormat.format(translator.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                      translator.getTranslation(Tags.REBASE))
          );
        } else if (cause instanceof org.eclipse.jgit.errors.LockFailedException) {
          // It's a pretty serious exception. Present it in a dialog so that the user takes measures.
          LockFailedException lockFailedException = (org.eclipse.jgit.errors.LockFailedException) cause;
          pluginWS.showErrorMessage(lockFailedException.getMessage(), lockFailedException);

          // This message gets presented in a status, at the bottom of the staging view.
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(lockFailedException.getMessage()), e));
        } else if (cause instanceof IOException) {
          String causeMsg = cause.getMessage();
          if (getOperation() == GitOperation.PUSH 
              && causeMsg.contains("Source ref") 
              && causeMsg.contains("doesn't resolve")) {
            pluginWS.showErrorMessage(
                translator.getTranslation(Tags.PUSH_FAILED) + ": " 
                    + MessageFormat.format(
                        translator.getTranslation(Tags.UNBORN_BRANCH),
                        gitAccess.getBranchInfo().getBranchName()) + " "
                    + translator.getTranslation(Tags.COMMIT_BEFORE_PUSHING),
                e);
            event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(cause.getMessage()), e));
          } else {
            pluginWS.showErrorMessage(e.getMessage(), e);
            event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(e.getMessage()), e));
          }
        } else {
          // It's a pretty serious exception. Present it in a dialog so that the user takes measures.
          pluginWS.showErrorMessage(e.getMessage(), e);
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(e.getMessage()), e));
        }
      } catch (RebaseUncommittedChangesException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getUncommittedChanges(),
            translator.getTranslation(Tags.PULL_REBASE_FAILED_BECAUSE_UNCOMMITTED));
      } catch (RebaseConflictsException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
                MessageFormat.format(translator.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                        translator.getTranslation(Tags.REBASE))
        );
      } catch (CheckoutConflictException e) {
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
                MessageFormat.format(translator.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                        translator.getTranslation(Tags.MERGE))
        );
      } catch (TransportException e) {
        String exMsg = e.getMessage();
        boolean isAuthProblem = exMsg.contains(AuthUtil.NOT_AUTHORIZED)
            || exMsg.contains(AuthUtil.AUTHENTICATION_NOT_SUPPORTED)
            || exMsg.contains(AuthUtil.NOT_PERMITTED);
        boolean shouldTryAgain = isAuthProblem ? 
            AuthUtil.handleAuthException(
                e,
                hostName,
                pluginWS::showErrorMessage,
                true) 
            : treatTransportException(e);
        if (shouldTryAgain) {
          // Skip notification now. We try again.
          notifyFinish = false;
          // Try again.
          executeCommand();
        } else {
          event = Optional.of(new PushPullEvent(getOperation(), composeAndReturnFailureMessage(exMsg), e));
        }
      } catch (GitAPIException e) {
        // Exception handling.
        boolean shouldTryAgain = AuthUtil.handleAuthException(
            e,
            hostName,
            pluginWS::showErrorMessage,
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
        LOGGER.error(e.getMessage(), e);
      } finally {
        if (notifyFinish) {
          notifyListeners(event);
        }
      }
    }

    /**
     * Notify listeners.
     * 
     * @param event The event that happened.
     */
    private void notifyListeners(Optional<PushPullEvent> event) {
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

    /**
     * Treat transport exception.
     * 
     * @param e The exception.
     * 
     * @return <code>true</code> to try the operation again.
     */
    private boolean treatTransportException(TransportException e) {
      boolean tryAgainOutside = false;
      Throwable cause = e.getCause();
      if (cause instanceof NoRemoteRepositoryException) {
        tryAgainOutside  = new AddRemoteDialog().linkRemote();
      } else {
        String remoteURL = null;
        try {
          remoteURL = gitAccess.getRemoteURLFromConfig();
        } catch (NoRepositorySelected e1) {
          LOGGER.error(e1.getMessage(), e1);
        }

        PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
        String unableToAccessRepoMsg = translator.getTranslation(Tags.UNABLE_TO_ACCESS_REPO) + " " + remoteURL;
        if (getOperation() == GitOperation.PUSH ) {
          pluginWS.showErrorMessage(
              translator.getTranslation(Tags.PUSH_FAILED)
              + ". "
              + unableToAccessRepoMsg,
              e);
        } else {
          pluginWS.showErrorMessage(
              translator.getTranslation(Tags.PULL_FAILED)
              + ". "
              + unableToAccessRepoMsg,
              e);
        }
      }
      return tryAgainOutside;
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
        PluginWorkspaceProvider.getPluginWorkspace()
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
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(errMess);
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

      RepositoryState repositoryState = RepoUtil.getRepoState().orElse(null);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Do pull. Pull type: " + pullType);
        LOGGER.debug("Repo state: " + repositoryState);
      }

      if(repositoryState != null) {
        if (repositoryState == RepositoryState.MERGING_RESOLVED) {
          PluginWorkspaceProvider.getPluginWorkspace()
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
          PluginWorkspaceProvider.getPluginWorkspace()
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
