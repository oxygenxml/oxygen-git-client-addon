package com.oxygenxml.git.view.staging;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.eclipse.jgit.lib.RepositoryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.staging.actions.DiscardAction;
import com.oxygenxml.git.view.staging.actions.OpenAction;
import com.oxygenxml.git.view.staging.actions.ShowBlameForUnstagedResourceAction;
import com.oxygenxml.git.view.staging.actions.StageUnstageResourceAction;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Contextual menu shown for staged/unstaged resources from the Git view
 * (either tree or list rendering).
 *
 * @author Beniamin Savu
 *
 */
public class GitResourceContextualMenu extends JPopupMenu {
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER =  LoggerFactory.getLogger(GitResourceContextualMenu.class.getName());
	/**
	 * The translator used for the contextual menu names
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Controller used for staging and unstaging
	 */
	private final GitControllerBase gitCtrl;

	/**
	 * The git API, containg the commands
	 */
	private static final GitAccess GIT_ACCESS = GitAccess.getInstance();

	/**
	 * Repository state.
	 */
	private final RepositoryState repoState;

	/**
	 * History interface.
	 */
  private final HistoryController historyController;
  /**
   * The Standalone Plugin Workspace;
   */
  private static final PluginWorkspace PLUGIN_WS = PluginWorkspaceProvider.getPluginWorkspace();
  /**
   * Show differences action.
   */
  private AbstractAction showDiffAction;
  /**
   * Open action.
   */
  private AbstractAction openAction;
  /**
   * Stage - Unstage action.
   */
  private AbstractAction stageUnstageAction;
  /**
   * Resolve using mine action.
   */
	private AbstractAction resolveUsingMineAction;
	/**
	 * Resolve using theirs action.
	 */
	private AbstractAction resolveUsingTheirsAction;
	/**
	 * Mark resolved action.
	 */
	private AbstractAction markResolvedAction;
	/**
	 * Restart merge action.
	 */
	private AbstractAction restartMergeAction;
	/**
	 * Discard action.
	 */
	private AbstractAction discardAction;
	/**
	 * History action.
	 */
	private AbstractAction historyAction;
	/**
	 * Show blame action.
	 */
	private AbstractAction blameAction;
	
  /**
   * Responsible for repository and UI refreshes.
   */
  private final GitRefreshSupport refreshManager;


