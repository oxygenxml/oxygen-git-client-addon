package com.oxygenxml.git.view;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.standalone.ui.Button;

/**
 * Panel that shows corresponding buttons when the repo is in conflict state.
 * The buttons can be "Abort merge", "Abort rebase", "Continue rebase".
 */
public class ConflictButtonsPanel extends JPanel {
  /**
   * i18n
   */
  private static final Translator translator = Translator.getInstance();
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(ConflictButtonsPanel.class.getName());
  /**
   * Merge conflict panel ID.
   */
  private static final String MERGE_CONFLICT_PANEL = "MERGE_CONFLICT_PANEL";
  /**
   * Rebase conflict panel ID.
   */
  private static final String REBASE_CONFLICT_PANEL = "REBASE_CONFLICT_PANEL";

  /**
   * Constructor.
   * 
   * @param gitController Git operations controller.
   */
  public ConflictButtonsPanel(GitControllerBase gitController) {
    setVisible(false);
    createGUI();
    gitController.addGitListener(new GitEventAdapter() {
      @Override
      public void operationAboutToStart(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (isPullStrategyEndingOperation(operation)) {
          ConflictButtonsPanel.this.setEnabled(false);
        }
      }
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        GitOperation operation = info.getGitOperation();
        if (operation == GitOperation.OPEN_WORKING_COPY 
            || operation == GitOperation.MERGE_RESTART
            || operation == GitOperation.PULL
            // A resolve using theirs or mine fires this kind of event.
            || operation == GitOperation.STAGE) {
          updateBasedOnRepoState();
        } else if (isPullStrategyEndingOperation(operation)) {
          ConflictButtonsPanel.this.setEnabled(true);
          ConflictButtonsPanel.this.setVisible(false);
        }
      }
      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        GitOperation operation = info.getGitOperation();
        if (isPullStrategyEndingOperation(operation)) {
          ConflictButtonsPanel.this.setEnabled(true);
        }
      }
      private boolean isPullStrategyEndingOperation(GitOperation operation) {
        return operation == GitOperation.ABORT_REBASE 
            || operation == GitOperation.CONTINUE_REBASE
            || operation == GitOperation.ABORT_MERGE;
      }
    });
  }
  
  /**
   * Create the graphical user interface.
   */
  private void createGUI() {
    FlowLayout flowLayout = new FlowLayout(
        FlowLayout.LEFT,
        2 *  UIConstants.COMPONENT_LEFT_PADDING,
        0);
    
    // Merge conflict buttons panel
    JPanel mergeConflictCard = new JPanel(flowLayout);
    Button abortMergeButton = new Button(translator.getTranslation(Tags.ABORT_MERGE));
    abortMergeButton.addActionListener(e -> GitAccess.getInstance().abortMerge());
    mergeConflictCard.add(abortMergeButton);
    
    // Rebase conflict buttons panel
    JPanel rebaseConflictCard = new JPanel(flowLayout);

    Button abortRebaseButton = new Button(translator.getTranslation(Tags.ABORT_REBASE));
    abortRebaseButton.addActionListener(e -> GitAccess.getInstance().abortRebase());
    rebaseConflictCard.add(abortRebaseButton);

    Button continueRebaseButton = new Button(translator.getTranslation(Tags.CONTINUE_REBASE));
    continueRebaseButton.addActionListener(e -> GitAccess.getInstance().continueRebase());
    rebaseConflictCard.add(continueRebaseButton);
    
    // Add to the main panel
    setLayout(new CardLayout());
    add(mergeConflictCard, MERGE_CONFLICT_PANEL);
    add(rebaseConflictCard, REBASE_CONFLICT_PANEL);
  }
  
  /**
   * Update panel.
   */
  public void updateBasedOnRepoState() {
    boolean shouldBeVisible = false;
    try {
      Repository repository = GitAccess.getInstance().getRepository();
      RepositoryState repoState = repository.getRepositoryState();
      boolean isRebaseConflict = isRebaseConflict(repoState);
      boolean isMergeConflict = isMergeConflict(repoState);
      shouldBeVisible = isRebaseConflict || isMergeConflict;
      
      if (shouldBeVisible) {
        CardLayout cardLayout = (CardLayout) ConflictButtonsPanel.this.getLayout();
        if (isMergeConflict) {
          cardLayout.show(this, MERGE_CONFLICT_PANEL);
        } else {
          cardLayout.show(this, REBASE_CONFLICT_PANEL);
        }
        
        setEnabled(true);
      }
    } catch (NoRepositorySelected e) {
      logger.debug(e, e);
    }
    
    setVisible(shouldBeVisible);
  }

  /**
   * Check if the repository is in a merge conflict state.
   * 
   * @param repoState The repo state.
   * 
   * @return <code>true</code> if the repository is in a merge conflict state.
   */
  private boolean isMergeConflict(RepositoryState repoState) {
    return repoState == RepositoryState.MERGING;
  }

  /**
   * Check if the repository is in a rebase conflict state.
   * 
   * @param repoState The repo state.
   * 
   * @return <code>true</code> if the repository is in a rebase conflict state.
   */
  private boolean isRebaseConflict(RepositoryState repoState) {
    return repoState == RepositoryState.REBASING
        || repoState == RepositoryState.REBASING_MERGE
        || repoState == RepositoryState.REBASING_REBASING;
  }
  
  /**
   * @see com.oxygenxml.git.view.ConflictButtonsPanel.setEnabled(boolean enabled)
   */
  @Override
  public void setEnabled(boolean enabled) {
    Component[] components = getComponents();
    setEnabled(enabled, components);
  }

  /**
   * Set enabled.
   * 
   * @param enabled    <code>true</code> to enable.
   * @param components The components to enable or disable.
   */
  private void setEnabled(boolean enabled, Component[] components) {
    for (Component component : components) {
      component.setEnabled(enabled);
      if (component instanceof Container) {
        Container container = (Container) component;
        setEnabled(enabled, container.getComponents());
      }
    }
  }
  
}
