package com.oxygenxml.git.view.progress;

import java.util.EnumMap;

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
  private final EnumMap<GitOperation, ProgressDialog> operationsProgressDialogsCache = new EnumMap<>(GitOperation.class);
  
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
  private ProgressDialog getProgressDialogForGitOperation(GitOperation operation) {
    ProgressDialog dialog = operationsProgressDialogsCache.get(operation);
    
    if(dialog == null) {
      dialog = createProgressDialogForOperation(operation);
      operationsProgressDialogsCache.put(operation, dialog);
    }
    
    dialog.initUI();
    
    return dialog;
  }

  /**
   * Creates a progress dialog for the given operation.
   * 
   * @param operation The operation to follow the progress.
   * 
   * @return The created progress dialog.
   */
  private GitOperationProgressDialog createProgressDialogForOperation(GitOperation operation) {
    GitOperationProgressDialog progressDialog = null;
    switch(operation) {
    case CHECKOUT: {
      progressDialog = 
          new GitOperationProgressDialog(gitCtrl, 
              Translator.getInstance().getTranslation(Tags.SWITCH_BRANCH), 
              GitOperation.CHECKOUT, DEFAULT_OPERATION_DELAY);
      break;
    }

    case MERGE: {
      progressDialog = 
          new GitOperationProgressDialog(gitCtrl, 
              Translator.getInstance().getTranslation(Tags.MERGE), 
              GitOperation.MERGE, DEFAULT_OPERATION_DELAY);
      break;
    }
    
    case MERGE_RESTART: {
      progressDialog = 
          new GitOperationProgressDialog(gitCtrl, 
              Translator.getInstance().getTranslation(Tags.MERGE), 
              GitOperation.MERGE_RESTART, DEFAULT_OPERATION_DELAY);
      break;
    }
    
    default: {
      break;
    }
    }
    
    return progressDialog;
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
    ProgressDialog progressDialog = getProgressDialogForGitOperation(operation);
    return progressDialog != null ? new GitOperationProgressMonitor(progressDialog) : null;
  }

}
