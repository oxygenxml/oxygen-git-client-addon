package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.historycomponents.HistoryController;

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
	private GitController stageController;

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
   * @param stageController       Staging controller.
   * @param historyController     History interface.
   * @param isStage               <code>true</code> if we create the menu for the staged resources,
   *                                  <code>false</code> for the unstaged resources.
	 * @param repoState             Repository state.
   */
  public GitViewResourceContextualMenu(
      SelectedResourcesProvider selResProvider,
      GitController stageController,
      HistoryController historyController,
      boolean isStage,
      RepositoryState repoState) {
    this.stageController = stageController;
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
	  if (!selResProvider.getAllSelectedResources().isEmpty() || isRepoMergingOrRebasing()) {
	    final List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
	    final List<FileStatus> selectedLeaves = selResProvider.getOnlySelectedLeaves();

	    // "Open in compare editor" action
	    AbstractAction showDiffAction = new AbstractAction(
	        translator.getTranslation(Tags.OPEN_IN_COMPARE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        DiffPresenter.showDiff(selectedLeaves.get(0), stageController);
	      }
	    };

	    // "Open" action
	    AbstractAction openAction = new AbstractAction(
	        translator.getTranslation(Tags.OPEN)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        for (FileStatus file : allSelectedResources) {
	          try {
	            URL fileURL = null;
	            if (file.getChangeType() == GitChangeType.ADD
	                || file.getChangeType() == GitChangeType.CHANGED) {
	              // A file from the INDEX. We need a special URL to access it.
	              fileURL = GitRevisionURLHandler.encodeURL(
	                  VersionIdentifier.INDEX_OR_LAST_COMMIT,
	                  file.getFileLocation());
	            } else {
	              // We must open a local copy.
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
	        translator.getTranslation(Tags.RESOLVE_USING_MINE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.RESOLVE_USING_MINE);
	      }
	    };

	    // Resolve using "theirs"
	    AbstractAction resolveUsingTheirsAction = new AbstractAction(
	        translator.getTranslation(Tags.RESOLVE_USING_THEIRS)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.RESOLVE_USING_THEIRS);
	      }
	    };

	    // "Mark resolved" action
	    AbstractAction markResolvedAction = new AbstractAction(
	        translator.getTranslation(Tags.MARK_RESOLVED)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(allSelectedResources, GitCommand.STAGE);
	      }
	    };

	    // "Restart Merge" action
	    AbstractAction restartMergeAction = new AbstractAction(
	        translator.getTranslation(Tags.RESTART_MERGE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        int result = JOptionPane.showConfirmDialog(
	            (Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
	            translator.getTranslation(Tags.RESTART_MERGE_CONFIRMATION),
	            translator.getTranslation(Tags.RESTART_MERGE),
	            JOptionPane.YES_NO_OPTION,
	            JOptionPane.WARNING_MESSAGE);
	        if (result == JOptionPane.YES_OPTION) {
	          gitAccess.restartMerge();
	        }
	      }
	    };
	    
	    // "Discard" action 
      AbstractAction discardAction = new DiscardAction(allSelectedResources, stageController);

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
	    this.add(stageUnstageAction);
	    this.add(resolveConflict);
	    this.add(discardAction);

	    if (!forStagedRes) {
	      AbstractAction historyAction = new AbstractAction(translator.getTranslation(Tags.SHOW_IN_HISTORY)) {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	          if (!allSelectedResources.isEmpty()) {
	            historyController.showResourceHistory(allSelectedResources.get(0).getFileLocation());
	          }
	        }
	      };
	      this.add(historyAction);

	      AbstractAction blameAction = new AbstractAction(translator.getTranslation(Tags.SHOW_BLAME)) {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	          if (!allSelectedResources.isEmpty()) {
	            try {
	              BlameManager.getInstance().doBlame(
	                  allSelectedResources.get(0).getFileLocation(), 
	                  historyController);
	            } catch (IOException | GitAPIException e1) {
	              logger.error(e1, e1);
	            }
	          }
	        }
	      };
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
