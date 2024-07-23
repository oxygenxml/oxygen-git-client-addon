package com.oxygenxml.git.view.actions;

public interface IProgressUpdater {
  
  /**
   * @param note  Set the new note to be displayed.
   */
  void setNote(String note);
  
  /**
   * @return <code>true</code> if the dialog is canceled.
   */
  boolean isCanceled();
  
  /**
   * When the progress is finished with success.
   */
  void markAsCompleted();
  
  /**
   * @return <code>true</code> if the operation was completed.
   */
  boolean isCompleted();

}
