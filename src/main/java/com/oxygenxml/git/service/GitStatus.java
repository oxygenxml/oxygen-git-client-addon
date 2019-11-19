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
   * Constructor.
   * 
   * @param unstagedFiles Unstaged files.
   * @param stagedFiles Staged files.
   */
  public GitStatus(List<FileStatus> unstagedFiles, List<FileStatus> stagedFiles) {
    this.unstagedFiles = unstagedFiles;
    this.stagedFiles = stagedFiles;
    logger.debug("Create GitStatus...");
    logger.debug("GitStatus unstaged files: " + unstagedFiles);
    logger.debug("GitStatus staged files: " + stagedFiles);
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
