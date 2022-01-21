package com.oxygenxml.git.service;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitsAheadAndBehind;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Tracks changes in the remote repository and notifies the user.
 */
public class RemoteRepositoryChangeWatcher {
  /**
   * Logger used to display exceptions.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRepositoryChangeWatcher.class);
  
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
  private GitController gitController;
  
  /**
   * The Option Manager instance.
   */
  private static OptionsManager optionsManager = OptionsManager.getInstance();
  
  /**
   * The Translator instance.
   */
  private Translator translator = Translator.getInstance();
  /**
   * Private constructor.
   * 
   * @param pluginWS Access to the workspace.
   * @param gitCtrl  High level Git commands support.
   */
  private RemoteRepositoryChangeWatcher(PluginWorkspace pluginWS, GitController gitCtrl) {
    addListeners4EditingAreas(pluginWS);
    this.gitController = gitCtrl;
    
    // Check the currently opened editors.
    boolean isNotifyAboutNewRemoteCommits = OptionsManager.getInstance().isNotifyAboutNewRemoteCommits();
    if(isNotifyAboutNewRemoteCommits) {
      GitOperationScheduler.getInstance().schedule(() -> checkRemoteRepository(true), 2 * SLEEP);
    }
  
  }
  
  /**
   * Adds hooks on the workspace and initializes the watcher which will notify the user when the remote 
   * repository changes.
   * 
   * @param saPluginWS  The Plugin Workspace.
   * @param gitCtrl     High level push and pull support.
   * 
   * @return An watcher that keeps track of the remote changes.
   */
  public static RemoteRepositoryChangeWatcher createWatcher(PluginWorkspace saPluginWS, GitController gitCtrl) {
    return new RemoteRepositoryChangeWatcher(saPluginWS, gitCtrl);
  }
  
  /**
   * Creates a new listener to supervise the remote changes and adds it to the editing areas
   * @param standalonePluginWorkspace The Plugin Workspace
   */
  private void addListeners4EditingAreas(PluginWorkspace standalonePluginWorkspace) {
    WSEditorChangeListener editorListenerAlways = new WSEditorChangeListener() {
      @Override
      public void editorOpened(URL editorLocation) {
        boolean isNotifyAboutNewRemoteCommits = OptionsManager.getInstance().isNotifyAboutNewRemoteCommits();
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Handle notification mode");
    }

    List<RevCommit> commitsBehind = checkForRemoteCommits(fetch);

    if (!commitsBehind.isEmpty() && shouldNotifyUser(commitsBehind.get(0))) {
      notifyUserAboutNewCommits(commitsBehind);
    }
  }
  
  /**
   * Marks the user as already notified after a reset to a specific commit has
   * happened.
   */
  public static void markAsNotified() {
    Repository repository;
    try {
      repository = GitAccess.getInstance().getRepository();
      List<RevCommit> commitsBehind = checkForRemoteCommits(false);
      if (!commitsBehind.isEmpty()) {
        optionsManager.setWarnOnChangeCommitId(repository.getIdentifier(), commitsBehind.get(0).name());
      }
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
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
    Repository repository = null;
    try {
      repository = GitAccess.getInstance().getRepository();
      optionsManager.setWarnOnChangeCommitId(repository.getIdentifier(), commitsBehind.get(0).name());
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
    }
    String workTree = repository != null ? repository.getWorkTree().getAbsolutePath() : "";
    String remoteRepoUrl = repository != null ? repository.getConfig().getString(
        ConfigConstants.CONFIG_KEY_REMOTE, Constants.DEFAULT_REMOTE_NAME, "url") : "";
    String branch = "";
    try {
      branch = repository != null ? repository.getBranch() : "";
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    String remoteBranch = GitAccess.getInstance().getUpstreamBranchShortNameFromConfig(branch);
    
    showNewCommitsInRemoteMessage(
        translator.getTranslation(Tags.NEW_COMMIT_UPSTREAM)
            + "\n"
            + translator.getTranslation(Tags.PULL_REMOTE_CHANGED_RECOMMENDATION)
            + "\n\n"
            + translator.getTranslation(Tags.REMOTE_REPO_URL) + " " + remoteRepoUrl + "\n"
            + translator.getTranslation(Tags.REMOTE_BRANCH) + " " + remoteBranch + "\n\n"
            + translator.getTranslation(Tags.WORKING_COPY_LABEL) + " " + workTree + "\n"
            + translator.getTranslation(Tags.LOCAL_BRANCH) + " " + branch + "\n\n"
            + translator.getTranslation(Tags.WANT_TO_PULL_QUESTION));
  }
  
  /**
   * Present to the user a notification about the state of the remote repository
   * and asks the user if he wants to pull the changes.
   * 
   * @param message The message to be displayed to the user
   */
  private void showNewCommitsInRemoteMessage(String message) {
    if (FileStatusDialog.showInformationMessage(
        translator.getTranslation(Tags.REMOTE_CHANGES_LABEL), 
        message,
        translator.getTranslation(Tags.PULL_CHANGES),
        translator.getTranslation(Tags.CLOSE)) == OKCancelDialog.RESULT_OK) {
      gitController.pull();
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
    Repository repository;
    try {
      repository = GitAccess.getInstance().getRepository();
      return !commitId.contentEquals(optionsManager.getWarnOnChangeCommitId(repository.getIdentifier()));
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e.getMessage(), e);
    }
    return false;
  }
  
  /**
   * Checks in the remote repository if there are new commits. 
   * @param fetch <code>true</code> to execute a fetch before making the checks.
   * 
   * @return <code>commitsAhead</code> a list with all new commits
   */
  private static List<RevCommit> checkForRemoteCommits(boolean fetch) {
    List<RevCommit> commitsBehind = Collections.emptyList();
    try {
      GitAccess gitAccess = GitAccess.getInstance();
      if (fetch) {
        gitAccess.fetch();
      }
      Repository repository = gitAccess.getRepository();
      CommitsAheadAndBehind commitsAheadAndBehind = RevCommitUtil.getCommitsAheadAndBehind(repository, repository.getFullBranch());
      if (commitsAheadAndBehind != null) {
        commitsBehind = commitsAheadAndBehind.getCommitsBehind();
      }
    } catch (NoRepositorySelected | IOException | SSHPassphraseRequiredException | PrivateRepositoryException
        | RepositoryUnavailableException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    return commitsBehind;
  }
  
}
