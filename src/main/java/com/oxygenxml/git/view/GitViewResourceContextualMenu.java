package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitController;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.history.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Contextual menu shown for staged/unstaged resources from the Git view 
 * (either tree or list rendering).
 * 
 * @author Beniamin Savu
 * 
 */
public class GitViewResourceContextualMenu extends JPopupMenu {
	/**
	 * The translator used for the contextual menu names
	 */
	private Translator translator = Translator.getInstance();

	/**
	 * Controller used for staging and unstaging
	 */
	private GitController gitCtrl;

	/**
	 * The git API, containg the commands
	 */
	private GitAccess gitAccess = GitAccess.getInstance();
	
	/**
	 * Repository state.
	 */
	private RepositoryState repoState;

	/**
	 * History interface.
	 */
    private HistoryController historyController;

  /**
   * Constructor.
   * 
   * @param selResProvider        Provides the resources that will be processed by the menu's actions. 
   * @param gitController       Staging controller.
   * @param historyController     History interface.
   * @param isStage               <code>true</code> if we create the menu for the staged resources,
   *                                  <code>false</code> for the unstaged resources.
	 * @param repoState             Repository state.
   */
  public GitViewResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      GitController gitController,
      HistoryController historyController,
      boolean isStage,
      RepositoryState repoState) {
    this.gitCtrl = gitController;
    this.historyController = historyController;
    this.repoState = repoState;
    populateMenu(selResProvider, isStage);
  }

	/**
	 * Populates the contextual menu for the selected files.
	 * 
	 * @param selResProvider   Provides the resources that will be processed by the menu's actions.
	 * @param forStagedRes  <code>true</code> if the contextual menu is created for staged files.
	 */
	private void populateMenu(
	    final SelectedResourcesProvider selResProvider, 
	    final boolean forStagedRes) {
	  StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
	  if (!selResProvider.getAllSelectedResources().isEmpty() || isRepoMergingOrRebasing()) {
	    final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
	    final List<FileStatus> selectedLeaves = selResProvider.getOnlySelectedLeaves();

	    // "Open in compare editor" action
	    AbstractAction showDiffAction = new AbstractAction(
	        translator.getTranslation(Tags.OPEN_IN_COMPARE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        DiffPresenter.showDiff(selectedLeaves.get(0), gitCtrl);
	      }
	    };

	    // "Open" action
	    AbstractAction openAction = new OpenAction(selResProvider);

	    // "Stage"/"Unstage" actions
	    AbstractAction stageUnstageAction = new StageUnstageResourceAction(
	        allSelectedResources, 
	        // If this contextual menu is built for a staged resource,
	        // then the action should be unstage.
	        !forStagedRes, 
	        gitCtrl);

	    // Resolve using "mine"
	    AbstractAction resolveUsingMineAction = new AbstractAction(
	        translator.getTranslation(Tags.RESOLVE_USING_MINE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        gitCtrl.asyncResolveUsingMine(allSelectedResources);
	      }
	    };

	    // Resolve using "theirs"
	    AbstractAction resolveUsingTheirsAction = new AbstractAction(
	        translator.getTranslation(Tags.RESOLVE_USING_THEIRS)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        gitCtrl.asyncResolveUsingTheirs(allSelectedResources);
	      }
	    };

	    // "Mark resolved" action
	    AbstractAction markResolvedAction = new AbstractAction(
	        translator.getTranslation(Tags.MARK_RESOLVED)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        gitCtrl.asyncAddToIndex(allSelectedResources);
	      }
	    };

	    // "Restart Merge" action
	    AbstractAction restartMergeAction = new AbstractAction(
	        translator.getTranslation(Tags.RESTART_MERGE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        String[] options = new String[] { 
	            "   " + translator.getTranslation(Tags.YES) + "   ",
	            "   " + translator.getTranslation(Tags.NO) + "   "};
	        int[] optionIds = new int[] { 0, 1 };
          int result = pluginWS.showConfirmDialog(
	            translator.getTranslation(Tags.RESTART_MERGE),
	            translator.getTranslation(Tags.RESTART_MERGE_CONFIRMATION),
	            options,
	            optionIds);
	        if (result == optionIds[0]) {
	          gitAccess.restartMerge();
	        }
	      }
	    };
	    
	    // "Discard" action 
      AbstractAction discardAction = new DiscardAction(selResProvider, gitCtrl);

	    // Resolve Conflict
	    JMenu resolveConflict = new JMenu();
	    resolveConflict.setText(translator.getTranslation(Tags.RESOLVE_CONFLICT));
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
	    addSeparator();
	    this.add(stageUnstageAction);
	    this.add(resolveConflict);
	    this.add(discardAction);

	    if (!forStagedRes) {
	      addSeparator();
	      
	      // Show history
	      AbstractAction historyAction = new AbstractAction(translator.getTranslation(Tags.SHOW_HISTORY)) {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	          if (!allSelectedResources.isEmpty()) {
	            historyController.showResourceHistory(allSelectedResources.get(0).getFileLocation());
	          }
	        }
	      };
	      historyAction.setEnabled(shouldEnableBlameAndHistory(allSelectedResources));
	      this.add(historyAction);

	      // Show blame
	      AbstractAction blameAction = new ShowBlameForUnstagedResourceAction(historyController, selResProvider);
	      blameAction.setEnabled(shouldEnableBlameAndHistory(allSelectedResources));
	      this.add(blameAction);
	    }

	    boolean allSelResHaveSameChangeType = true;
	    boolean selectionContainsConflicts = false;
	    boolean selectionContainsSubmodule = false;
	    boolean selectionContainsDeletions = false;

	    if (!allSelectedResources.isEmpty()) {
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
	    }

	    // Enable/disable the actions
	    showDiffAction.setEnabled(selectedLeaves.size() == 1);
	    openAction.setEnabled(!selectionContainsDeletions && !selectionContainsSubmodule && !allSelectedResources.isEmpty());
	    stageUnstageAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
	    resolveConflict.setEnabled(isRepoMergingOrRebasing());
	    resolveUsingMineAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
	    resolveUsingTheirsAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
	    markResolvedAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
	    restartMergeAction.setEnabled(isRepoMergingOrRebasing());
	    discardAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
	  }
	}

	/**
	 * Checks if we have just one resource and if it's a resource that is committed in the repository.
	 * 
	 * @param allSelectedResources A set of resources.
	 * 
	 * @return <code>true</code> if we have just one resource in the set and that resource is one with history.
	 */
  private boolean shouldEnableBlameAndHistory(final List<FileStatus> allSelectedResources) {
    boolean hasHistory = false;
    if (allSelectedResources.size() == 1) {
      GitChangeType changeType = allSelectedResources.get(0).getChangeType(); 
      hasHistory = 
          changeType == GitChangeType.CHANGED || 
          changeType == GitChangeType.CONFLICT ||
          changeType == GitChangeType.MODIFIED;
    }
    return hasHistory;
  }
	
	 /**
   * Get repo state
   * 
   * @return <code>true</code> if the repository merging or rebasing.
   */
  private boolean isRepoMergingOrRebasing() {
    boolean toReturn = false;
    if (repoState != null) {
      toReturn = repoState == RepositoryState.MERGING
          || repoState == RepositoryState.MERGING_RESOLVED
          || repoState == RepositoryState.REBASING
          || repoState == RepositoryState.REBASING_MERGE
          || repoState == RepositoryState.REBASING_REBASING;
    }
    return toReturn;
  }
}
