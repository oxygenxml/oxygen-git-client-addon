package com.oxygenxml.git.service;

import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;

import com.oxygenxml.git.view.event.GitEventInfo;

/**
 * A cache intended to avoid reading the file system too often.
 * @author alex_jitianu
 */
public class StatusCache {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(StatusCache.class);
  
  
  private GitStatus cache = null;
  private Supplier<Git> statusComputer;
  
  public StatusCache(GitListeners listeners, Supplier<Git> statusComputer) {
    this.statusComputer = statusComputer;
    listeners.addGitListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        resetCache();
      }
    });
  }
  
  /**
   * @return Cached status.
   */
  public synchronized GitStatus getStatus() {
    if (cache == null) {
      cache = new GitStatusCommand(statusComputer.get()).getStatus();
    }
    return cache;
  }

  public synchronized void resetCache() {
    cache = null;
  }
}
