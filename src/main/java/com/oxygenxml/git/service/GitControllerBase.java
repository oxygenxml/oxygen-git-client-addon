package com.oxygenxml.git.service;

import java.text.MessageFormat;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * 
 * Executes Git commands. A higher level wrapper over the GitAccess.
 *  
 * @author Beniamin Savu
 */
public abstract class GitControllerBase {
  /**
   * Translator for the UI.
   */
  private Translator translator = Translator.getInstance();
  /**
   * Access to the Git API.
   */
  protected GitAccess gitAccess;
  /**
   * Events issued by the git access. We use it to identify skipped fail events.
   */
  private Deque<GitEventInfo> events = new LinkedList<>();
  /**
   * <code>true</code> to disable event tracking.
   */
  private boolean skipEventTracking = false;
  /**
   * Git notifications.
   */
  protected GitListeners listeners = GitListeners.getInstance();

  /**
   * Constructor.
   * 
   * @param access Access to the Git API.
   */
  protected GitControllerBase(GitAccess access) {
    this.gitAccess = access;
    
    addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        if (!skipEventTracking) {
          events.push(info);
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if (!skipEventTracking && !events.isEmpty()) {
          events.pop();
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if (!skipEventTracking) {
          events.pop();
        }
      }
    });
  }

  /**
   * Resolves the conflict state for the files by keeping the local version.
   * 
   * @param conflictFiles Conflict files.
   */
  public void asyncResolveUsingMine(List<FileStatus> conflictFiles) {
    async(() ->  {
      if (shouldContinueResolvingConflictUsingMineOrTheirs(ConflictResolution.RESOLVE_USING_MINE)) {
        resolveUsingMine(conflictFiles);
      }
    });
  }

  /**
   * Resolves the conflict state for the files by keeping the remote version.
   * 
   * @param conflictFiles Conflict files.
   */
  public void asyncResolveUsingTheirs(List<FileStatus> conflictFiles) {
    async(() ->  {
      if (shouldContinueResolvingConflictUsingMineOrTheirs(ConflictResolution.RESOLVE_USING_THEIRS)) {
        resolveUsingTheirs(conflictFiles);
      }
    });
  }

  /**
   * Runs a task on the Git operation thread.
   * 
   * @param r Git Task.
   * 
   * @return A future monitoring the orginal task.
   */
  @SuppressWarnings("java:S1452")
  ScheduledFuture<?> async(Runnable r) {
    return async(r, null);
  }

  /**
   * Runs a task on the Git operation thread.
   * 
   * @param r Git Task.
   * @param errorHandler Error handler.
   * 
   * @return A future monitoring the orginal task.
   */
  @SuppressWarnings("java:S1452")
  private ScheduledFuture<?> async(Runnable r, Consumer<Throwable> errorHandler) {
    return GitOperationScheduler.getInstance().schedule(r, t -> {
      consumeEvents(t);
      if (errorHandler != null) {
        errorHandler.accept(t);
      }
    });
  }

  /**
   * Runs a task on the Git operation thread.
   * 
   * @param <T> The type of the task result.
   * @param task Git Task.
   * @param errorHandler Error handler.
   * 
   * @return A future monitoring the orginal task.
   */
  @SuppressWarnings("java:S1452")
  public <T> ScheduledFuture<?> asyncTask(Callable<T> task, Consumer<Throwable> errorHandler) {
    return asyncTask(task, v -> {}, errorHandler);
  }

  /**
   * Runs a task on the Git operation thread. Exceptions should be handled in the errorHandler parameter.
   * 
   * @param <T> The type of the callable result.
   * @param callable Git related instructions. <b>Do not catch Git exceptions. Intercept them in the errorHandler</b>
   * @param resultHandler Receives a notification when the result of the task is completed.
   * @param errorHandler Notified when the task throws exceptions.
   * 
   * @return A future monitoring the original task.
   */
  @SuppressWarnings("java:S1452")
  public <T> ScheduledFuture<?> asyncTask(Callable<T> callable, Consumer<T> resultHandler, Consumer<Throwable> errorHandler) {
    return GitOperationScheduler.getInstance().schedule(callable, resultHandler, t -> {
      consumeEvents(t);
      if (errorHandler != null) {
        errorHandler.accept(t);
      }
    });
  }

  /**
   * An exception was intercepted. Make sure we notify any started() events that haven't been notified of failed().
   * 
   * @param t Caught exception.
   */
  private void consumeEvents(Throwable t) {
    if (!events.isEmpty()) {
      skipEventTracking = true;
      try {
        // Operations in progress, no failed issued for them.
        while (!events.isEmpty()) {
          GitEventInfo pop = events.pop();

          gitAccess.fireOperationFailed(pop, t);
        }
      } finally {
        skipEventTracking = false;
      }
    }
  }


  /**
   * Adds multiple files to the staging area.
   * 
   * @param filesStatuses Files to add.
   */
  public void asyncAddToIndex(List<FileStatus> filesStatuses) {
    async(() -> gitAccess.addAll(filesStatuses));
  }

  /**
   * Reset all the specified files from the staging area.
   * 
   * @param filesStatuses Files to add.
   */
  public void asyncReset(List<FileStatus> filesStatuses) {
    async(() -> gitAccess.resetAll(filesStatuses));
  }

  /**
   * Discard files.
   * 
   * @param filesStatuses The resources to discard.
   */
  public void asyncDiscard(List<FileStatus> filesStatuses) {
    async(() -> discard(filesStatuses));
  }

  /**
   * Should continue resolving a conflict using 'mine' or 'theirs'.
   * 
   * @param cmd ConflictResolution.RESOLVE_USING_MINE or ConflictResolution.RESOLVE_USING_THEIRS.
   * 
   * @return <code>true</code> to continue resolving the conflict using 'mine' or 'theirs'.
   */
  private boolean shouldContinueResolvingConflictUsingMineOrTheirs(ConflictResolution cmd) {
    boolean shouldContinue = false;
    RepositoryState repoState = RepoUtil.getRepoState();
    if (repoState != null && repoState != RepositoryState.REBASING_MERGE
        // When having a conflict while rebasing, 'mine' and 'theirs' are switched.
        // Tell this to the user and ask if they are OK with their choice.
        || isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(cmd)) {
      shouldContinue = true;
    }
    return shouldContinue;
  }

  /**
   * Ask the user if they are OK with continuing the conflict resolving.
   * When having a conflict while rebasing, 'mine' and 'theirs' are reversed.
   * Tell this to the user and ask if they are OK with their choice.
   * 
   * @param cmd {@link GitOperation#RESOLVE_USING_MINE} or
   *  {@link GitOperation#RESOLVE_USING_THEIRS}.
   * 
   * @return <code>true</code> to continue.
   */
  protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
    boolean isResolveUsingMine = cmd == ConflictResolution.RESOLVE_USING_MINE;
    String actionName = isResolveUsingMine ? translator.getTranslation(Tags.RESOLVE_USING_MINE)
        : translator.getTranslation(Tags.RESOLVE_USING_THEIRS);
    String side = isResolveUsingMine ? translator.getTranslation(Tags.MINE)
        : translator.getTranslation(Tags.THEIRS);
    String branch = isResolveUsingMine ? translator.getTranslation(Tags.THE_UPSTREAM_BRANCH)
        : translator.getTranslation(Tags.THE_WORKING_BRANCH);
    String[] options = new String[] { 
        "   " + translator.getTranslation(Tags.YES) + "   ",
        "   " + translator.getTranslation(Tags.NO) + "   "};
    int[] optionIds = new int[] { 0, 1 };
    int result = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
        actionName,
        MessageFormat.format(
            translator.getTranslation(Tags.CONTINUE_RESOLVING_REBASE_CONFLICT_USING_MINE_OR_THEIRS),
            side,
            branch),
        options,
        optionIds);
    return result == optionIds[0];
  }

  /**
   * Resolve using 'Mine'.
   * 
   * @param filesStatuses The resources to resolve.
   */
  private void resolveUsingMine(List<FileStatus> filesStatuses) {
    discard(filesStatuses);
    gitAccess.addAll(filesStatuses);
  }

  /**
   * Resolve using 'Theirs'.
   * 
   * @param filesStatuses The resources to resolve.
   */
  private void resolveUsingTheirs(List<FileStatus> filesStatuses) {
    for (FileStatus file : filesStatuses) {
      gitAccess.replaceWithRemoteContent(file.getFileLocation());
    }
    gitAccess.addAll(filesStatuses);
  }

  /**
   * Discard files.
   * 
   * @param filesStatuses The resources to discard.
   */
  private void discard(List<FileStatus> filesStatuses) {
    gitAccess.resetAll(filesStatuses);
    List<String> paths = new LinkedList<>();
    for (FileStatus file : filesStatuses) {
      if (file.getChangeType() != GitChangeType.SUBMODULE) {
        paths.add(file.getFileLocation());
      }
    }
    gitAccess.restoreLastCommitFile(paths);
  }

  /**
   * Add a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to add.
   */
  public void addGitListener(GitEventListener listener) {
    listeners.addGitListener(listener);
  }

  /**
   * Removes a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to remove.
   */
  public void removeGitListener(GitEventAdapter listener) {
    listeners.removeGitListener(listener);
  }

  /**
   * @return Low level operation controller.
   */
  public GitAccess getGitAccess() {
    return gitAccess;
  }
}
