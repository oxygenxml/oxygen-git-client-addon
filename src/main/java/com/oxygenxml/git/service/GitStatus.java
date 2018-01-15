package com.oxygenxml.git.service;

import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;

/**
 * Working copy's status info.
 * 
 * @author alex_jitianu
 */
public class GitStatus {
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
