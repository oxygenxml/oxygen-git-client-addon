package com.oxygenxml.git.view.history.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.oxygenxml.git.auth.AuthUtil;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Translator;

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
 * Constructor
 * 
 * @param commitCharacteristics
 */
  public CreateTagAction(String commitId) {
    super("create Tag here");
    this.commitId = commitId;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitAccess.getInstance().tagCommit("testTag43s", "test_message", commitId);
    CredentialsProvider credentialsProvider = AuthUtil.getCredentialsProvider(GitAccess.getInstance().getHostName());
    try {
      GitAccess.getInstance().getGit().push().setCredentialsProvider(credentialsProvider).setPushTags().call();
    } catch (GitAPIException e1) {
      e1.printStackTrace();
    }
  }

}
