package com.oxygenxml.git.view.event;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.errors.CanceledException;
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
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.exceptions.IndexLockExistsException;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RebaseConflictsException;
import com.oxygenxml.git.service.exceptions.RebaseUncommittedChangesException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.actions.IProgressUpdater;
import com.oxygenxml.git.view.branches.BranchCheckoutMediator;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.RebaseInProgressDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.util.ExceptionHandlerUtil;

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
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * The mediator for branches checkout.
   */
  private BranchCheckoutMediator branchesCheckoutMediator;
  
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
   * @param message          An optional message about the operation.
   * @param command          The command runnable to execute.
   * @param progressMonitor  The optional progress manager.
   *
   * @return The result of the operation execution.
   */
  private Future<?> execute(String message, ExecuteCommandRunnable command, Optional<IGitViewProgressMonitor> progressMonitor) {
    PushPullEvent pushPullEvent = new PushPullEvent(command.getOperation(), message);
    try {
      listeners.fireOperationAboutToStart(pushPullEvent);
      progressMonitor.ifPresent(pm -> pm.showWithDelay(IProgressUpdater.DEFAULT_OPERATION_DELAY));
    } catch (IndexLockExistsException e) {
      // Ignore. We already had a mechanism for pull. Let it do its job.
      // The old mechanism also seems to update the status at the bottom of the Git Staging view.
    }

    return GitOperationScheduler.getInstance().schedule(command);
  }

  /**
   * @param branchesCheckoutMediator The new branches checkout mediator responsible with branches checkout.
   */
  public void setBranchesCheckoutMediator(@NonNull BranchCheckoutMediator branchesCheckoutMediator) {
    this.branchesCheckoutMediator = branchesCheckoutMediator;
  }
  
  /**
   * @return The branches checkout mediator responsible with branches checkout.
   */
  @NonNull
  public BranchCheckoutMediator getBranchesCheckoutMediator() {
    return branchesCheckoutMediator;
  }
  
  /**
   * Push.
   * 
   * @param progressMonitor  The optional progress manager.
   *
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> push(Optional<IGitViewProgressMonitor> progressMonitor) {
    return execute(TRANSLATOR.getTranslation(Tags.PUSH_IN_PROGRESS), new ExecutePushRunnable(progressMonitor), progressMonitor);
  }

  /**
   * Pull.
   * 
   * @param progressMonitor    Receive the progress of the current operation.
   * 
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> pull(Optional<IGitViewProgressMonitor> progressMonitor) {
    return	pull(PullType.MERGE_FF, progressMonitor);
  }

  /**
   * Pull and choose the merging strategy.
   * 
   * @param pullType           The pull type / merging strategy.
   * @param progressMonitor    Receive the progress of the current operation.
   * 
   * @return The result of the operation execution.
   */
  @SuppressWarnings("java:S1452")
  public Future<?> pull(PullType pullType, Optional<IGitViewProgressMonitor> progressMonitor) {
    return execute(TRANSLATOR.getTranslation(Tags.PULL_IN_PROGRESS), 
      new ExecutePullRunnable(pullType, progressMonitor), progressMonitor);
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
        TRANSLATOR.getTranslation(Tags.PULL_STATUS), DialogType.WARNING)
        .setTargetResourcesWithTooltips(FileStatusUtil.comuteFilesTooltips(conflictingFilesList))
        .setMessage(TRANSLATOR.getTranslation(Tags.PULL_SUCCESSFUL_CONFLICTS))
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
      LOGGER.debug("Pull failed with the following message: {}. Resources: {}", message, filesWithChanges);
    }
    SwingUtilities.invokeLater(() -> MessagePresenterProvider.getBuilder(
        TRANSLATOR.getTranslation(Tags.PULL_STATUS), DialogType.ERROR)
        .setTargetResourcesWithTooltips(FileStatusUtil.comuteFilesTooltips(filesWithChanges))
        .setMessage(message)
        .setCancelButtonVisible(false)
        .buildAndShow());
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
     * @return The optional progress monitor for the operation.
     */
    protected abstract Optional<IGitViewProgressMonitor> getProgressMonitor();

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
      } catch(CanceledException e) {
        event = Optional.of(new PushPullEvent(getOperation(), null, e));
      } catch (JGitInternalException e) {
        event = treatJGitInternalException(e);
      } catch (RebaseUncommittedChangesException e) {
        event = Optional.of(new PushPullEvent(getOperation(), null, e));
        showPullFailedBecauseOfCertainChanges(
            e.getUncommittedChanges(),
            TRANSLATOR.getTranslation(Tags.PULL_REBASE_FAILED_BECAUSE_UNCOMMITTED));
      } catch (RebaseConflictsException e) {
        event = Optional.of(new PushPullEvent(getOperation(), null, e));
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
                MessageFormat.format(TRANSLATOR.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                        TRANSLATOR.getTranslation(Tags.REBASE))
        );
      } catch (CheckoutConflictException e) {
        event = Optional.of(new PushPullEvent(getOperation(), null, e));
        showPullFailedBecauseOfCertainChanges(
            e.getConflictingPaths(),
                MessageFormat.format(TRANSLATOR.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                        TRANSLATOR.getTranslation(Tags.MERGE).toLowerCase())
        );
      } catch (TransportException e) {
        String exMsg = e.getMessage();
        boolean shouldTryAgain = isAuthProblem(exMsg) ? 
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
          event.ifPresent(pullPushEvent -> {
            getProgressMonitor().ifPresent(pm -> {
              if(pullPushEvent.getCause() != null) {
                pm.markAsFailed();
              } else {
                pm.markAsCompleted();
              }
            });
          });
          notifyListeners(event);
        }
      }
    }

    /**
     * Check if the given message represents an authentication problem.
     * 
     * @param exMsg The message.
     * 
     * @return <code>true</code> if it is an authentication problem.
     */
    private boolean isAuthProblem(String exMsg) {
      return exMsg.contains(AuthUtil.NOT_AUTHORIZED)
          || exMsg.contains(AuthUtil.AUTHENTICATION_NOT_SUPPORTED)
          || exMsg.contains(AuthUtil.NOT_PERMITTED);
    }
    
    /**
     * Notify listeners.
     * 
     * @param event The event that happened.
     */
    private void notifyListeners(Optional<PushPullEvent> event) {
      PushPullEvent toFire = event.isPresent() ? event.get() : new PushPullEvent(getOperation(), "");
      if (toFire.getCause() != null) {
        listeners.fireOperationFailed(toFire, toFire.getCause());
      } else {
        listeners.fireOperationSuccessfullyEnded(toFire);
      }
    }

    /**
     * Treat an internal exception thrown by JGit. Look at its cause.
     * 
     * @param e The exception to treat.
     * 
     * @return an optional event containing details about to what happened to the push/pull operation
     * after treating the exception.
     */
    private Optional<PushPullEvent> treatJGitInternalException(JGitInternalException e) {
      LOGGER.debug(e.getMessage(), e);
      
      Optional<PushPullEvent> event = Optional.empty();
      
      PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();

      Throwable cause = e.getCause();
      if(ExceptionHandlerUtil.hasCauseOfType(e, CanceledException.class)) {
        event = Optional.of(new PushPullEvent(getOperation(), cause.getMessage()));
      } else if (cause instanceof org.eclipse.jgit.errors.CheckoutConflictException) {
        String[] conflictingFile = ((org.eclipse.jgit.errors.CheckoutConflictException) cause).getConflictingFiles();
        showPullFailedBecauseOfCertainChanges(
            Arrays.asList(conflictingFile),
            MessageFormat.format(TRANSLATOR.getTranslation(Tags.PULL_FAILED_BECAUSE_CONFLICTING_PATHS),
                    TRANSLATOR.getTranslation(Tags.REBASE))
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
              TRANSLATOR.getTranslation(Tags.PUSH_FAILED) + ": " 
                  + MessageFormat.format(
                      TRANSLATOR.getTranslation(Tags.UNBORN_BRANCH),
                      gitAccess.getBranchInfo().getBranchName()) + " "
                  + TRANSLATOR.getTranslation(Tags.COMMIT_BEFORE_PUSHING),
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
      return event;
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
        String unableToAccessRepoMsg = TRANSLATOR.getTranslation(Tags.UNABLE_TO_ACCESS_REPO) + " " + remoteURL;
        if (getOperation() == GitOperation.PUSH ) {
          pluginWS.showErrorMessage(
              TRANSLATOR.getTranslation(Tags.PUSH_FAILED)
              + ". "
              + unableToAccessRepoMsg,
              e);
        } else {
          pluginWS.showErrorMessage(
              TRANSLATOR.getTranslation(Tags.PULL_FAILED)
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
    
    /**
     * The progress monitor.
     */
    private final Optional<IGitViewProgressMonitor> pm;
    
    /**
     * Constructor.
     * 
     * @param pm The progress monitor.
     */
    public ExecutePushRunnable(Optional<IGitViewProgressMonitor> pm) {
      this.pm = pm;
    }

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
        event = new PushPullEvent(GitOperation.PUSH, TRANSLATOR.getTranslation(Tags.PUSH_SUCCESSFUL));
      } else if (Status.REJECTED_NONFASTFORWARD == response.getStatus()) {
        PluginWorkspaceProvider.getPluginWorkspace()
        .showErrorMessage(TRANSLATOR.getTranslation(Tags.BRANCH_BEHIND));
      } else if (Status.UP_TO_DATE == response.getStatus()) {
        event = new PushPullEvent(GitOperation.PUSH, TRANSLATOR.getTranslation(Tags.PUSH_UP_TO_DATE));
      } else if (Status.REJECTED_OTHER_REASON == response.getStatus()) {
        String errMess = TRANSLATOR.getTranslation(Tags.PUSH_FAILED_UNKNOWN);
        if (response.getMessage() != null) {
          String details = response.getMessage();
          if (details.contains("pre-receive hook declined")) {
            details = TRANSLATOR.getTranslation(Tags.PRE_RECEIVE_HOOK_DECLINED_CUSTOM_MESSAGE);
          }
          errMess += " " + details;
        } else {
          errMess += " " + TRANSLATOR.getTranslation(Tags.NO_DETAILS_AVAILABLE);
        }
        PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(errMess);
      }

      return Optional.ofNullable(event);
    }

    @Override
    protected String composeAndReturnFailureMessage(String message) {
      return TRANSLATOR.getTranslation(Tags.PUSH_FAILED) + ": " + message;
    }

    @Override
    protected Optional<IGitViewProgressMonitor> getProgressMonitor() {
      return pm;
    }
  }

  /**
   * Execute command runnable.
   */
  private class ExecutePullRunnable extends ExecuteCommandRunnable {

    private final PullType pullType;
    
    private final Optional<IGitViewProgressMonitor> progressMonitor;

    public ExecutePullRunnable(PullType pullType, Optional<IGitViewProgressMonitor> progressMonitor) {
      this.pullType = pullType;
      this.progressMonitor = progressMonitor;
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
        LOGGER.debug("Do pull. Pull type: {}", pullType);
        LOGGER.debug("Repo state: {}", repositoryState);
      }

      if(repositoryState != null) {
        if (repositoryState == RepositoryState.MERGING_RESOLVED) {
          PluginWorkspaceProvider.getPluginWorkspace()
              .showWarningMessage(TRANSLATOR.getTranslation(Tags.CONCLUDE_MERGE_MESSAGE));
        } else if (repositoryState == RepositoryState.REBASING_MERGE
            || repositoryState == RepositoryState.REBASING_REBASING) {
          showRebaseInProgressDialog();
        } else {
          PullResponse response = gitAccess.pull(
              credentialsProvider,
              pullType,
              progressMonitor,
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
          event = new PushPullEvent(GitOperation.PULL, TRANSLATOR.getTranslation(Tags.PULL_SUCCESSFUL));
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
              .showErrorMessage(TRANSLATOR.getTranslation(Tags.PULL_WHEN_REPO_IN_CONFLICT));
          break;
        case UP_TO_DATE:
          event = new PushPullEvent(GitOperation.PULL, TRANSLATOR.getTranslation(Tags.PULL_UP_TO_DATE));
          break;
        case LOCK_FAILED:
          event = new PushPullEvent(GitOperation.PULL, TRANSLATOR.getTranslation(Tags.LOCK_FAILED));
          break;
        default:
          // Nothing
          break;
      }
      return event;
    }

    @Override
    protected String composeAndReturnFailureMessage(String message) {
      return TRANSLATOR.getTranslation(Tags.PULL_FAILED) + ": " + message;
    }

    @Override
    protected Optional<IGitViewProgressMonitor> getProgressMonitor() {
      return progressMonitor;
    }
  }
 
}
