package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Contextual menu shown for staged/unstaged resources from the Git view 
 * (either tree or list rendering).
 * 
 * @author Beniamin Savu
 * 
 */
public class GitViewResourceContextualMenu extends JPopupMenu {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitViewResourceContextualMenu.class);

	/**
	 * The translator used for the contextual menu names
	 */
	private Translator translator = Translator.getInstance();

	/**
	 * Controller used for staging and unstaging
	 */
	private StageController stageController;

	/**
	 * The git API, containg the commands
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
   * Constructor.
   * 
   * @param stageController  Staging controller.
   * @param isStage          <code>true</code> if we create the menu for the staged resources.
   */
  public GitViewResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      StageController stageController,
      boolean isStage) {
    this.stageController = stageController;
    populateMenu(selResProvider, isStage);
  }

	/**
	 * Populates the contextual menu for the selected files.
	 * 
	 * @param forStagedRes  <code>true</code> if the contextual menu is created for staged files.
	 */
	private void populateMenu(final SelectedResourcesProvider selResProvider, final boolean forStagedRes) {
	  if (!selResProvider.getAllSelectedResources().isEmpty()) {
	    final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
	    final List<FileStatus> selectedLeaves = selResProvider.getOnlySelectedLeaves();

	    // "Open in compare editor" action
	    AbstractAction showDiffAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        new DiffPresenter(selectedLeaves.get(0), stageController).showDiff();
	      }
	    };

	    // "Open" action
	    AbstractAction openAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        for (FileStatus file : allSelectedResources) {
	          try {
	            URL fileURL = null;
	            if (file.getChangeType() == GitChangeType.ADD) {
	              fileURL = GitRevisionURLHandler.encodeURL(
	                  VersionIdentifier.INDEX_OR_LAST_COMMIT,
	                  file.getFileLocation());
	            } else {
	              fileURL = FileHelper.getFileURL(file.getFileLocation());  
	            }
	            PluginWorkspaceProvider.getPluginWorkspace().open(fileURL);
	          } catch (Exception ex) {
	            logger.error(ex, ex);
	          }
	        }
	      }
	    };

	    // "Stage"/"Unstage" actions
	    AbstractAction stageUnstageAction = new StageUnstageResourceAction(
	        allSelectedResources, 
	        // If this contextual menu is built for a staged resource,
	        // then the action should be unstage.
	        !forStagedRes, 
	        stageController);

	    // Resolve using "mine"
	    AbstractAction resolveUsingMineAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_MINE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.RESOLVE_USING_MINE);
	      }
	    };

	    // Resolve using "theirs"
	    AbstractAction resolveUsingTheirsAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_THEIRS)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.RESOLVE_USING_THEIRS);
	      }
	    };

	    // "Mark resolved" action
	    AbstractAction markResolvedAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_MARK_RESOLVED)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.STAGE);
	      }
	    };

	    // "Restart Merge" action
	    AbstractAction restartMergeAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESTART_MERGE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        gitAccess.restartMerge();
	      }
	    };
	    
	    // "Discard" action 
      AbstractAction discardAction = new DiscardAction(allSelectedResources, stageController);

	    // Resolve Conflict
	    JMenu resolveConflict = new JMenu();
	    resolveConflict.setText(translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_CONFLICT));
	    resolveConflict.add(showDiffAction);
	    resolveConflict.addSeparator();
	    resolveConflict.add(resolveUsingMineAction);
	    resolveConflict.add(resolveUsingTheirsAction);
	    resolveConflict.add(markResolvedAction);
	    resolveConflict.addSeparator();
	    resolveConflict.add(restartMergeAction);

	    // Populate contextual menu
	    this.add(showDiffAction);
	    this.add(openAction);
	    this.add(stageUnstageAction);
	    this.add(resolveConflict);
	    this.add(discardAction);

	    boolean allSelResHaveSameChangeType = true;
	    boolean selectionContainsConflicts = false;
	    boolean selectionContainsSubmodule = false;
	    boolean selectionContainsDeletions = false;

	    GitChangeType firstChangeType = allSelectedResources.get(0).getChangeType();
	    for (FileStatus file : allSelectedResources) {
	      GitChangeType changeType = file.getChangeType();
        if (changeType != firstChangeType) {
	        allSelResHaveSameChangeType = false;
	      }
	      if (changeType == GitChangeType.CONFLICT) {
	        selectionContainsConflicts = true;
	      } else if (changeType == GitChangeType.SUBMODULE) {
	        selectionContainsSubmodule = true;
	      } else if (changeType == GitChangeType.MISSING || changeType == GitChangeType.REMOVED) {
	        selectionContainsDeletions = true;
	      }
	    }

	    // Enable/disable the actions
	    showDiffAction.setEnabled(selectedLeaves.size() == 1);
	    openAction.setEnabled(!selectionContainsDeletions && !selectionContainsSubmodule);
	    stageUnstageAction.setEnabled(!selectionContainsConflicts);
	    resolveConflict.setEnabled(isRepoInMergingState());
	    resolveUsingMineAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType);
	    resolveUsingTheirsAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType);
	    markResolvedAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType);
	    restartMergeAction.setEnabled(isRepoInMergingState());
	    discardAction.setEnabled(!selectionContainsConflicts);
	  }
	}

	/**
	 * Check if the repository is in merging state.
	 * 
	 * @return <code>true</code> if the repository is in merging state.
	 */
  private boolean isRepoInMergingState() {
    boolean toReturn = false;
    try {
      toReturn =  getRepositoryState() == RepositoryState.MERGING_RESOLVED
          || getRepositoryState() == RepositoryState.MERGING;
    } catch (NoRepositorySelected e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    }
    return toReturn;
  }

  /**
   * Get repository state.
   * 
   * @return the repository state.
   * 
   * @throws NoRepositorySelected
   */
  protected RepositoryState getRepositoryState() throws NoRepositorySelected {
    return gitAccess.getRepository().getRepositoryState();
  }
}
