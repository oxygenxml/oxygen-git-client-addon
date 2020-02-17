package com.oxygenxml.git.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Schedules git operations on a thread. The same thread is being used. 
 */
public class GitOperationScheduler {
  /**
   * Logger.
   */
  private static final Logger logger = Logger.getLogger(GitOperationScheduler.class);
  /**
   * Refresh executor.
   */
  private ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1) {
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      if (t != null) {
        logger.error(t, t);
      }

      if (r instanceof Future) {
        try {
          ((Future<?>) r).get();
        } catch (CancellationException e) {
          logger.debug(e, e);
        } catch (Exception e) {
          logger.error(e, e);
        }
      }
    }
  };
  
  /**
   * Singleton instance.
   */
  private static GitOperationScheduler instance;
  
  /**
   * Singleton private constructor.
   */
  private GitOperationScheduler() {}
  
  /**
   * @return The singleton instance.
   */
  public static GitOperationScheduler getInstance() {
    if (instance == null) {
      instance = new GitOperationScheduler();
    }
    
    return instance;
  }
  
  /**
   * Schedules a runnable.
   * 
   * @param r Code to be executed on thread.
   * @return
   */
  public ScheduledFuture<?> schedule(Runnable r) {
    if (executor.isShutdown()) {
      // A shutdown operation was canceled.
      executor = new ScheduledThreadPoolExecutor(1);
    }
    
    return executor.schedule(r, 500, TimeUnit.MILLISECONDS);
  }

  /**
   * Attempts to shutdown any running tasks.
   */
  public void shutdown() {
    executor.shutdown();
    try {
      executor.awaitTermination(2000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      logger.warn("Unable to stop task thread: " + e.getMessage(), e);
      // Restore interrupted state...
      Thread.currentThread().interrupt();

    }    
  }
}
