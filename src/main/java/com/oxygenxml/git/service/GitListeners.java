package com.oxygenxml.git.service;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * Git notifications support. Central point to register all parties interested in receiving 
 * Git operation notifications.
 */
public class GitListeners {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GitListeners.class);
  /**
   * Receive notifications when things change. First ones to be notified.
   */
  private HashSet<GitEventListener> gitEventPriorityListeners = new LinkedHashSet<>();
  
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation about to start: {}", info);
    }
    
    for (GitEventListener gitEventListener : gitEventPriorityListeners) {
      gitEventListener.operationAboutToStart(info);
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation successfully ended: {}", info);
    }
    
    for (GitEventListener gitEventListener : gitEventPriorityListeners) {
      gitEventListener.operationSuccessfullyEnded(info);
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Fire operation failed: {}. Reason: {}.", info, t.getMessage());
    }
    
    for (GitEventListener gitEventListener : gitEventPriorityListeners) {
      gitEventListener.operationFailed(info, t);
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
  void addGitPriorityListener(GitEventListener listener) {
    HashSet<GitEventListener> clone = (HashSet<GitEventListener>) gitEventPriorityListeners.clone();
    clone.add(listener);
    
    gitEventPriorityListeners = clone;
  } 

  /**
   * Add a listener that gets notified about file or repository changes.
   * 
   * @param listener The listener to add.
   */
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
  public void removeGitListener(GitEventListener listener) {
    HashSet<GitEventListener> clone = (HashSet<GitEventListener>) gitEventListeners.clone();
    clone.remove(listener);
    
    gitEventListeners = clone;
  }

  /**
   * Drops all recorded listeners. Usually used from tests.
   */
  void clear() {
    gitEventPriorityListeners.clear();
    gitEventListeners.clear();
  }
}
