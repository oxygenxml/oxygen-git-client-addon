package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.FileState;
import com.oxygenxml.git.view.event.StageController;

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
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitViewResourceContextualMenu.class);

	/**
	 * The translator used for the contextual menu names
	 */
	private Translator translator;

	/**
	 * Controller used for staging and unstaging
	 */
	private StageController stageController;

	/**
	 * The git API, containg the commands
	 */
	private GitAccess gitAccess;

	/**
	 * Constructor.
	 * 
	 * @param translator       Translator for i18n.
	 * @param stageController  Staging controller.
	 * @param gitAccess        Access to the Git API.
	 */
	public GitViewResourceContextualMenu(
	    Translator translator,
	    StageController stageController,
	    GitAccess gitAccess) {
		this.translator = translator;
		this.stageController = stageController;
		this.gitAccess = gitAccess;
	}

	/**
	 * Creates the contextual menu for the selected files.
	 * 
	 * @param files      The selected files.
	 * @param staging    <code>true</code> if the files are staged.
	 */
	public void createContextualMenu(final List<FileStatus> files, final boolean staging) {
		final FileStatus fileStatus = files.get(0);

		// "Open in compare editor" action
		AbstractAction showDiffAction = new AbstractAction(
		    translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        new DiffPresenter(fileStatus, stageController, translator).showDiff();
      }
    };

		// "Open" action
    AbstractAction openAction = new AbstractAction(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_OPEN)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        DiffPresenter diff = new DiffPresenter(fileStatus, stageController, translator);
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
    AbstractAction stageUnstageAction = new AbstractAction(
        staging ? translator.getTranslation(Tags.CONTEXTUAL_MENU_UNSTAGE)
            : translator.getTranslation(Tags.CONTEXTUAL_MENU_STAGE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileState oldState = FileState.UNSTAGED;
        FileState newState = FileState.STAGED;
        if (staging) {
          oldState = FileState.STAGED;
          newState = FileState.UNSTAGED;
        }
        stageController.stateChanged(new ChangeEvent(newState, oldState, files));
      }
    };

		// Resolve using "mine"
    AbstractAction resolveUsingMineAction = new AbstractAction(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_MINE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        stageController.stateChanged(
            new ChangeEvent(FileState.DISCARD, FileState.UNSTAGED, files));
      }
    };

		// Resolve using "theirs"
    AbstractAction resolveUsingTheirsAction = new AbstractAction(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_THEIRS)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (FileStatus file : files) {
          gitAccess.remove(file);
          gitAccess.updateWithRemoteFile(file.getFileLocation());
        }
        stageController.stateChanged(
            new ChangeEvent(FileState.STAGED, FileState.UNSTAGED, files));
      }
    };

		// "Mark resolved" action
    AbstractAction markResolvedAction = new AbstractAction(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_MARK_RESOLVED)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        stageController.stateChanged(
            new ChangeEvent(FileState.STAGED, FileState.UNSTAGED, files));
      }
    };

		// "Restart Merge" action
    AbstractAction restartMergeAction = new AbstractAction(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_RESTART_MERGE)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitAccess.restartMerge();
        stageController.stateChanged(
            new ChangeEvent(FileState.UNDEFINED, FileState.UNDEFINED,
                Collections.<FileStatus> emptyList()));
      }
    };

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

		// "Discard" action 
		AbstractAction discardAction = new AbstractAction(
		    translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        String[] options = new String[] { "   Yes   ", "   No   " };
        int[] optonsId = new int[] { 0, 1 };
        int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
            translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD),
            translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD_CONFIRMATION_MESSAGE), options, optonsId);
        if (response == 0) {
          for (FileStatus file : files) {
            if (file.getChangeType() == GitChangeType.ADD
                || file.getChangeType() == GitChangeType.UNTRACKED) {
              try {
                FileUtils.forceDelete(
                    new File(OptionsManager.getInstance().getSelectedRepository() + '/' + file.getFileLocation()));
              } catch (IOException e1) {
                logger.error(e1, e1);
              }
            } else if (file.getChangeType() == GitChangeType.SUBMODULE) {
              gitAccess.discardSubmodule();
            }
          }

          stageController.stateChanged(
              new ChangeEvent(FileState.DISCARD, FileState.UNDEFINED, files));
        }
      }
    };
    
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
		// the active actions for all selected files that contain each type
		if (files.size() > 1 && containsConflicts && !sameChangeType) {
			
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
			// the active actions for all the selected files that are added
			if (fileStatus.getChangeType() == GitChangeType.ADD && sameChangeType) {
			  showDiffAction.setEnabled(false);
			  openAction.setEnabled(true);
			  stageUnstageAction.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveUsingMineAction.setEnabled(false);
				resolveUsingTheirsAction.setEnabled(false);
				restartMergeAction.setEnabled(true);
				markResolvedAction.setEnabled(false);
				discardAction.setEnabled(true);
				// the active actions for all the selected files that are deleted
			} else if (fileStatus.getChangeType() == GitChangeType.MISSING && sameChangeType) {
			  showDiffAction.setEnabled(false);
			  openAction.setEnabled(false);
			  stageUnstageAction.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveUsingMineAction.setEnabled(false);
				resolveUsingTheirsAction.setEnabled(false);
				restartMergeAction.setEnabled(true);
				markResolvedAction.setEnabled(false);
				discardAction.setEnabled(true);
				// the active actions for all the selected files that are modified
			} else if (fileStatus.getChangeType() == GitChangeType.MODIFIED && sameChangeType) {
			  openAction.setEnabled(true);
			  stageUnstageAction.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveUsingMineAction.setEnabled(false);
				resolveUsingTheirsAction.setEnabled(false);
				restartMergeAction.setEnabled(true);
				markResolvedAction.setEnabled(false);
				discardAction.setEnabled(true);
				// the active actions for all the selected files that are submodules
			} else if (fileStatus.getChangeType() == GitChangeType.SUBMODULE && sameChangeType) {
			  openAction.setEnabled(false);
			  stageUnstageAction.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveUsingMineAction.setEnabled(false);
				resolveUsingTheirsAction.setEnabled(false);
				restartMergeAction.setEnabled(true);
				markResolvedAction.setEnabled(false);
				discardAction.setEnabled(true);
				// the active actions for all the selected files that are in conflict
			} else if (fileStatus.getChangeType() == GitChangeType.CONFLICT && sameChangeType) {
			  openAction.setEnabled(true);
			  stageUnstageAction.setEnabled(false);
				resolveConflict.setEnabled(true);
				resolveUsingMineAction.setEnabled(true);
				resolveUsingTheirsAction.setEnabled(true);
				restartMergeAction.setEnabled(true);
				markResolvedAction.setEnabled(true);
				discardAction.setEnabled(false);
				// the active actions for all the selected files that contain each type
				// without conflict
			} else {
			  showDiffAction.setEnabled(false);
			  openAction.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveUsingMineAction.setEnabled(false);
				resolveUsingTheirsAction.setEnabled(false);
				restartMergeAction.setEnabled(false);
				markResolvedAction.setEnabled(false);
				discardAction.setEnabled(true);
				if(containsSubmodule || containsDelete){
				  openAction.setEnabled(false);
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
