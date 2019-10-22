package com.oxygenxml.git.view;

import java.awt.FlowLayout;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommandEvent;

import ro.sync.exml.workspace.api.standalone.ui.Button;

/**
 * Rebase panel. "Abort rebase", "Continue rebase" buttons.
 */
public class RebasePanel extends JPanel {
  /**
   * i18n
   */
  private Translator translator = Translator.getInstance();
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RebasePanel.class.getName());

  /**
   * Constructor.
   */
  public RebasePanel() {
    setVisible(false);
    createGUI();
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        updateVisibilityBasedOnRepoState();
      }

      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        GitCommandEvent cmd = changeEvent.getGitCommandState();
        if (cmd == GitCommandEvent.ABORT_REBASE_ENDED 
            || cmd == GitCommandEvent.CONTINUE_REBASE_ENDED) {
          RebasePanel.this.setVisible(false);
        }
      }
    });
  }
  
  /**
   * Create the graphical user interface.
   */
  private void createGUI() {
    setLayout(new FlowLayout(
        FlowLayout.LEFT,
        2 *  UIConstants.COMPONENT_LEFT_PADDING,
        0));

    Button abortRebaseButton = new Button(translator.getTranslation(Tags.ABORT_REBASE));
    abortRebaseButton.addActionListener(e -> GitAccess.getInstance().abortRebase());
    add(abortRebaseButton);

    Button continueRebaseButton = new Button(translator.getTranslation(Tags.CONTINUE_REBASE));
    continueRebaseButton.addActionListener(e -> GitAccess.getInstance().continueRebase());
    add(continueRebaseButton);
  }
  
  /**
   * Update panel visibility.
   */
  public void updateVisibilityBasedOnRepoState() {
    boolean shouldBeVisible = false;
    try {
      Repository repository = GitAccess.getInstance().getRepository();
      RepositoryState repoState = repository.getRepositoryState();
      shouldBeVisible = repoState == RepositoryState.REBASING
          || repoState == RepositoryState.REBASING_MERGE
          || repoState == RepositoryState.REBASING_REBASING;
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
    RebasePanel.this.setVisible(shouldBeVisible);
  }
  
}
