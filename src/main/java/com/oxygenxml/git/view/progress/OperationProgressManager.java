package com.oxygenxml.git.view.progress;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.lib.ProgressMonitor;

import com.oxygenxml.git.service.OperationProgressFactory;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitOperation;

/**
 * Manages the progress of the Git operations.
 * 
 * @author alex_smarandache
 */
public class OperationProgressManager implements OperationProgressFactory {
  
  /**
   * The current Git controller.
   */
  private final GitController gitCtrl;
  
  /**
   * A map with the operations and the progress dialogs.
   */
  private final Map<GitOperation, ProgressDialog> operationsProgressDialogsCache = new HashMap<>();
  
  /**
   * The default delay for the operation in millis.
   */
  private static final int DEFAULT_OPERATION_DELAY = 2000;
  
  /**
   * Constructor.
   * 
   * @param gitCtrl The Git controller.
   */
  public OperationProgressManager(GitController gitCtrl) {
    this.gitCtrl = gitCtrl;
  }
  
  /**
   * Get the corresponding progress dialog for the current git operation if there is support for progress on this operation.
   * 
   * @param operation The current operation.
   * 
   * @return The created progress dialog.
   */
  private ProgressDialog getProgressDialogByGitOperation(GitOperation operation) {
    if(!operationsProgressDialogsCache.containsKey(operation)) {
      addProgressDialogForOperation(operation);
    }
    
    ProgressDialog dialog = operationsProgressDialogsCache.get(operation);
    if(dialog != null) {
      dialog.initUI();
    }
    
    return dialog;
    
  }

  /**
   * Add in the dialogs cache a progress dialog for the given operation.
   * 
   * @param operation The operation to follow the progress.
   */
  private void addProgressDialogForOperation(GitOperation operation) {
    switch(operation) {
      case CHECKOUT:
        operationsProgressDialogsCache.put(
            GitOperation.CHECKOUT, 
            new GitOperationProgressDialog(
                gitCtrl, 
                Translator.getInstance().getTranslation(Tags.SWITCH_BRANCH), 
                GitOperation.CHECKOUT, DEFAULT_OPERATION_DELAY));
        break;

      case MERGE:
        operationsProgressDialogsCache.put(
            GitOperation.MERGE, 
            new GitOperationProgressDialog(
                gitCtrl, 
                Translator.getInstance().getTranslation(Tags.MERGE), 
                GitOperation.MERGE, DEFAULT_OPERATION_DELAY));
        break;

      case MERGE_RESTART: 
        operationsProgressDialogsCache.put(
            GitOperation.MERGE_RESTART, 
            new GitOperationProgressDialog(
                gitCtrl, 
                Translator.getInstance().getTranslation(Tags.MERGE), 
                GitOperation.MERGE_RESTART, DEFAULT_OPERATION_DELAY));
        break;

      default: 
        break;
    }
  }

  /**
   * Create a progress monitor for the given operation.
   * 
   * @param operation The current git operation.
   * 
   * @return The created progress monitor.
   */
  @Override
  public ProgressMonitor getProgressMonitorByOperation(GitOperation operation) {
    ProgressDialog progressDialog = getProgressDialogByGitOperation(operation);
    return progressDialog != null ? new GitOperationProgressMonitor(progressDialog) : null;
  }

}