  /**
   * Constructor.
   *
   * @param selResProvider        Provides the resources that will be processed by the menu's actions.
   * @param gitController         Git controller.
   * @param historyController     History interface.
   * @param isStage               <code>true</code> if we create the menu for the staged resources,
   *                                  <code>false</code> for the unstaged resources.
	 * @param repoStateOptional     Repository state.
	 * @param refreshManager      Responsible for refresh when needed.
   */
  public GitResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      GitControllerBase gitController,
      HistoryController historyController,
      boolean isStage,
      Optional<RepositoryState> repoStateOptional, 
      final GitRefreshSupport refreshSupport) {
    this.gitCtrl = gitController;
    this.historyController = historyController;
    this.repoState = repoStateOptional.orElse(null);
    this.refreshManager = refreshSupport;
    populateMenu(selResProvider, isStage);
  }


	/**
	 * Populates the contextual menu for the selected files.
	 *
	 * @param selResProvider   Provides the resources that will be processed by the menu's actions.
	 * @param forStagedRes     <code>true</code> if the contextual menu is created for staged files.
	 */
	private void populateMenu(
	    final SelectedResourcesProvider selResProvider,
	    final boolean forStagedRes) {

	  if (selResProvider.getAllSelectedResources().isEmpty() && !RepoUtil.isUnfinishedConflictState(repoState)) {
      return;
    }

		final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
		final List<FileStatus> selectedLeaves = selResProvider.getOnlySelectedLeaves();

		createAllActions(allSelectedResources, selectedLeaves, selResProvider, forStagedRes);

		// Resolve Conflict
		final JMenu resolveConflict = OxygenUIComponentsFactory.createMenu(
		    TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICT));
		resolveConflict.add(OxygenUIComponentsFactory.createMenuItem(showDiffAction));
		resolveConflict.addSeparator();
		resolveConflict.add(OxygenUIComponentsFactory.createMenuItem(resolveUsingMineAction));
		resolveConflict.add(OxygenUIComponentsFactory.createMenuItem(resolveUsingTheirsAction));
    if (1 == selResProvider.getAllSelectedResources().size()) {
      //only one resource can be marked as resolved EXM-53689
      resolveConflict.add(OxygenUIComponentsFactory.createMenuItem(markResolvedAction));
    }
		resolveConflict.addSeparator();
		resolveConflict.add(OxygenUIComponentsFactory.createMenuItem(restartMergeAction));

		// Populate contextual menu
		this.add(OxygenUIComponentsFactory.createMenuItem(showDiffAction));
		this.add(OxygenUIComponentsFactory.createMenuItem(openAction));
		addSeparator();
		this.add(OxygenUIComponentsFactory.createMenuItem(stageUnstageAction));
		this.add(resolveConflict);
		this.add(OxygenUIComponentsFactory.createMenuItem(discardAction));

	if (!forStagedRes) {
			addSeparator();
			historyAction.setEnabled(FileUtil.shouldEnableBlameAndHistory(allSelectedResources));
			this.add(OxygenUIComponentsFactory.createMenuItem(historyAction));
			blameAction.setEnabled(FileUtil.shouldEnableBlameAndHistory(allSelectedResources));
			this.add(OxygenUIComponentsFactory.createMenuItem(blameAction));
		}

		boolean allSelResHaveSameChangeType = true;
		boolean selectionContainsConflicts = false;
		boolean selectionContainsDeletions = false;

		if (!allSelectedResources.isEmpty()) {
			GitChangeType firstChangeType = allSelectedResources.get(0).getChangeType();
			for (FileStatus file : allSelectedResources) {
				GitChangeType changeType = file.getChangeType();
				allSelResHaveSameChangeType = changeType == firstChangeType && allSelResHaveSameChangeType;
				switch (changeType) {
					case CONFLICT:
						selectionContainsConflicts = true;
						break;
					case MISSING:
					  // jump to the next case
					case REMOVED:
						selectionContainsDeletions = true;
						break;
					default:
					  break;
				}
			}
		}

		// Enable/disable the actions
		showDiffAction.setEnabled(selectedLeaves.size() == 1);
		openAction.setEnabled(!selectionContainsDeletions && !allSelectedResources.isEmpty());
		stageUnstageAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
		resolveConflict.setEnabled(RepoUtil.isUnfinishedConflictState(repoState) || selectionContainsConflicts);
		resolveUsingMineAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		resolveUsingTheirsAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		markResolvedAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		restartMergeAction.setEnabled(RepoUtil.isRepoMergingOrRebasing(repoState));
		discardAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
	}


	/**
	 * Creates actions for contextual menu.
	 *  @param allSelectedResources  A list with all selected resources.
	 * @param selectedLeaves         A list of FileStatus with selected leaves.
	 * @param selResProvider         Provides the resources that will be processed by the menu's actions.
	 * @param forStagedRes           <code>true</code> if the contextual menu is created for staged files.
	 */
	private void createAllActions(
					final List<FileStatus> allSelectedResources,
					final List<FileStatus> selectedLeaves,
					final SelectedResourcesProvider selResProvider,
					final boolean forStagedRes
	) {
	// "Open in compare editor" action
    showDiffAction = new AbstractAction(
            TRANSLATOR.getTranslation(Tags.OPEN_IN_COMPARE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        DiffPresenter.showDiff(selectedLeaves.get(0), gitCtrl, refreshManager);
      }
    };

    // "Open" action
    openAction = new OpenAction(selResProvider);

    // "Stage"/"Unstage" actions
    stageUnstageAction = new StageUnstageResourceAction(
            allSelectedResources,
            // If this contextual menu is built for a staged resource,
            // then the action should be unstage.
            !forStagedRes,
            gitCtrl);

    // Resolve using "mine"
    resolveUsingMineAction = new AbstractAction(
            TRANSLATOR.getTranslation(Tags.RESOLVE_USING_MINE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitCtrl.asyncResolveUsingMine(allSelectedResources);
      }
    };

    // Resolve using "theirs"
    resolveUsingTheirsAction = new AbstractAction(
            TRANSLATOR.getTranslation(Tags.RESOLVE_USING_THEIRS)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitCtrl.asyncResolveUsingTheirs(allSelectedResources);
      }
    };


    // "Mark resolved" action
    markResolvedAction = new AbstractAction(
            TRANSLATOR.getTranslation(Tags.MARK_RESOLVED)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          if(FileUtil.containsConflictMarkers(allSelectedResources, GIT_ACCESS.getWorkingCopy())) {
						final int answer = MessagePresenterProvider.getBuilder(
						    TRANSLATOR.getTranslation(Tags.MARK_RESOLVED), DialogType.WARNING)
                .setQuestionMessage(TRANSLATOR.getTranslation(Tags.CONFLICT_MARKERS_MESSAGE))
                .setOkButtonName(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICT))
                .setCancelButtonName(TRANSLATOR.getTranslation(Tags.CANCEL))
                .buildAndShow().getResult();								
            if (answer == OKCancelDialog.RESULT_OK) {
                // "Mark resolved" only has problems
                for (FileStatus fileStatus : selectedLeaves) {
                  DiffPresenter.showDiff(fileStatus, gitCtrl);
                }
 
            }
          } else {
            gitCtrl.asyncAddToIndex(allSelectedResources);
          }
        } catch (Exception err) {
          LOGGER.error(err.getMessage(), err);
        }
      }
    };


    // "Restart Merge" action
    restartMergeAction = new AbstractAction(
            TRANSLATOR.getTranslation(Tags.RESTART_MERGE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        String[] options = new String[] {
                "   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
                "   " + TRANSLATOR.getTranslation(Tags.NO) + "   "};
        int[] optionIds = new int[] { 0, 1 };
        int result = GitResourceContextualMenu.PLUGIN_WS.showConfirmDialog(
                TRANSLATOR.getTranslation(Tags.RESTART_MERGE),
                TRANSLATOR.getTranslation(Tags.RESTART_MERGE_CONFIRMATION),
                options,
                optionIds);
        if (result == optionIds[0]) {
          GIT_ACCESS.restartMerge();
        }
      }
    };


    // "Discard" action
    discardAction = new DiscardAction(selResProvider, gitCtrl);
    
    if(!forStagedRes) {
      // Show history
      historyAction = new AbstractAction(TRANSLATOR.getTranslation(Tags.SHOW_HISTORY)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!allSelectedResources.isEmpty()) {
            historyController.showResourceHistory(allSelectedResources.get(0).getFileLocation());
          }
        }
      };
      
      // "Blame" action
      blameAction = new ShowBlameForUnstagedResourceAction(historyController, selResProvider);
    }
	}

}
