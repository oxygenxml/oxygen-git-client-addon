package com.oxygenxml.git.view.actions;

/**
 * This interface is implemented by the classes that update their info based on a progress.
 * 
 * @author alex_smarandache
 */
public interface IProgressUpdater {
  
  /**
   * The default delay for the operation in millis.
   */
  static final int DEFAULT_OPERATION_DELAY = 2000;
  
  /**
   * Show the progress with a delay if it is still not finished.
   * 
   * @param millisDelay The delay in milliseconds.
   */
  void showWithDelay(long millisDelay);
  
  /**
   * @param note  Set the new note to be displayed.
   */
  void setNote(String note);
  
  /**
   * @return <code>true</code> if the dialog is cancelled.
   */
  boolean isCancelled();
  
  /**
   * When the progress is finished with success.
   */
  void markAsCompleted();
  
  /**
   * When the progress is finished with fail.
   */
  void markAsFailed();
  
  /**
   * @return <code>true</code> if the operation was completed.
   */
  boolean isCompleted();
  
  /**
   * @return <code>true</code> if the operation fail.
   */
  boolean isFailed();

}
