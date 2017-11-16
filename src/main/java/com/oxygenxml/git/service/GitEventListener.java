package com.oxygenxml.git.service;

/**
 * Receives notifications when the state of the Git repository changes.
 * 
 * @author alex_jitianu
 * 
 * 
 * TODO We need a number of notifications:
 * 
 * 2. push/pull
 * 3. commit
 * 4. branchChanged
 * 5. fileStateChanged (the stuff from the observer)
 */
public interface GitEventListener {
  /**
   * TODO The WCPanel, the clone action and the select submodule action (both on toolbar)
   * are the ones that change the repository. This means that both the WCPanel and the Toolbar 
   * must listen for repository changes and update if they are not the ones that initiated the event. 
   * 
   * 
   * A new repository was loaded.
   * 
   * @param repository The path of the new repository.
   */
  void repositoryChanged();
}
