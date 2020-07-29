package com.oxygenxml.git.watcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension;
import com.oxygenxml.git.options.OptionsManager;
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

public class RepositoryChangeWatcher {
  /**
   * Sleeping time used to implement the coalescing.
   */
  private static final int SLEEP = 400;

  protected static ScheduledFuture<?> future;

  private static Logger logger = Logger.getLogger(RepositoryChangeWatcher.class);

  private static PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();

  /**
   * Private constructor.
   */
  private RepositoryChangeWatcher() {}
  
  /**
   * Add a listener for the editing areas and schedule the first search for changes in the remote repository
   * @param standalonePluginWorkspace The Plugin Workspace
   */
  public static void initialize(StandalonePluginWorkspace standalonePluginWorkspace) {
    RepositoryChangeWatcher.addListeners4EditingAreas(standalonePluginWorkspace);
    String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
    GitOperationScheduler.getInstance().schedule(() -> notifyUser(value), SLEEP);
  }
  
  /**
   * Creates a new listener to supervise the remote changes and adds it to the editing areas
   * @param standalonePluginWorkspace The Plugin Workspace
   */
  public static void addListeners4EditingAreas(StandalonePluginWorkspace standalonePluginWorkspace) {
    WSEditorChangeListener editorListenerAlways = createWatcherListener();
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.MAIN_EDITING_AREA);
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA);
  }
  
 
  /**
   * Creates the listener for supervising changes in the remote repository
   */
  public static WSEditorChangeListener createWatcherListener() {
    return new WSEditorChangeListener() {
      @Override
      public void editorOpened(URL editorLocation) {
        String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
        if (value.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)
            || value.equals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
          // coalescing
          if (future != null) {
            future.cancel(false);
          }
          future = GitOperationScheduler.getInstance().schedule(() -> notifyUser(value), SLEEP);
        }
      }
    };
  }
  /**
   * Executes the task accordingly to the option
   * @param notifyMode Contains the option chosen by the user
   */
  public static void notifyUser(String notifyMode) {
    if (notifyMode.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)
        || notifyMode.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
      List<RevCommit> commitsBehind = checkForRemoteCommits();
      if (!commitsBehind.isEmpty()) {
        if (notifyMode.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)) {
          // notify new commit in remote
          pluginWorkspace.showInformationMessage(Translator.getInstance().getTranslation(Tags.NEW_COMMIT_UPSTREAM));
        } else if (notifyMode.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
          HashMap<String, String> checkForRemoteFileChanges = checkForRemoteFileChanges(commitsBehind);
          if (!checkForRemoteFileChanges.isEmpty()) {
            pluginWorkspace.showInformationMessage(
                Translator.getInstance().getTranslation(Tags.NEW_COMMIT_WITH_MODIFIED_OPENED_FILES)
                    + getFilesModified(checkForRemoteFileChanges));
          }
        }
      }
    }
  }
  
  /**
   * Checks in the remote repository if there are changes that affect the opened files in the editor areas.
   * @param commitsBehind A list of new commits from remote that have to be checked 
   * 
   * @return A map with all the files modified remote that are opened locally
   */
  private static HashMap<String, String> checkForRemoteFileChanges(List<RevCommit> commitsBehind) {
    HashMap<String, String> changedRemoteFiles = new HashMap<>();
    HashSet<String> openedLocalFiles = getFilesOpenedInEditors();

    List<CommitCharacteristics> commitsCharacteristics = RevCommitUtil.createRevCommitCharacteristics(commitsBehind);

    for (CommitCharacteristics commitsCharacteristicsIterator : commitsCharacteristics) {
      String commitId = commitsCharacteristicsIterator.getCommitId();
      try {
        List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitId);
        
        for (FileStatus changedFilesIterator : changedFiles) {
          String fileLocation = changedFilesIterator.getFileLocation();
          if (openedLocalFiles.contains(fileLocation)) {
            changedRemoteFiles.put(fileLocation, commitId);
          }
        }
      } catch (IOException | GitAPIException e) {
        logger.error(e, e);
      }
    }
    return changedRemoteFiles;
  }

  /**
   * Retrieves all the files opened in dita maps and main editing areas
   * @return  <code>changedLocalFiles</code> a map with all the opened files
   */
  private static HashSet<String> getFilesOpenedInEditors() {
    HashSet<String> changedLocalFiles = new HashSet<>();
    getFilesFromEditor(changedLocalFiles, StandalonePluginWorkspace.MAIN_EDITING_AREA);
    getFilesFromEditor(changedLocalFiles, StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA);

    return changedLocalFiles;
  }
  
  /**
   * Stores in the map <code>changedLocalFiles</code> all files opened in a specific editing area <code>editingArea</code>
   * @param changedLocalFiles   Map in which to store the files
   * @param editingArea         Editing area from which to get the files        
   */
  private static void getFilesFromEditor(HashSet<String> changedLocalFiles, int editingArea) {
    URL[] allEditorLocations = pluginWorkspace.getAllEditorLocations(editingArea);
    for (URL editorLocationIterator : allEditorLocations) {
      String file = editorLocationIterator.getFile();
      changedLocalFiles.add(file);
    }
  }
  
  /**
   * Checks in the remote repository if there are new commits. 
   * @return <code>commitsAhead</code> a list with all new commits
   */
  private static List<RevCommit> checkForRemoteCommits() {
    GitAccess gitAccess = GitAccess.getInstance();
    Repository repo;
    List<RevCommit> commitsBehind = new ArrayList<>();
    try {
      gitAccess.fetch();
      repo = gitAccess.getRepository();
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
  private static String getFilesModified(HashMap<String, String> filesMap) {
    StringBuilder stringBuilder = new StringBuilder();
    String previousCommitId = "";

    for (Map.Entry<String, String> entry : filesMap.entrySet()) {
      String commitId = entry.getValue();
      if (!commitId.equals(previousCommitId)) {
        stringBuilder.append("\nCommitID " + commitId + ": ");
        previousCommitId = commitId;
      }
      stringBuilder.append(entry.getKey() + ", ");
    }

    stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), ".");

    return stringBuilder.toString();
  }
}
