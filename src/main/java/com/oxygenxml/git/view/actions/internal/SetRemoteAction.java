package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.service.exceptions.RemoteNotFoundException;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Action to set remote for current branch.
 * 
 * @author alex_smarandache
 *
 */
public class SetRemoteAction extends GitAbstractAction {

  /**
   * The translator for the messages that are displayed in this panel
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedPullAction.class);

  /**
   * Constructor.
   */
  public SetRemoteAction() {
    super(TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH) +  "...");
  }


  @Override
  public void actionPerformed(ActionEvent arg0) {
    setRemote();
  }


  /**
   * Tries to set the remote for current branch.
   */
  private void setRemote() {
    try {
      new CurrentBranchRemotesDialog().showDialog();
    } catch (NoRepositorySelected | URISyntaxException ex) {
      LOGGER.error(ex.getMessage(), ex);
    } catch(RemoteNotFoundException ex) {
      if(ex.getStatus() == RemoteNotFoundException.STATUS_REMOTE_NOT_EXISTS) {
        OKCancelDialog addRemoteDialog = new AddRemoteDialog();
        addRemoteDialog.setVisible(true);
        if(addRemoteDialog.getResult() == OKCancelDialog.RESULT_OK) {
          setRemote();
        }
      } else if(ex.getStatus() == RemoteNotFoundException.STATUS_BRANCHES_NOT_EXIST) {
        MessagePresenterProvider.getBuilder(
            TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH), DialogType.ERROR)
        .setMessage(TRANSLATOR.getTranslation(Tags.NO_BRANCHES_FOUNDED))
        .setCancelButtonVisible(false)
        .setOkButtonName(TRANSLATOR.getTranslation(Tags.CLOSE))
        .buildAndShow();  
      }
    }
  }

}
