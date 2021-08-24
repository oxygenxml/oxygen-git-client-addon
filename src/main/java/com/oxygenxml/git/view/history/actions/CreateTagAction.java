package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Create an action that creates a Tag
 * 
 * @author gabriel_nedianu
 */
public class CreateTagAction extends AbstractAction {
  /**
   * i18n
   */
  private static Translator translator = Translator.getInstance();
  /**
   * The ID of the commit used for tag.
   */
  private String commitId;
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LogManager.getLogger(CreateTagAction.class.getName());
  
/**
 * Constructor
 * 
 * @param commitCharacteristics
 */
  public CreateTagAction(String commitId) {
    super("Create tag for this commit");
    this.commitId = commitId;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    CreateTagDialog dialog = new CreateTagDialog("Tag commit");
    String tagTitle = dialog.getTagTitle();
    String tagMessage = dialog.getTagMessage();
    if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
      GitOperationScheduler.getInstance().schedule(() -> {
        try {
          GitAccess.getInstance().tagCommit(tagTitle, tagMessage, commitId);
          if(dialog.shouldPushNewTag()) {
            GitAccess.getInstance().pushTag(tagTitle);
          }
        } catch (GitAPIException ex) {
          LOGGER.debug(ex);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);}
      });
    }

  }
  
}
