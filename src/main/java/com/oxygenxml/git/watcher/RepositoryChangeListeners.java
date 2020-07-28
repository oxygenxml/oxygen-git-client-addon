package com.oxygenxml.git.watcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;
import com.oxygenxml.git.view.historycomponents.CommitsAheadAndBehind;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class RepositoryChangeListeners {

  protected static ScheduledFuture<?> future;

  private static Logger logger = Logger.getLogger(RepositoryChangeListeners.class);

  private static PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();

  /**
   * Creates the listener for supervising changes in the remote repository
   */
  public static WSEditorChangeListener createWatcherListener() {

    WSEditorChangeListener wsEditorChangeListener = new WSEditorChangeListener() {
      @Override
      public void editorOpened(URL editorLocation) {
        String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
        if (value.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)
            || value.equals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
          // coalescing
          if (future != null) {
            future.cancel(false);
          }
          future = GitOperationScheduler.getInstance().schedule(() -> task(value), 400);
        }
      }
    };
    return wsEditorChangeListener;
  }
  /**
   * Executes the task accordingly to the option
   * @param option Contains the option chosen by the user
   * @return <code>null</code>
   */
  public static Object task(String option) {
    if (option.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)
        || option.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
      List<RevCommit> commitsAhead;
      commitsAhead = checkForRemoteCommits();
      if (!commitsAhead.isEmpty()) {
        if (option.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ALWAYS)) {
          // notify new commit in remote
          pluginWorkspace.showInformationMessage(Translator.getInstance().getTranslation(Tags.NEW_COMMIT_UPSTREAM));
        } else if (option.contentEquals(OxygenGitOptionPagePluginExtension.WARN_UPSTREAM_ON_CHANGE)) {
          HashMap<String, String> checkForRemoteFileChanges = checkForRemoteFileChanges();
          if (!checkForRemoteFileChanges.isEmpty()) {
            pluginWorkspace.showInformationMessage(
                Translator.getInstance().getTranslation(Tags.NEW_COMMIT_WITH_MODIFIED_OPENED_FILES)
                    + getFilesModified(checkForRemoteFileChanges));
          }
        }
      }
    }
    return null;
  }
  
  /**
   * Checks in the remote repository if there are changes that affect the opened files in the editor areas
   * @return <code>changedRemoteFiles</code> a map with all the files modified remote that are opened locally
   */
  private static HashMap<String, String> checkForRemoteFileChanges() {
    GitAccess gitAccess = GitAccess.getInstance();
    Repository repo;
    HashMap<String, String> changedRemoteFiles = new HashMap<String, String>();
    HashMap<String, String> openedLocalFiles = getFilesOpenedInEditors();

    try {
      repo = gitAccess.getRepository();
      List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(repo.getFullBranch());

      for (CommitCharacteristics commitsCharacteristicsIterator : commitsCharacteristics) {
        String commitId = commitsCharacteristicsIterator.getCommitId();
        List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitId);

        for (FileStatus changedFilesIterator : changedFiles) {
          String fileLocation = changedFilesIterator.getFileLocation();
          if (openedLocalFiles.containsKey(fileLocation)) {
            changedRemoteFiles.put(fileLocation, fileLocation);
          }
        }
      }
    } catch (IOException | NoRepositorySelected | GitAPIException e) {
      logger.error(e, e);
    }
    return changedRemoteFiles;
  }

  /**
   * Retrieves all the files opened in dita maps and main editing areas
   * @return  <code>changedLocalFiles</code> a map with all the opened files
   */
  private static HashMap<String, String> getFilesOpenedInEditors() {

    HashMap<String, String> changedLocalFiles = new HashMap<String, String>();
    getFilesFromEditor(changedLocalFiles, StandalonePluginWorkspace.MAIN_EDITING_AREA);
    getFilesFromEditor(changedLocalFiles, StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA);

    return changedLocalFiles;
  }
  
  /**
   * Stores in the map <code>changedLocalFiles</code> all files opened in a specific editing area <code>editingArea</code>
   * @param changedLocalFiles
   * @param editingArea
   */
  private static void getFilesFromEditor(HashMap<String, String> changedLocalFiles, int editingArea) {
    URL[] allEditorLocations = pluginWorkspace.getAllEditorLocations(editingArea);
    for (URL editorLocationIterator : allEditorLocations) {
      String file = editorLocationIterator.getFile();
      changedLocalFiles.put(file, file);
    }
  }
  
  /**
   * Checks in the remote repository if there are new commits. 
   * @return <code>commitsAhead</code> a list with all new commits
   */
  private static List<RevCommit> checkForRemoteCommits() {
    GitAccess gitAccess = GitAccess.getInstance();
    Repository repo;
    List<RevCommit> commitsAhead = new ArrayList<RevCommit>();
    try {
      gitAccess.fetch();
      repo = gitAccess.getRepository();
      CommitsAheadAndBehind commitsAheadAndBehind = RevCommitUtil.getCommitsAheadAndBehind(repo, repo.getFullBranch());
      commitsAhead = commitsAheadAndBehind.getCommitsAhead();

    } catch (NoRepositorySelected | IOException | SSHPassphraseRequiredException | PrivateRepositoryException
        | RepositoryUnavailableException e) {
      logger.error(e, e);
    }
    return commitsAhead;
  }
  
  /**
   * Creates a list in string format from all files in <code>filesMap</code> map
   * @param filesMap
   * @return a string which represents the list
   */
  private static String getFilesModified(HashMap<String, String> filesMap) {
    StringBuilder stringBuilder = new StringBuilder();

    filesMap.forEach((key, value) -> stringBuilder.append(key + ", "));
    stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), ".");

    return stringBuilder.toString();
  }
}
