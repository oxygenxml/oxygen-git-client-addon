package com.oxygenxml.git.service;

import com.oxygenxml.git.view.event.ChangeEvent;

/**
 * Receives notifications when the state of the Git repository changes.
 * 
 * @author alex_jitianu
 * 
 * 
 * TODO We need a number of notifications:
 * 
 * 2. push/pull
 * 4. branchChanged
 */
public interface GitEventListener {
  /**
   * A new repository was loaded.
   * 
   * @param repository The path of the new repository.
   */
  void repositoryChanged();
  
  /**
   * The state of some files changed because of a specific command.
   * 
   * @param changeEvent Details about the change.
   */
  void stateChanged(ChangeEvent changeEvent);
}
