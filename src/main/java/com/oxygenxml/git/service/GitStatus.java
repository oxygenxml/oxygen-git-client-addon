package com.oxygenxml.git.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Working copy's status info.
 * 
 * @author alex_jitianu
 */
public class GitStatus {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GitStatus.class.getName());
  /**
   * Unstaged files.
   */
  private List<FileStatus> unstagedFiles;
  /**
   * Staged files.
   */
  private List<FileStatus> stagedFiles;
  
  /**
   * Whether there are uncommitted changes. {@code true} if any tracked file is changed
   */
  private final boolean hasUncommittedChanges;
  
  /**
   * <code>null</code> if is not initialized, <code>true</code> if the repository has files in conflict or <code>false</code> otherwise.
   */
  private Boolean hasFileInConflicts = null;
  
  
  /**
   * Whether there are uncommitted changes.
   *
   * @return {@code true} if any tracked file is changed.
   */
  public boolean hasUncommittedChanges() {
    return hasUncommittedChanges;
  }

  /**
   * Constructor.
   * 
   * @param unstagedFiles Unstaged files.
   * @param stagedFiles Staged files.
   * @param hasUncommittedChanges  {@code true} if any tracked file is changed.
   */
  public GitStatus(List<FileStatus> unstagedFiles, List<FileStatus> stagedFiles, boolean hasUncommittedChanges) {
    this.unstagedFiles = unstagedFiles;
    this.stagedFiles = stagedFiles;
    this.hasUncommittedChanges = hasUncommittedChanges;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Create GitStatus...");
      LOGGER.debug("GitStatus unstaged files: {}", unstagedFiles);
      LOGGER.debug("GitStatus staged files: {}", stagedFiles);
    }
  }
  
  /**
   * @return Staged files.
   */
  public List<FileStatus> getStagedFiles() {
    return stagedFiles;
  }
  
  /**
   * @return UnStaged files.
   */
  public List<FileStatus> getUnstagedFiles() {
    return unstagedFiles;
  }
  
  /**
   * @return <code>true</code> if the repository has files in conflict or <code>false</code> otherwise.
   */
  public boolean repositoryHasConflicts() {
    if(hasFileInConflicts == null) {
      hasFileInConflicts = getUnstagedFiles() != null && 
          getUnstagedFiles().stream().anyMatch(file -> file.getChangeType() == GitChangeType.CONFLICT);
    }
    
    return hasFileInConflicts;
  }
}
