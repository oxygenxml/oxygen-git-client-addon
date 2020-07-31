package com.oxygenxml.git.watcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PrivateRepositoryException;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;
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
   * The Plugin Workspace.
   */
  private PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
  
  /**
   * The Option Manager instance.
   */
  private OptionsManager optionsManager = OptionsManager.getInstance();
  
  /**
   * The Git Access instance.
   */
  private GitAccess gitAccess = GitAccess.getInstance();
  
  /**
   * The Translator instance.
   */
  private Translator translator = Translator.getInstance();
  /**
   * Private constructor.
   */
  private RepositoryChangeWatcher(StandalonePluginWorkspace standalonePluginWorkspace) {
    addListeners4EditingAreas(standalonePluginWorkspace);
    
    // Check the currently opened editors.
    String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
    GitOperationScheduler.getInstance().schedule(() -> checkRemoteRepository(value), SLEEP);
  
  }
  
  /**
   * Adds hooks on the workspace and initializes the watcher which will notify the user when the remote 
   * repository changes.
   * 
   * @param standalonePluginWorkspace The Plugin Workspace
   * 
   * @return An watcher that keeps track of the remote changes.
   */
  public static RepositoryChangeWatcher createWatcher(StandalonePluginWorkspace standalonePluginWorkspace) {
    return new RepositoryChangeWatcher(standalonePluginWorkspace);
  }
  
  /**
   * Creates a new listener to supervise the remote changes and adds it to the editing areas
   * @param standalonePluginWorkspace The Plugin Workspace
   */
  public void addListeners4EditingAreas(StandalonePluginWorkspace standalonePluginWorkspace) {
    WSEditorChangeListener editorListenerAlways = new WSEditorChangeListener() {
      @Override
      public void editorOpened(URL editorLocation) {
        String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
        if (value.equals(RemoteTrackingAction.WARN_UPSTREAM_ALWAYS)
            || value.equals(RemoteTrackingAction.WARN_UPSTREAM_ON_CHANGE)) {
          // Remote tracking is activated.
          // Cancel the previous scheduled task, if any, to implement coalescing.
          if (future != null) {
            future.cancel(false);
          }
          future = GitOperationScheduler.getInstance().schedule(() -> checkRemoteRepository(value), SLEEP);
        }
      }
    };
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.MAIN_EDITING_AREA);
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA);
  }
  
  /**
   * The main task. Analyzes the remote repository to identify changes that are not in the local repository.
   * 
   * @param notifyMode One of {@link RemoteTrackingAction}. Controls when and if the user receives a notification.
   */
  public void checkRemoteRepository(String notifyMode) {
    if (logger.isDebugEnabled()) {
      logger.debug("Handle notification mode: " + notifyMode);
    }
    
    if (notifyMode.equals(RemoteTrackingAction.WARN_UPSTREAM_ALWAYS)
        || notifyMode.equals(RemoteTrackingAction.WARN_UPSTREAM_ON_CHANGE)) {
      List<RevCommit> commitsBehind = checkForRemoteCommits();
      
      if (!commitsBehind.isEmpty() && shouldNotifyUser(commitsBehind.get(0))) {
        if (notifyMode.equals(RemoteTrackingAction.WARN_UPSTREAM_ALWAYS)) {
          // Remember that we warn the user about this particular commit.
          optionsManager.setWarnOnCommitIdChange(commitsBehind.get(0).name());
          
          // notify new commit in remote
          pluginWorkspace.showInformationMessage(Translator.getInstance().getTranslation(Tags.NEW_COMMIT_UPSTREAM));
        } else if (notifyMode.equals(RemoteTrackingAction.WARN_UPSTREAM_ON_CHANGE)) {
          Set<String> conflictingFiles = checkForRemoteFileChanges(getFilesOpenedInEditors(), commitsBehind);
          if (!conflictingFiles.isEmpty()) {
            // Remember that we warn the user about this particular commit.
            optionsManager.setWarnOnCommitIdChange(commitsBehind.get(0).name());
            
            showNewCommitsInRemoteMessage(translator.getTranslation(Tags.NEW_COMMIT_WITH_MODIFIED_OPENED_FILES)
                + getFilesModified(conflictingFiles));
          }
        }
      }
    }
  }
  
  /**
   * present to the user a notification aboutt the state of the remote repository and asks the user if he wants to pull
   *  the changes.
   * 
   * @param message The message to be displayed to the user
   */
  private void showNewCommitsInRemoteMessage(String message) {
    String[] options = { translator.getTranslation(Tags.YES), translator.getTranslation(Tags.NO) };
    int[] optionsIds = { 1, 0 };
    StringBuilder fullMessage = new StringBuilder(message);
    fullMessage.append("\n\n");
    fullMessage.append(translator.getTranslation(Tags.WANT_TO_PULL_QUESTION));

    if (PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
        translator.getTranslation(Tags.COMMIT_MESSAGE_LABEL), fullMessage.toString(), options, optionsIds) == 1) {
      String hostName = gitAccess.getHostName();
      UserCredentials userCredentials = optionsManager.getGitCredentials(hostName);
      try {
        gitAccess.pull(userCredentials.getUsername(), userCredentials.getPassword());
      } catch (GitAPIException e) {
        pluginWorkspace.showInformationMessage(e.getMessage());
        logger.error(e, e);
      }
    }
  }
  
  /**
   * Checks if the given local files were changed in the commits detected in the remote repository.
   * 
   * @param localFiles Local files paths, relative to the working copy directory.
   * @param commitsBehind A list of new commits from remote that haven't been pulled yet into the local.
   * 
   * @return All local files modified in the remote.
   */
  private Set<String> checkForRemoteFileChanges(
      Set<String> localFiles,
      List<RevCommit> commitsBehind) {
    HashSet<String> changedRemoteFiles = new HashSet<>();

    List<CommitCharacteristics> commitsCharacteristics = RevCommitUtil.createRevCommitCharacteristics(commitsBehind);

    for (CommitCharacteristics commitsCharacteristicsIterator : commitsCharacteristics) {
      String commitId = commitsCharacteristicsIterator.getCommitId();
      try {
        List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitId);
        
        for (FileStatus changedFilesIterator : changedFiles) {
          String fileLocation = changedFilesIterator.getFileLocation();
          if (localFiles.contains(fileLocation)) {
            changedRemoteFiles.add(fileLocation);
          }
        }
      } catch (IOException | GitAPIException e) {
        logger.error(e, e);
      }
    }
    return changedRemoteFiles;
  }

  /**
   * Checks if the user should receive a notification regarding the remote repository state.
   * 
   * @param topRevCommit The newest commit fetched from upstream.
   * 
   * @return <code>true</code> to present a notification to the user.
   */
  private boolean shouldNotifyUser(RevCommit topRevCommit) {
    String commitId = topRevCommit.getId().getName();
    
    return !commitId.contentEquals(optionsManager.getWarnOnCommitIdChange());
  }
  
  /**
   * Retrieves all the files opened in dita maps and main editing areas.
   * 
   * @return Paths relative to the working copy directory.
   */
  private HashSet<String> getFilesOpenedInEditors() {
    HashSet<String> changedLocalFiles = new HashSet<>();
    
    try {
      URL wcDir = pluginWorkspace.getUtilAccess().convertFileToURL(GitAccess.getInstance().getWorkingCopy());
      
      collectFilesFromRepository(
          wcDir, 
          pluginWorkspace.getAllEditorLocations(StandalonePluginWorkspace.MAIN_EDITING_AREA), 
          changedLocalFiles);
      collectFilesFromRepository(
          wcDir, 
          pluginWorkspace.getAllEditorLocations(StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA),
          changedLocalFiles);
    } catch (MalformedURLException | NoRepositorySelected e) {
      logger.debug(e, e);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("Repository files open in the editor: " + changedLocalFiles);
    }

    return changedLocalFiles;
  }
  
  /**
   * Collects the path relative to the repository directory. Only the resources that are actually from the repository 
   * will be collected.
   * 
   * @param wcDir Working copy directory.
   * @param resources URLs to test.
   * @param changedLocalFiles   Resources from the repository are collected in here in their relative form.
   */
  private static void collectFilesFromRepository(
      URL wcDir,
      URL[] resources,
      Set<String> changedLocalFiles) {
    String wcDirPath = wcDir.toExternalForm();
    for (URL editorLocationIterator : resources) {
      String externalForm = editorLocationIterator.toExternalForm();
      if (externalForm.startsWith(wcDirPath)) {
        // This resource is from the repository.
        changedLocalFiles.add(externalForm.substring(wcDirPath.length()));
      }
    }
  }
  
  /**
   * Checks in the remote repository if there are new commits. 
   * 
   * @return <code>commitsAhead</code> a list with all new commits
   */
  private static List<RevCommit> checkForRemoteCommits() {
    GitAccess gitAccess = GitAccess.getInstance();
    List<RevCommit> commitsBehind = new ArrayList<>();
    try {
      gitAccess.fetch();
      Repository repo = gitAccess.getRepository();
      CommitsAheadAndBehind commitsAheadAndBehind = RevCommitUtil.getCommitsAheadAndBehind(repo, repo.getFullBranch());
      commitsBehind = commitsAheadAndBehind.getCommitsBehind();

    } catch (NoRepositorySelected | IOException | SSHPassphraseRequiredException | PrivateRepositoryException
        | RepositoryUnavailableException e) {
      logger.error(e, e);
    }
    return commitsBehind;
  }
  
  /**
   * Creates a list in string format from all files in <code>filesMap</code> map
   * @param filesMap  Map that contains the files to be transformed
   * @return a string which represents the list of files
   */
  private static String getFilesModified(Set<String> filesSet) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String file : filesSet) {
      stringBuilder.append(file + ", ");
    }

    stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), ".");

    return stringBuilder.toString();
  }
}
