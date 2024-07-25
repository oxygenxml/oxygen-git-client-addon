package com.oxygenxml.git.view.actions;

/**
 * This interface is implemented by the classes that update their info based on a progress.
 * 
 * @author alex_smarandache
 */
public interface IProgressUpdater {
  
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
   * @return <code>true</code> if the operation was completed.
   */
  boolean isCompleted();

}
