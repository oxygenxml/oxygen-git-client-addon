package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RemoteNotFoundException;
import com.oxygenxml.git.service.internal.PullConfig;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.AdvancedPullDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.ProgressDialog;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.event.GitController;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * The action to configure an advanced pull operation.
 * 
 * @author alex_smarandache
 */
public class AdvancedPullAction extends GitAbstractAction {
  
  /**
   * Logger for logging.
   */
private static final Logger LOGGER = LoggerFactory.getLogger(PullAction.class);
  
  /**
   * The Git Controller.
   */
  private final transient GitController gitController;
  
  /**
   * Constructor.
   * 
   * @param gitController   Git Controller.
   */
  public AdvancedPullAction(final GitController gitController) {
    super(Translator.getInstance().getTranslation(Tags.PULL) + "...");
    this.gitController = gitController;
  }

  
  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      if (gitController.getGitAccess().getRepository() != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Advanced pull action invoked");
        }
        AdvancedPullDialog pullToConfigDialog = new AdvancedPullDialog(gitController);
        pullToConfigDialog.showDialog();
        if(pullToConfigDialog.getResult() == OKCancelDialog.RESULT_OK) {
          PullConfig pullConfig = pullToConfigDialog.getPullConfig();
          if(pullConfig != null) {
            GitOperationProgressMonitor progressMonitor = new GitOperationProgressMonitor(
                new ProgressDialog(
                    Translator.getInstance().getTranslation(Tags.PULL),
                    true));
            gitController.pull(pullConfig, Optional.of(progressMonitor));
          }
        }
      }
    } catch (NoRepositorySelected | URISyntaxException ex) {
      LOGGER.error(ex.getMessage(), ex);
    } catch (RemoteNotFoundException ex) {
      treatRemoteNotFound(ex);
    }
  }


  /**
   * Treat the exception when the remote is not found. 
   * 
   * @param ex The exception.
   */
  private void treatRemoteNotFound(RemoteNotFoundException ex) {
    if(RemoteNotFoundException.STATUS_BRANCHES_NOT_EXIST == ex.getStatus()) {
      MessagePresenterProvider.getBuilder(
          Translator.getInstance().getTranslation(Tags.PULL), DialogType.ERROR)
      .setMessage(Translator.getInstance().getTranslation(Tags.NO_BRANCHES_FOUNDED))
      .setCancelButtonVisible(false)
      .setOkButtonName(Translator.getInstance().getTranslation(Tags.CLOSE))
      .buildAndShow();  
    } else {
      OKCancelDialog addRemoteDialog = new AddRemoteDialog();
      addRemoteDialog.setVisible(true);
      if(addRemoteDialog.getResult() == OKCancelDialog.RESULT_OK) {
        actionPerformed(null);
      }
    }
  }
  
}

