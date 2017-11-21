package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Custom contextual menu that appears when you right click on the slected files
 * in the flat or tree view
 * 
 * @author Beniamin Savu
 *
 */
public class CustomContextualMenu extends JPopupMenu {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(CustomContextualMenu.class);

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

	public CustomContextualMenu(Translator translator, StageController stageController, GitAccess gitAccess) {
		this.translator = translator;
		this.stageController = stageController;
		this.gitAccess = gitAccess;

	}

	/**
	 * Adds the contextual menu items and creates for each of them an action. The
	 * action will apply for the given files. Some action may be different
	 * depending on the given staging state
	 * 
	 * @param files
	 * @param staging
	 */
	public void createContextualMenuFor(final List<FileStatus> files, final boolean staging) {
		final FileStatus fileStatus = files.get(0);

		// Show Diff menu
		JMenuItem showDiff = new JMenuItem();
		showDiff.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				DiffPresenter diff = new DiffPresenter(fileStatus, stageController, translator);
				diff.showDiff();
			}
		});
		showDiff.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE));

		// Open menu
		JMenuItem open = new JMenuItem();
		open.addActionListener(new ActionListener() {

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

		});
		open.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_OPEN));

		// Stage/Unstage
		JMenuItem changeState = new JMenuItem();
		changeState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				StageState oldState = StageState.UNSTAGED;
				StageState newState = StageState.STAGED;
				if (staging) {
					oldState = StageState.STAGED;
					newState = StageState.UNSTAGED;
				}
				List<FileStatus> resolveUsingMineFiles = new ArrayList<FileStatus>(files);
				ChangeEvent changeEvent = new ChangeEvent(newState, oldState, resolveUsingMineFiles);
				stageController.stateChanged(changeEvent);
			}
		});
		if (staging) {
			changeState.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_UNSTAGE));
		} else {
			changeState.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_STAGE));
		}

		// Resolve "mine"
		JMenuItem resolveMine = new JMenuItem();
		resolveMine.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				StageState oldState = StageState.UNSTAGED;
				StageState newState = StageState.DISCARD;
				List<FileStatus> resolveUsingMineFiles = new ArrayList<FileStatus>(files);
				ChangeEvent changeEvent = new ChangeEvent(newState, oldState, resolveUsingMineFiles);
				stageController.stateChanged(changeEvent);
			}
		});
		resolveMine.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_MINE));

		// Resolve "theirs"
		JMenuItem resolveTheirs = new JMenuItem();
		resolveTheirs.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				for (FileStatus file : files) {
					gitAccess.remove(file);
					gitAccess.updateWithRemoteFile(file.getFileLocation());
				}
				StageState oldState = StageState.UNSTAGED;
				StageState newState = StageState.STAGED;
				ChangeEvent changeEvent = new ChangeEvent(newState, oldState, files);
				stageController.stateChanged(changeEvent);
			}
		});
		resolveTheirs.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_RESOLVE_USING_THEIRS));

		// show diff
		JMenuItem diff = new JMenuItem();
		diff.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				DiffPresenter diff = new DiffPresenter(fileStatus, stageController, translator);
				diff.showDiff();
			}
		});
		diff.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_OPEN_IN_COMPARE));

		// Mark resolved
		JMenuItem markResolved = new JMenuItem();
		markResolved.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				StageState oldState = StageState.UNSTAGED;
				StageState newState = StageState.STAGED;
				ChangeEvent changeEvent = new ChangeEvent(newState, oldState, files);
				stageController.stateChanged(changeEvent);
			}
		});
		markResolved.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_MARK_RESOLVED));

		// Restart Merge
		JMenuItem restartMerge = new JMenuItem();
		restartMerge.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				gitAccess.restartMerge();
				ChangeEvent changeEvent = new ChangeEvent(StageState.UNDEFINED, StageState.UNDEFINED,
						new ArrayList<FileStatus>());
				stageController.stateChanged(changeEvent);
			}
		});
		restartMerge.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_RESTART_MERGE));

		// Resolve Conflict
		JMenu resolveConflict = new JMenu();
		resolveConflict.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_RESOLVE_CONFLICT));
		resolveConflict.add(diff);
		resolveConflict.addSeparator();
		resolveConflict.add(resolveMine);
		resolveConflict.add(resolveTheirs);
		resolveConflict.add(markResolved);
		resolveConflict.addSeparator();
		resolveConflict.add(restartMerge);

		// Discard Menu
		JMenuItem discard = new JMenuItem();
		discard.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String[] options = new String[] { "   Yes   ", "   No   " };
				int[] optonsId = new int[] { 0, 1 };
				int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
						translator.getTraslation(Tags.CONTEXTUAL_MENU_DISCARD),
						translator.getTraslation(Tags.CONTEXTUAL_MENU_DISCARD_CONFIRMATION_MESSAGE), options, optonsId);
				if (response == 0) {
					for (FileStatus file : files) {
						if (file.getChangeType() == GitChangeType.ADD
						    || file.getChangeType() == GitChangeType.UNTRACKED) {
							try {
								FileUtils.forceDelete(
										new File(OptionsManager.getInstance().getSelectedRepository() + "/" + file.getFileLocation()));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else if (file.getChangeType() == GitChangeType.SUBMODULE) {
							gitAccess.discardSubmodule();
						}
					}

					StageState oldState = StageState.UNDEFINED;
					StageState newState = StageState.DISCARD;
					ChangeEvent changeEvent = new ChangeEvent(newState, oldState, files);
					stageController.stateChanged(changeEvent);
				}
			}
		});
		discard.setText(translator.getTraslation(Tags.CONTEXTUAL_MENU_DISCARD));
		this.add(showDiff);
		this.add(open);
		this.add(changeState);
		this.add(resolveConflict);
		this.add(discard);

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
			showDiff.setEnabled(false);
			diff.setEnabled(false);
		} else {
			showDiff.setEnabled(true);
			diff.setEnabled(true);
		}
		// the active actions for all selected files that contain each type
		if (files.size() > 1 && containsConflicts && !sameChangeType) {
			
			showDiff.setEnabled(false);
			open.setEnabled(true);
			changeState.setEnabled(false);
			resolveConflict.setEnabled(true);
			diff.setEnabled(false);
			resolveMine.setEnabled(false);
			resolveTheirs.setEnabled(false);
			restartMerge.setEnabled(true);
			markResolved.setEnabled(false);
			discard.setEnabled(false);
			if(containsSubmodule || containsDelete){
				open.setEnabled(false);
			}
		} else {
			// the active actions for all the selected files that are added
			if (fileStatus.getChangeType() == GitChangeType.ADD && sameChangeType) {
				showDiff.setEnabled(false);
				open.setEnabled(true);
				changeState.setEnabled(true);
				resolveConflict.setEnabled(false);
				diff.setEnabled(false);
				resolveMine.setEnabled(false);
				resolveTheirs.setEnabled(false);
				restartMerge.setEnabled(true);
				markResolved.setEnabled(false);
				discard.setEnabled(true);
				// the active actions for all the selected files that are deleted
			} else if (fileStatus.getChangeType() == GitChangeType.MISSING && sameChangeType) {
				showDiff.setEnabled(false);
				open.setEnabled(false);
				changeState.setEnabled(true);
				resolveConflict.setEnabled(false);
				diff.setEnabled(false);
				resolveMine.setEnabled(false);
				resolveTheirs.setEnabled(false);
				restartMerge.setEnabled(true);
				markResolved.setEnabled(false);
				discard.setEnabled(true);
				// the active actions for all the selected files that are modified
			} else if (fileStatus.getChangeType() == GitChangeType.MODIFIED && sameChangeType) {
				open.setEnabled(true);
				changeState.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveMine.setEnabled(false);
				resolveTheirs.setEnabled(false);
				restartMerge.setEnabled(true);
				markResolved.setEnabled(false);
				discard.setEnabled(true);
				// the active actions for all the selected files that are submodules
			} else if (fileStatus.getChangeType() == GitChangeType.SUBMODULE && sameChangeType) {
				open.setEnabled(false);
				changeState.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveMine.setEnabled(false);
				resolveTheirs.setEnabled(false);
				restartMerge.setEnabled(true);
				markResolved.setEnabled(false);
				discard.setEnabled(true);
				// the active actions for all the selected files that are in conflict
			}else if (fileStatus.getChangeType() == GitChangeType.CONFLICT && sameChangeType) {
				open.setEnabled(true);
				changeState.setEnabled(false);
				resolveConflict.setEnabled(true);
				resolveMine.setEnabled(true);
				resolveTheirs.setEnabled(true);
				restartMerge.setEnabled(true);
				markResolved.setEnabled(true);
				discard.setEnabled(false);
				// the active actions for all the selected files that contain each type
				// without conflict
			} else {
				showDiff.setEnabled(false);
				open.setEnabled(true);
				resolveConflict.setEnabled(false);
				resolveMine.setEnabled(false);
				resolveTheirs.setEnabled(false);
				restartMerge.setEnabled(false);
				markResolved.setEnabled(false);
				discard.setEnabled(true);
				if(containsSubmodule || containsDelete){
					open.setEnabled(false);
				}
				
			}
		}
		try {
			if (gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING_RESOLVED
					|| gitAccess.getRepository().getRepositoryState() == RepositoryState.MERGING) {
				resolveConflict.setEnabled(true);
				restartMerge.setEnabled(true);
			}
		} catch (NoRepositorySelected e1) {
			resolveConflict.setEnabled(false);
		}
	}
}
