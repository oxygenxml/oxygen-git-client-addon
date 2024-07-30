package com.oxygenxml.git.service;

import com.oxygenxml.git.service.exceptions.IndexLockExistsException;
import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * Receives notifications when the state of the Git repository changes.
 */
public interface GitEventListener {
  /**
   * Operation about to start.
   * 
   * @param info Extra information about the current event.
   * 
   * @throws IndexLockExistsException If index.lock exists and would prevent operations from executing.
   */
  void operationAboutToStart(GitEventInfo info) throws IndexLockExistsException;
  /**
   * Operation successfully ended.
   * 
   * @param info Extra information about the current event.
   */
  void operationSuccessfullyEnded(GitEventInfo info);
  /**
   * Operation failed.
   * 
   * @param info Extra information about the current event.
   * @param t    Exception/error related to the failure. May be <code>null</code>.
   */
  void operationFailed(GitEventInfo info, Throwable t);
}
