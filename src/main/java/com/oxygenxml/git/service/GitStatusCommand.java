package com.oxygenxml.git.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.submodule.SubmoduleStatus;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.RepoUtil;

/**
 * A wrapper over a JGit status command that performs groups the files into stage and 
 * unstaged.
 * 
 * @author alex_jitianu
 */
public class GitStatusCommand {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(GitAccess.class);
  /**
   * A provider for the JGit API.
   */
  private Supplier<Git> git;

  /**
   * Constructor.
   * 
   * @param git Supplier for the current git repository.
   */
  GitStatusCommand(Supplier<Git> git) {
    this.git = git;
    
  }
  
  /**
   * @return A status of the Working Copy, with the unstaged and staged files.
   */
  public GitStatus getStatus() {
    GitStatus gitStatus = null;
    if (git != null) {
      try {
        LOGGER.debug("-- Compute our GitStatus -> getStatus() --");
        Status status = git.get().status().call();
        LOGGER.debug("-- Get JGit status -> git.status().call() --");
        gitStatus = new GitStatus(getUnstagedFiles(status), getStagedFiles(status), status.hasUncommittedChanges());
      } catch (GitAPIException e) {
        LOGGER.error(e, e);
      }
    }
    return gitStatus != null ? gitStatus 
        : new GitStatus(Collections.emptyList(),Collections.emptyList(), false);
  }
  
  /**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.<br><br>
   * 
   * NOTE: if the staged files are also needed, use {@link #getStatus()} method instead.
   * 
   * @return - A list with all unstaged files
   */
  public List<FileStatus> getUnstagedFiles() {
    return getUnstagedFiles(Collections.<String>emptyList());
  }
  
