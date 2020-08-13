package com.oxygenxml.git.watcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.historycomponents.CommitsAheadAndBehind;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Tracks changes in the remote repository and notifies the user.
 */
public class RepositoryChangeWatcher {
  /**
   * Logger used to display exceptions.
   */
  private static Logger logger = Logger.getLogger(RepositoryChangeWatcher.class);
  
  /**
   * Sleeping time used to implement the coalescing.
   */
  private static final int SLEEP = 400;
  
  /**
   * Task for verifying and coalescing.
   */
  protected ScheduledFuture<?> future;

  /**
   * High level push and pull support.
   */
  private PushPullController pushPullController;
  
  /**
   * Stores the current working repository.
   */
  private Repository repository;

  /**
   * The Option Manager instance.
   */
  private OptionsManager optionsManager = OptionsManager.getInstance();
  
  /**
   * The Translator instance.
   */
  private Translator translator = Translator.getInstance();
  /**
   * Private constructor.
   * 
   * @param pushPullController High level push and pull support.
   */
  private RepositoryChangeWatcher(StandalonePluginWorkspace standalonePluginWorkspace, PushPullController pushPullController) {
    addListeners4EditingAreas(standalonePluginWorkspace);
    this.pushPullController = pushPullController;
    
    // Check the currently opened editors.
    boolean isNotifyAboutNewRemoteCommits = OptionsManager.getInstance().getNotifyAboutNewRemoteCommits();
    if(isNotifyAboutNewRemoteCommits) {
      GitOperationScheduler.getInstance().schedule(() -> checkRemoteRepository(true), 2 * SLEEP);
    }
  
  }
  
  /**
   * Adds hooks on the workspace and initializes the watcher which will notify the user when the remote 
   * repository changes.
   * 
   * @param standalonePluginWorkspace The Plugin Workspace.
   * @param pushPullController High level push and pull support.
   * 
   * @return An watcher that keeps track of the remote changes.
   */
  public static RepositoryChangeWatcher createWatcher(StandalonePluginWorkspace standalonePluginWorkspace, PushPullController pushPullController) {
    return new RepositoryChangeWatcher(standalonePluginWorkspace, pushPullController);
  }
  
  /**
   * Creates a new listener to supervise the remote changes and adds it to the editing areas
   * @param standalonePluginWorkspace The Plugin Workspace
   */
  private void addListeners4EditingAreas(StandalonePluginWorkspace standalonePluginWorkspace) {
    WSEditorChangeListener editorListenerAlways = new WSEditorChangeListener() {
      @Override
      public void editorOpened(URL editorLocation) {
        boolean isNotifyAboutNewRemoteCommits = OptionsManager.getInstance().getNotifyAboutNewRemoteCommits();
        if (isNotifyAboutNewRemoteCommits) {
          // Remote tracking is activated.
          // Cancel the previous scheduled task, if any, to implement coalescing.
          if (future != null) {
            future.cancel(false);
          }
          future = GitOperationScheduler.getInstance().schedule(() -> checkRemoteRepository(true), SLEEP);
        }
      }
    };
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, PluginWorkspace.MAIN_EDITING_AREA);
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, PluginWorkspace.DITA_MAPS_EDITING_AREA);
  }
  
  /**
   * The main task. Analyzes the remote repository to identify changes that are not in the local repository.
   * 
   * @param fetch <code>true</code> to execute a fetch before making the checks.
   */
  public void checkRemoteRepository(boolean fetch) {
    if (logger.isDebugEnabled()) {
      logger.debug("Handle notification mode");
    }

    List<RevCommit> commitsBehind = checkForRemoteCommits(fetch);

    if (!commitsBehind.isEmpty() && shouldNotifyUser(commitsBehind.get(0))) {
      notifyUserAboutNewCommits(commitsBehind);
    }
  }
  
  /**
   * Notifies the user about new commits in the remote repository and asks to pull
   * the changes
   * 
   * @param commitsBehind The new commits in remote repository.
   */
  private void notifyUserAboutNewCommits(List<RevCommit> commitsBehind) {
    // Remember that we warn the user about this particular commit.
    optionsManager.setWarnOnChangeCommitId(repository.getIdentifier(), commitsBehind.get(0).name());

    // Notify new commit in remote.
    showNewCommitsInRemoteMessage(translator.getTranslation(Tags.NEW_COMMIT_UPSTREAM) + " "
        + translator.getTranslation(Tags.WANT_TO_PULL_QUESTION));
  }
  
  /**
   * Present to the user a notification about the state of the remote repository
   * and asks the user if he wants to pull the changes.
   * 
   * @param message The message to be displayed to the user
   */
  private void showNewCommitsInRemoteMessage(String message) {
    String[] options = { translator.getTranslation(Tags.PULL_CHANGES), translator.getTranslation(Tags.CLOSE) };
    int okPressed = 1;
    int[] optionsIds = { okPressed, 0 };

    if (PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
        translator.getTranslation(Tags.REMOTE_CHANGES_LABEL), message, options, optionsIds) == okPressed) {
      pushPullController.pull();
    }
  }
  

  /**
   * Checks if the user should receive a notification regarding the remote
   * repository state. \nCompares the two commit IDs that come from
   * {@link org.eclipse.jgit.revwalk.RevCommit.getId().getName()}, and in case of
   * inequality, informs that there are new commits by returning true.
   * 
   * @param topRevCommit The newest commit fetched from upstream.
   * 
   * @return <code>true</code> to present a notification to the user.
   */
  private boolean shouldNotifyUser(RevCommit topRevCommit) {
    String commitId = topRevCommit.getId().getName();
    
    return !commitId.contentEquals(optionsManager.getWarnOnChangeCommitId(repository.getIdentifier()));
  }
  
  /**
   * Checks in the remote repository if there are new commits. 
   * @param fetch <code>true</code> to execute a fetch before making the checks.
   * 
   * @return <code>commitsAhead</code> a list with all new commits
   */
  private List<RevCommit> checkForRemoteCommits(boolean fetch) {
    List<RevCommit> commitsBehind = Collections.emptyList();
    try {
      GitAccess gitAccess = GitAccess.getInstance();
      if (fetch) {
        gitAccess.fetch();
      }
      repository = gitAccess.getRepository();
      CommitsAheadAndBehind commitsAheadAndBehind = RevCommitUtil.getCommitsAheadAndBehind(repository, repository.getFullBranch());
      if (commitsAheadAndBehind != null) {
        commitsBehind = commitsAheadAndBehind.getCommitsBehind();
      }
    } catch (NoRepositorySelected | IOException | SSHPassphraseRequiredException | PrivateRepositoryException
        | RepositoryUnavailableException e) {
      logger.debug(e, e);
    }
    return commitsBehind;
  }
  
}
