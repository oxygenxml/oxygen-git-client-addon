package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.internal.PullConfig;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.AdvancedPullDialog;
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
        pullToConfigDialog.setVisible(true);
        if(pullToConfigDialog.getResult() == OKCancelDialog.RESULT_OK) {
          PullConfig pullConfig = pullToConfigDialog.getPullConfig();
          if(pullConfig != null) {
            gitController.pull(pullConfig, Optional.empty());
          }
        }
      }
    } catch (NoRepositorySelected e1) {
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug(e1.getMessage(), e1);
      }
    }
  }
  
}

