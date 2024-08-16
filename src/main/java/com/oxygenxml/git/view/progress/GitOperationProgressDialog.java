package com.oxygenxml.git.view.progress;

import javax.swing.SwingUtilities;

import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

/**
 * The dialog that presents a git operation progress. 
 * 
 * @author alex_smarandache
 *
 */
public class GitOperationProgressDialog extends ProgressDialog {
  
  /**
   * The default delay for the operation in millis.
   */
  private static final int DEFAULT_OPERATION_DELAY = 2000;
  
  /**
   * Constructor.
   * 
   * @param gitController          The git controller.
   * @param dialogTitle            The title of the current dialog.
   * @param operation              The Git operation of this dialog progress.
   * @param minOperationDuration   The minimum duration of an operation to show its progress.
   * @param isCancelOperationSupported     <code>true</code> if the cancel operation is supported.
   */
  public GitOperationProgressDialog(
      GitController gitController, 
      String dialogTitle, 
      GitOperation operation,
      boolean isCancelOperationSupported) {
    super(dialogTitle, isCancelOperationSupported);
    
    gitController.addGitListener(new GitEventListener() {
      
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        if(info.getGitOperation() == operation) {
          SwingUtilities.invokeLater(() -> showWithDelay(DEFAULT_OPERATION_DELAY));
        }
        
      }
      
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if(info.getGitOperation() == operation) {
          SwingUtilities.invokeLater(() -> doCancel());
        }
      }
      
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if(info.getGitOperation() == operation) {
          SwingUtilities.invokeLater(() -> markAsCompleted());
        }
      }
    }); 
  }
  
}

