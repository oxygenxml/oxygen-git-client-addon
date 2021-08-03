package com.oxygenxml.git.view.staging;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.oxygenxml.git.options.OptionsManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
   * Constructor.
   * 
   * @param selResProvider        Provides the resources that will be processed by the menu's actions. 
   * @param gitController         Git controller.
   * @param historyController     History interface.
   * @param isStage               <code>true</code> if we create the menu for the staged resources,
   *                                  <code>false</code> for the unstaged resources.
	 * @param repoState             Repository state.
   */
  public GitResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      GitControllerBase gitController,
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
	 * @param forStagedRes     <code>true</code> if the contextual menu is created for staged files.
	 */
	private void populateMenu(
	    final SelectedResourcesProvider selResProvider, 
	    final boolean forStagedRes) {
	  
	  if (selResProvider.getAllSelectedResources().isEmpty() && !RepoUtil.isRepoMergingOrRebasing(repoState)) {
      return;
    }
	  
	  final StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();

		final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
		final List<FileStatus> selectedLeaves = selResProvider.getOnlySelectedLeaves();

		// "Open in compare editor" action
		AbstractAction showDiffAction = new AbstractAction(
						TRANSLATOR.getTranslation(Tags.OPEN_IN_COMPARE)) {
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
						TRANSLATOR.getTranslation(Tags.RESOLVE_USING_MINE)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				gitCtrl.asyncResolveUsingMine(allSelectedResources);
			}
		};

		// Resolve using "theirs"
		AbstractAction resolveUsingTheirsAction = new AbstractAction(
						TRANSLATOR.getTranslation(Tags.RESOLVE_USING_THEIRS)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				gitCtrl.asyncResolveUsingTheirs(allSelectedResources);
			}
		};


		// "Mark resolved" action
		AbstractAction markResolvedAction = new AbstractAction(
						TRANSLATOR.getTranslation(Tags.MARK_RESOLVED)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(OptionsManager.getInstance().shouldNotifyConflictMarkers()
									&& containsConflictMarkers(allSelectedResources, GIT_ACCESS.getWorkingCopy())) {
						pluginWS.showWarningMessage(TRANSLATOR.getTranslation(Tags.CONFLICT_MARKERS_MESSAGE));
					} else {
						gitCtrl.asyncAddToIndex(allSelectedResources);
					}
				} catch (Exception err) {
					LOGGER.error(err, err);
				}
			}
		};


		// "Restart Merge" action
		AbstractAction restartMergeAction = new AbstractAction(
						TRANSLATOR.getTranslation(Tags.RESTART_MERGE)) {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[] {
								"   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
								"   " + TRANSLATOR.getTranslation(Tags.NO) + "   "};
				int[] optionIds = new int[] { 0, 1 };
				int result = pluginWS.showConfirmDialog(
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
		AbstractAction discardAction = new DiscardAction(selResProvider, gitCtrl);

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

			// Show history
			AbstractAction historyAction = new AbstractAction(TRANSLATOR.getTranslation(Tags.SHOW_HISTORY)) {
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
		boolean selectionContainsDeletions = false;

		if (!allSelectedResources.isEmpty()) {
			GitChangeType firstChangeType = allSelectedResources.get(0).getChangeType();
			for (FileStatus file : allSelectedResources) {
				GitChangeType changeType = file.getChangeType();
				allSelResHaveSameChangeType = (changeType != firstChangeType) ? false: allSelResHaveSameChangeType;
				switch (changeType) {
					case CONFLICT:
						selectionContainsConflicts = true;
						break;
					case MISSING:
					  // jump to the next case
					case REMOVED:
						selectionContainsDeletions = true;
						break;
				}
			}
		}
		
		// Enable/disable the actions
		showDiffAction.setEnabled(selectedLeaves.size() == 1);
		openAction.setEnabled(!selectionContainsDeletions && !allSelectedResources.isEmpty());
		stageUnstageAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
		resolveConflict.setEnabled(RepoUtil.isRepoMergingOrRebasing(repoState));
		resolveUsingMineAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		resolveUsingTheirsAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		markResolvedAction.setEnabled(selectionContainsConflicts && allSelResHaveSameChangeType && !allSelectedResources.isEmpty());
		restartMergeAction.setEnabled(RepoUtil.isRepoMergingOrRebasing(repoState));
		discardAction.setEnabled(!selectionContainsConflicts && !allSelectedResources.isEmpty());
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
	 * @param allSelectedResources a list of FileStatus.
	 * @param workingCopy          the working copy.
	 *
	 * @return <code>true</code> if the conflicted files contains at least a conflict marker.
	 *
	 */
  protected static boolean containsConflictMarkers(final List<FileStatus> allSelectedResources,
      final File workingCopy) {
    return allSelectedResources.stream()
        .parallel()
        .anyMatch(file -> file.getChangeType() == GitChangeType.CONFLICT 
            && GitResourceContextualMenu.hasConflictMarker(file, workingCopy));
  }

	/**
	 * @param fileStatus  the file status.
	 * @param workingCopy the working copy.
	 *
	 * @return <code>True</code> if the file contains at least a conflict marker.
	 */
  private static boolean hasConflictMarker(final FileStatus fileStatus, final File workingCopy)  {
    boolean toReturn;
		File currentFile = new File(workingCopy, fileStatus.getFileLocation());
		try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
			String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
			toReturn = (content.contains("<<<<<<<") || content.contains("=======") || content.contains(">>>>>>>"));
		} catch (IOException exception) {
			LOGGER.error(exception, exception);
			toReturn = false;
		}
		return toReturn;
  }
  
}
