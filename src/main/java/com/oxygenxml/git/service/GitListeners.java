package com.oxygenxml.git.service;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;

import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * Git notifications support. Central point to register all parties interested in receiving 
 * Git operation notifications.
 */
public class GitListeners {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitListeners.class);
  
  /**
   * Receive notifications when things change.
   */
  private HashSet<GitEventListener> gitEventListeners = new LinkedHashSet<>();
  /**
   * Singleton.
   */
  private static GitListeners instance = null;
  /**
   * Private contructor.
   */
  private GitListeners() {}
  /**
   * @return The singleton instance.
   */
  public static GitListeners getInstance() {
    if (instance == null) {
      instance = new GitListeners();
    }
    
   return instance; 
  }
  
  /**
   * Fire operation about to start.
   * 
   * @param info event info.
   */
  public void fireOperationAboutToStart(GitEventInfo info) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation about to start: " + info);
    }
    
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationAboutToStart(info);
    }
  }
  
  /**
   * Fire operation successfully ended.
   * 
   * @param info event info.
   */
  public void fireOperationSuccessfullyEnded(GitEventInfo info) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation successfully ended: " + info);
    }
    
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationSuccessfullyEnded(info);
    }
  }
  
  /**
   * Fire operation failed.
   * 
   * @param info event info.
   * @param t related exception/error. May be <code>null</code>.
   */
  public void fireOperationFailed(GitEventInfo info, Throwable t) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fire operation failed: " + info + ". Reason: " + t.getMessage());
    }
    
    for (GitEventListener gitEventListener : gitEventListeners) {
      gitEventListener.operationFailed(info, t);
    }
  }
  
  

  /**
   * Add a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to add.
   */
  @SuppressWarnings("unchecked")
  public void addGitListener(GitEventListener listener) {
    HashSet<GitEventListener> clone = (HashSet<GitEventListener>) gitEventListeners.clone();
    clone.add(listener);
    
    gitEventListeners = clone;
  }
  
  /**
   * Removes a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to remove.
   */
  @SuppressWarnings("unchecked")
  public void removeGitListener(GitEventListener listener) {
    HashSet<GitEventListener> clone = (HashSet<GitEventListener>) gitEventListeners.clone();
    clone.remove(listener);
    
    gitEventListeners = clone;
  }

  public void clear() {
    gitEventListeners.clear();
  }
}
