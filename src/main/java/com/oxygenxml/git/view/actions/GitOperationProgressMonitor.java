package com.oxygenxml.git.view.actions;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.service.IGitViewProgressMonitor;

/**
 * The progress monitor of a Git operation.
 * 
 * @author alex_smarandache
 *
 */
public class GitOperationProgressMonitor implements IGitViewProgressMonitor {

  /**
   * The title of the task.
   */
  private String taskTitle;
  
  /**
   * The total work to be done.
   */
  private float totalWork;
  
  /**
   * The current work.
   */
  private float currentWork = 0;

  /**
   * This element is updated with the new operation progress.
   */
  private final IProgressUpdater progressUpdater;

  /**
   * Constructor.
   * 
   * @param progressUpdater This element is updated with the new operation progress.
   */
  public GitOperationProgressMonitor(@NonNull final IProgressUpdater progressUpdater) {
    this.progressUpdater = progressUpdater;
  }

  @Override
  public void update(int completed) {
    currentWork += completed;
    if (progressUpdater != null) {
      String text = "";
      if (totalWork != 0) {
        float percentFloat = currentWork / totalWork * 100;
        int percent = (int) percentFloat;
        text = taskTitle + " " + percent + "% completed";
      } else {
        text = taskTitle + "100% completed";
      }
      progressUpdater.setNote(text);
    }
  }

  @Override
  public void start(int totalTasks) {
    currentWork = 0;
  }
  
  @Override
  public boolean isCancelled() {
    boolean isCancelled = false;
    if (progressUpdater != null && progressUpdater.isCancelled()) {
      progressUpdater.setNote("Canceling...");
      isCancelled = true;
    }
    return isCancelled;
  }

  @Override
  public void endTask() {
    currentWork = 0;
  }

  @Override
  public void beginTask(String title, int totalWork) {
    currentWork = 0;
    this.taskTitle = title;
    this.totalWork = totalWork;
  }

  @Override
  public void showDuration(boolean enabled) {
    // Not of interest
  }

  @Override
  public void showWithDelay(long millisDelay) {
    if(progressUpdater != null) {
      progressUpdater.showWithDelay(millisDelay);
    }
    
  }

  @Override
  public void markAsCompleted() {
    if(progressUpdater != null) {
      progressUpdater.markAsCompleted();
    }    
  }

  @Override
  public void markAsFailed() {
    if(progressUpdater != null) {
      progressUpdater.markAsFailed();
    }
    
  }
}