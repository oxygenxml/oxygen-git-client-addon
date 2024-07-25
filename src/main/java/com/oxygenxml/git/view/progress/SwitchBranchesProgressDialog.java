package com.oxygenxml.git.view.progress;

import javax.swing.SwingUtilities;

import com.oxygenxml.git.service.GitEventListener;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.dialog.internal.OnDialogCancel;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

/**
 * The dialog that presents the switch branch operation progress. 
 * 
 * @author alex_smarandache
 */
class SwitchBranchesProgressDialog extends ProgressDialog {
  
  /**
   * Constructor.
   * 
   * @param gitController The git controller.
   */
  public SwitchBranchesProgressDialog(GitController gitController) {
    super(Translator.getInstance().getTranslation(Tags.SWITCH_BRANCH));
    
    gitController.addGitListener(new GitEventListener() {
      
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        if(info.getGitOperation() == GitOperation.CHECKOUT) {
          SwingUtilities.invokeLater(() -> {
            showProgress();
          });
        }
        
      }
      
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        if(info.getGitOperation() == GitOperation.CHECKOUT) {
          SwingUtilities.invokeLater(() -> {
            doCancel();
          });
        }
      }
      
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if(info.getGitOperation() == GitOperation.CHECKOUT) {
          SwingUtilities.invokeLater(() -> {
            markAsCompleted();
          });
        }
        
      }
    }); 
    
    setCancelListener(new OnDialogCancel() {
      @Override
      public void doOnCancel() {
        setVisible(false);
      }
    });
  }
  
  /**
   * This method show the dialog with a small delay.
   */
  public void showProgress() {
    show(2000);
  }
  
}
