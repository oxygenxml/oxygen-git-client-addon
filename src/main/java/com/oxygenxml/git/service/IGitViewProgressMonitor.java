package com.oxygenxml.git.service;

import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * The visual progress monitor for a Git operation.
 * 
 * @author alex_smarandache
 */
public interface IGitViewProgressMonitor extends ProgressMonitor {
  
  /**
   * Show the progress view with the given delay.
   * So, show the progress even the operation duration is longest that the delay.
   * 
   * @param millisDelay The delay in milliseconds.
   */
  void showWithDelay(long millisDelay);
  
  /**
   * When the progress is finished with success.
   */
  void markAsCompleted();
  
  /**
   * When the progress is finished with a fail.
   */
  void markAsFailed();
  

}