  /**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.
   * 
   * @param paths A subset of interest.
   * 
   * @return - A list with the files from the given set that are un-staged as well
   *         as their states.
   */
  public List<FileStatus> getUnstagedFiles(Collection<String> paths) {
    if (git != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("PUBLIC - GET UNSTAGED FILES");
        LOGGER.debug("Prepare fot JGit status, in paths " + paths);
      }
      
      StatusCommand statusCmd = git.get().status();
      for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
        statusCmd.addPath(iterator.next());
      }
      try {
        Status status = statusCmd.call();
        LOGGER.debug("JGit Status computed: " + status);
        return getUnstagedFiles(status);
      } catch (GitAPIException e) {
        LOGGER.error(e, e);
      }
    }
    
    return Collections.emptyList();
  }

  /**
   * Makes a diff between the files from the last commit and the files from the
   * working directory. If there are diffs, they will be saved and returned.
   * 
   * @param status The repository's status.
   * 
   * @return The unstaged files and their states.
   */
  private List<FileStatus> getUnstagedFiles(Status status) {
    LOGGER.debug("PRIVATE - GET UNSTAGE FOR GIVEN STATUS " + status);
    List<FileStatus> unstagedFiles = new ArrayList<>();
    if (git != null) {
      try {
        Set<String> submodules = getSubmoduleAccess().getSubmodules();
        addSubmodulesToUnstaged(unstagedFiles, submodules);
        addUntrackedFilesToUnstaged(status, unstagedFiles, submodules);
        addModifiedFilesToUnstaged(status, unstagedFiles, submodules);
        addMissingFilesToUnstaged(status, unstagedFiles, submodules);
        addConflictingFilesToUnstaged(status, unstagedFiles);
      } catch (NoWorkTreeException | GitAPIException e1) {
        LOGGER.error(e1, e1);
      }
    }
    return unstagedFiles;
  }

  /**
   * Add conflicting files to the list of resources that are not staged.
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   */
  private void addConflictingFilesToUnstaged(Status status, List<FileStatus> unstagedFiles) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addConflictingFilesToUnstaged: " + status.getConflicting());
    }
    for (String fileName : status.getConflicting()) {
      unstagedFiles.add(new FileStatus(GitChangeType.CONFLICT, fileName));
    }
  }

  /**
   * Add missing files to the list of resources that are not staged (not in the
   * INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addMissingFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addMissingFilesToUnstaged: " + status.getMissing());
    }
    for (String string : status.getMissing()) {
      if (!submodules.contains(string)) {
        unstagedFiles.add(new FileStatus(GitChangeType.MISSING, string));
      }
    }
  }

  /**
   * Add modified files to the list of resources that are not staged (not in the
   * INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addModifiedFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addModifiedFilesToUnstaged " + status.getModified());
    }
    for (String string : status.getModified()) {
      // A file that was modified compared to the one from INDEX.
      if (!submodules.contains(string)) {
        unstagedFiles.add(new FileStatus(GitChangeType.MODIFIED, string));
      }
    }
  }

  /**
   * Add untracked files (i.e. newly created files) to the list of resources that
   * are not staged (not in the INDEX).
   * 
   * @param status        The repository's status.
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   */
  private void addUntrackedFilesToUnstaged(Status status, List<FileStatus> unstagedFiles, Set<String> submodules) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addUntrackedFilesToUnstaged " + status.getUntracked());
    }
    for (String string : status.getUntracked()) {
      if (!submodules.contains(string)) {
        unstagedFiles.add(new FileStatus(GitChangeType.UNTRACKED, string));
      }
    }
  }

  /**
   * Add submodules to the list of resources that are not staged.
   * 
   * @param unstagedFiles The list of unstaged (not in the INDEX) files.
   * @param submodules    The set of submodules.
   * 
   * @throws GitAPIException When an error occurs when trying to check the
   *                         submodules status.
   */
  private void addSubmodulesToUnstaged(List<FileStatus> unstagedFiles, Set<String> submodules) throws GitAPIException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("addSubmodulesToUnstaged " + submodules);
    }
    for (String submodulePath : submodules) {
      SubmoduleStatus submoduleStatus = git.get().submoduleStatus().call().get(submodulePath);
      if (submoduleStatus != null && submoduleStatus.getHeadId() != null
          && !submoduleStatus.getHeadId().equals(submoduleStatus.getIndexId())) {
        
        unstagedFiles.add(
            new FileStatus(GitChangeType.SUBMODULE, submodulePath).setDescription(
                RepoUtil.extractSubmoduleChangeDescription(git.get().getRepository(), submoduleStatus)));
        
      }
    }
  }

  /**
   * @return API for working with submodules.
   */
  public SubmoduleAccess getSubmoduleAccess() {
    return SubmoduleAccess.wrap(git);
  }

  

  /**
  * Gets all the files from the index.<br><br>
  * 
  * NOTE: if the unstaged files are also needed, use {@link #getStatus()} method instead.
  * 
  * @return A set containing all the staged file names
  */
 public List<FileStatus> getStagedFiles() {
   return getStagedFile(Collections.<String>emptyList());
 }
 
 /**
  * Checks which files from the given subset are in the Index and returns their
  * state.
  * 
  * @param paths The files of interest.
  * 
  * @return - a set containing the subset of files present in the INDEX.
  */
 public List<FileStatus> getStagedFile(Collection<String> paths) {
   if (git != null) {
     StatusCommand statusCmd = git.get().status();
     for (String path : paths) {
       statusCmd.addPath(path);
     }

     try {
       Status status = statusCmd.call();
       return getStagedFiles(status);
     } catch (GitAPIException e) {
       LOGGER.error(e, e);
     }
   }
   
   return Collections.emptyList();
 }

 /**
  * Checks which files from the given subset are in the Index and returns their
  * state.
  * 
  * @param status The current status.
  * 
  * @return - a set containing the subset of files present in the INDEX.
  */
 private List<FileStatus> getStagedFiles(Status status) {
   List<FileStatus> stagedFiles = new ArrayList<>();
   Set<String> submodules = getSubmoduleAccess().getSubmodules();

   for (String fileName : status.getChanged()) {
     // File from INDEX, modified from HEAD
     if (submodules.contains(fileName)) {
       stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
     } else {
       stagedFiles.add(new FileStatus(GitChangeType.CHANGED, fileName));
     }
   }
   for (String fileName : status.getAdded()) {
     // Newly created files added in the INDEX
     if (submodules.contains(fileName)) {
       stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
     } else {
       stagedFiles.add(new FileStatus(GitChangeType.ADD, fileName));
     }
   }
   for (String fileName : status.getRemoved()) {
     // A delete added in the INDEX, file is present in HEAD.
     if (submodules.contains(fileName)) {
       stagedFiles.add(new FileStatus(GitChangeType.SUBMODULE, fileName));
     } else {
       stagedFiles.add(new FileStatus(GitChangeType.REMOVED, fileName));
     }
   }

   return stagedFiles;
 }

}
