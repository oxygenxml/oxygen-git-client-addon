package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.tags.CreateTagDialog;

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
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(CreateTagAction.class.getName());
  /**
   * The ID of the commit used for tag.
   */
  private String commitId;
  
/**
 * Constructor that creates a tag action
 *  
 * @param commitId The id of the commit
 */
  public CreateTagAction(String commitId) {
    super(translator.getTranslation(Tags.CREATE_TAG) + "...");
    this.commitId = commitId;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    CreateTagDialog dialog = new CreateTagDialog();
    String tagTitle = dialog.getTagTitle();
    String tagMessage = dialog.getTagMessage();
    if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
      GitOperationScheduler.getInstance().schedule(() -> {
        try {
          GitAccess.getInstance().tagCommit(tagTitle, tagMessage, commitId);
          if(dialog.shouldPushNewTag()) {
            GitAccess.getInstance().pushTag(tagTitle);
          }
        } catch (GitAPIException | RevisionSyntaxException | NoRepositorySelected | IOException ex) {
          LOGGER.debug(ex.getMessage(), ex);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);
        }
      });
    }

  }
  
}
