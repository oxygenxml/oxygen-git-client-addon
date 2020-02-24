package com.oxygenxml.git.service;

import java.io.File;

import com.oxygenxml.git.view.event.GitEvent;

/**
 * Receives notifications when the state of the Git repository changes.
 * 
 * @author alex_jitianu
 * 
 * 
 * TODO We need a number of notifications:
 * 
 * - push/pull
 */
public interface GitEventListener {
  /**
   * A repository is about to be opened.
   * 
   * @param repo The repository.
   */
  void repositoryIsAboutToOpen(File repo);
  
  /**
   * A new repository was loaded.
   * 
   * @param repository The path of the new repository.
   */
  void repositoryChanged();
  
  /**
   * A repository could not be opened.
   * 
   * @param repo The repository.
   * @param ex   The exception that broke the opening. May be <code>null</code>.
   */
  void repositoryOpeningFailed(File repo, Throwable ex);
  
  /**
   * The state of some files changed because of a specific command.
   * 
   * @param changeEvent Details about the change.
   */
  void stateChanged(GitEvent changeEvent);
  
  /**
   * The active branch changed.
   * 
   * @param oldBranch Previous branch qualified name.
   * @param newBranch New branch qualified name.
   */
  void branchChanged(String oldBranch, String newBranch);
}
