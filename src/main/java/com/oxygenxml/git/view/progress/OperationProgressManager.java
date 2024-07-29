package com.oxygenxml.git.view.progress;

import java.util.HashMap;
import java.util.Map;

import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitOperation;

/**
 * Manages the progress of the Git operations.
 * 
 * @author alex_smarandache
 */
public class OperationProgressManager {
  
  /**
   * The current Git controller.
   */
  private static GitController gitCtrl;
  
  /**
   * A map with the operations and the progress dialogs.
   */
  private final static Map<GitOperation, ProgressDialog> OPERATIONS_PROGRESS_DIALOG_MAP;
  static {
    OPERATIONS_PROGRESS_DIALOG_MAP = new HashMap<>();
  }
  
  /**
   * Hidden constructor.
   */
  private OperationProgressManager() {
    // not needed
  }
  
  /**
   * Get the corresponding progress dialog for the current git operation if there is support for progress on this operation.
   * 
   * @param operation The current operation.
   * 
   * @return The created progress dialog.
   */
  public static ProgressDialog getProgressDialogByGitOperation(GitOperation operation) {
    if (operation == GitOperation.CHECKOUT && !OPERATIONS_PROGRESS_DIALOG_MAP.containsKey(operation)) {
      OPERATIONS_PROGRESS_DIALOG_MAP.put(GitOperation.CHECKOUT, new SwitchBranchesProgressDialog(gitCtrl));
    }
    
    ProgressDialog dialog = OPERATIONS_PROGRESS_DIALOG_MAP.get(operation);
    
    if(dialog != null) {
      dialog.initUI();
    }
    
    return dialog;
    
  }
  
  /**
   * Initialize the manager.
   * 
   * @param gitCtrl The git controller.
   */
  public static void init(GitController gitCtrl) {
    OperationProgressManager.gitCtrl = gitCtrl;
  }

}
