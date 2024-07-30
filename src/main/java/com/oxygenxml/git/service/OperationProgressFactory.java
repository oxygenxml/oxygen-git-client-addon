package com.oxygenxml.git.service;

import org.eclipse.jgit.lib.ProgressMonitor;

import com.oxygenxml.git.view.event.GitOperation;

/**
 * The factory with the operation progress.
 * 
 * @author alex_smarandache
 */
public interface OperationProgressFactory {
  
  /**
   * Create a progress monitor for the given operation.
   * 
   * @param operation The current git operation.
   * 
   * @return The created progress monitor.
   */
  ProgressMonitor getProgressMonitorByOperation(GitOperation operation);

}
