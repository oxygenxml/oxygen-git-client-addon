package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

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
	 * @param files            The selected files.
	 * @param stageController  Staging controller.
	 * @param isStage          <code>true</code> if we create the menu for the staged resources.
	 */
	public GitViewResourceContextualMenu(
	    List<FileStatus> files,
	    StageController stageController,
	    boolean isStage) {
		this.stageController = stageController;
		populateMenu(files, isStage);
	}

	/**
	 * Populates the contextual menu for the selected files.
	 * 
	 * @param files      The selected files.
	 * @param isStage    <code>true</code> if the contextual menu is created
	 *                       for the staged files.
	 */
	private void populateMenu(final List<FileStatus> files, final boolean isStage) {
	  if (!files.isEmpty()) {
	    final FileStatus fileStatus = files.get(0);

	    // "Open in compare editor" action
	    AbstractAction showDiffAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        new DiffPresenter(fileStatus, stageController).showDiff();
	      }
	    };

	    // "Open" action
	    AbstractAction openAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        DiffPresenter diff = new DiffPresenter(fileStatus, stageController);
	        for (FileStatus file : files) {
	          diff.setFile(file);
	          try {
	            diff.openFile();
	          } catch (Exception e1) {
	            logger.error(e1, e1);
	          }
	        }
	      }
	    };

	    // "Stage"/"Unstage" actions
	    AbstractAction stageUnstageAction = new StageUnstageResourceAction(
	        files, 
	        isStage, 
	        stageController);

	    // Resolve using "mine"
	    AbstractAction resolveUsingMineAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_MINE)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(files, GitCommand.RESOLVE_USING_MINE);
	      }
	    };

	    // Resolve using "theirs"
	    AbstractAction resolveUsingTheirsAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_THEIRS)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(files, GitCommand.RESOLVE_USING_THEIRS);
	      }
	    };

	    // "Mark resolved" action
	    AbstractAction markResolvedAction = new AbstractAction(
	        translator.getTranslation(Tags.CONTEXTUAL_MENU_MARK_RESOLVED)) {
	      @Override
	      public void actionPerformed(ActionEvent e) {
	        stageController.doGitCommand(files, GitCommand.STAGE);
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
      AbstractAction discardAction = new DiscardAction(files, stageController);

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

	    this.add(showDiffAction);
	    this.add(openAction);
	    this.add(stageUnstageAction);
	    this.add(resolveConflict);
	    this.add(discardAction);

	    boolean sameChangeType = true;
	    boolean containsConflicts = false;
	    boolean containsSubmodule = false;
	    boolean containsDelete = false;
	    if (files.size() > 1) {
	      GitChangeType gitChangeType = files.get(0).getChangeType();
	      for (FileStatus file : files) {
	        if (gitChangeType != file.getChangeType()) {
	          sameChangeType = false;
	        }
	        if (GitChangeType.CONFLICT == file.getChangeType()) {
	          containsConflicts = true;
	        }
	        if (GitChangeType.SUBMODULE == file.getChangeType()) {
	          containsSubmodule = true;
	        }
	        if (GitChangeType.MISSING == file.getChangeType()
	            || GitChangeType.REMOVED == file.getChangeType()) {
	          containsDelete = true;
	        }
	      }
	      showDiffAction.setEnabled(false);
	    } else {
	      showDiffAction.setEnabled(true);
	    }
	    if (files.size() > 1 && containsConflicts && !sameChangeType) {
	      // the active actions for all selected files that contain each type
	      showDiffAction.setEnabled(false);
	      openAction.setEnabled(true);
	      stageUnstageAction.setEnabled(false);
	      resolveConflict.setEnabled(true);
	      resolveUsingMineAction.setEnabled(false);
	      resolveUsingTheirsAction.setEnabled(false);
	      restartMergeAction.setEnabled(true);
	      markResolvedAction.setEnabled(false);
	      discardAction.setEnabled(false);
	      if(containsSubmodule || containsDelete){
	        openAction.setEnabled(false);
	      }
	    } else {
	      if (fileStatus.getChangeType() == GitChangeType.ADD && sameChangeType) {
	        // the active actions for all the selected files that are added
	        showDiffAction.setEnabled(false);
	        openAction.setEnabled(true);
	        stageUnstageAction.setEnabled(true);
	        resolveConflict.setEnabled(false);
	        resolveUsingMineAction.setEnabled(false);
	        resolveUsingTheirsAction.setEnabled(false);
	        restartMergeAction.setEnabled(true);
	        markResolvedAction.setEnabled(false);
	        discardAction.setEnabled(true);
	      } else if (fileStatus.getChangeType() == GitChangeType.MISSING && sameChangeType) {
	        // the active actions for all the selected files that are deleted
	        showDiffAction.setEnabled(false);
	        openAction.setEnabled(false);
	        stageUnstageAction.setEnabled(true);
	        resolveConflict.setEnabled(false);
	        resolveUsingMineAction.setEnabled(false);
	        resolveUsingTheirsAction.setEnabled(false);
	        restartMergeAction.setEnabled(true);
	        markResolvedAction.setEnabled(false);
	        discardAction.setEnabled(true);
	      } else if (fileStatus.getChangeType() == GitChangeType.MODIFIED && sameChangeType) {
	        // the active actions for all the selected files that are modified
	        openAction.setEnabled(true);
	        stageUnstageAction.setEnabled(true);
	        resolveConflict.setEnabled(false);
	        resolveUsingMineAction.setEnabled(false);
	        resolveUsingTheirsAction.setEnabled(false);
	        restartMergeAction.setEnabled(true);
	        markResolvedAction.setEnabled(false);
	        discardAction.setEnabled(true);
	      } else if (fileStatus.getChangeType() == GitChangeType.SUBMODULE && sameChangeType) {
	        // the active actions for all the selected files that are submodules
	        openAction.setEnabled(false);
	        stageUnstageAction.setEnabled(true);
	        resolveConflict.setEnabled(false);
	        resolveUsingMineAction.setEnabled(false);
	        resolveUsingTheirsAction.setEnabled(false);
	        restartMergeAction.setEnabled(true);
	        markResolvedAction.setEnabled(false);
	        discardAction.setEnabled(true);
	      } else if (fileStatus.getChangeType() == GitChangeType.CONFLICT && sameChangeType) {
	        // the active actions for all the selected files that are in conflict
	        openAction.setEnabled(true);
	        stageUnstageAction.setEnabled(false);
	        resolveConflict.setEnabled(true);
	        resolveUsingMineAction.setEnabled(true);
	        resolveUsingTheirsAction.setEnabled(true);
	        restartMergeAction.setEnabled(true);
	        markResolvedAction.setEnabled(true);
	        discardAction.setEnabled(false);
	      } else {
	        // the active actions for all the selected files that contain each type
	        // without conflict
	        showDiffAction.setEnabled(false);
	        resolveConflict.setEnabled(false);
	        resolveUsingMineAction.setEnabled(false);
	        resolveUsingTheirsAction.setEnabled(false);
	        restartMergeAction.setEnabled(false);
	        markResolvedAction.setEnabled(false);
	        discardAction.setEnabled(true);
	        if(containsSubmodule || containsDelete) {
	          openAction.setEnabled(false);
	        } else {
	          openAction.setEnabled(true);
	        }

	      }
	    }
	    try {
	      if (gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING_RESOLVED
	          || gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING) {
	        resolveConflict.setEnabled(true);
	        restartMergeAction.setEnabled(true);
	      }
	    } catch (NoRepositorySelected e1) {
	      resolveConflict.setEnabled(false);
	    }
	  }
	}
}
