package com.oxygenxml.git.service;

import java.util.List;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.entities.FileStatus;

/**
 * Working copy's status info.
 * 
 * @author alex_jitianu
 */
public class GitStatus {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitStatus.class.getName());
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
    if (logger.isDebugEnabled()) {
      logger.debug("Create GitStatus...");
      logger.debug("GitStatus unstaged files: " + unstagedFiles);
      logger.debug("GitStatus staged files: " + stagedFiles);
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
}
