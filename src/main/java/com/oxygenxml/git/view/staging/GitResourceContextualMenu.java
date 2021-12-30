package com.oxygenxml.git.view.staging;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.staging.actions.DiscardAction;
import com.oxygenxml.git.view.staging.actions.OpenAction;
import com.oxygenxml.git.view.staging.actions.ShowBlameForUnstagedResourceAction;
import com.oxygenxml.git.view.staging.actions.StageUnstageResourceAction;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

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
	private static final Logger LOGGER = Logger.getLogger(GitResourceContextualMenu.class.getName());
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
   * Constructor.
   *
   * @param selResProvider        Provides the resources that will be processed by the menu's actions.
   * @param gitController         Git controller.
   * @param historyController     History interface.
   * @param isStage               <code>true</code> if we create the menu for the staged resources,
   *                                  <code>false</code> for the unstaged resources.
	 * @param repoStateOptional     Repository state.
   */
  public GitResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      GitControllerBase gitController,
      HistoryController historyController,
      boolean isStage,
      Optional<RepositoryState> repoStateOptional) {
    this.gitCtrl = gitController;
    this.historyController = historyController;
    this.repoState = repoStateOptional.orElse(null);
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
		JMenu resolveConflict = new JMenu();
		resolveConflict.setText(TRANSLATOR.getTranslation(Tags.RESOLVE_CONFLICT));
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
			historyAction.setEnabled(FileUtil.shouldEnableBlameAndHistory(allSelectedResources));
			this.add(historyAction);
			blameAction.setEnabled(FileUtil.shouldEnableBlameAndHistory(allSelectedResources));
			this.add(blameAction);
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
        DiffPresenter.showDiff(selectedLeaves.get(0), gitCtrl);
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
						int answer = FileStatusDialog.showWarningMessageWithConfirmation(
										TRANSLATOR.getTranslation(Tags.MARK_RESOLVED),
										TRANSLATOR.getTranslation(Tags.CONFLICT_MARKERS_MESSAGE),
										TRANSLATOR.getTranslation(Tags.RESOLVE_ANYWAY),
										TRANSLATOR.getTranslation(Tags.CANCEL)
										
						);
						if(answer == FileStatusDialog.RESULT_OK) {
						  gitCtrl.asyncAddToIndex(allSelectedResources);
						}
          } else {
            gitCtrl.asyncAddToIndex(allSelectedResources);
          }
        } catch (Exception err) {
          LOGGER.error(err, err);
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
