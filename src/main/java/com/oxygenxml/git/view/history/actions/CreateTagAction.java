package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;

import com.oxygenxml.git.auth.AuthUtil;
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
  private static final Logger LOGGER = LogManager.getLogger(CreateBranchFromCommitAction.class.getName());
  
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
    
    if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
      GitOperationScheduler.getInstance().schedule(() -> {
        try {
          GitAccess.getInstance().tagCommit(dialog.getTagTitle(), tagTitle, commitId);
          if(dialog.shouldPushNewTag()) {
            CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
            GitAccess.getInstance().getGit()
              .push()
              .setCredentialsProvider(credentialsProvider)
              .setRefSpecs(new RefSpec("refs/tags/"+ tagTitle +":refs/tags/" + tagTitle))
              .call();
          }
        } catch (GitAPIException ex) {
          LOGGER.debug(ex);
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(ex.getMessage(), ex);}
      });
    }

  }
}
