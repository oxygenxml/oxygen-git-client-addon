package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.protocol.GitFile;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Displays the diff depending on the what change type the file is.
 * 
 * @author Beniamin Savu
 *
 */
public class DiffPresenter {

	/**
	 * The file on which the diffPresenter works
	 */
	private FileStatus file;

	/**
	 * The frame of the oxygen's diff
	 */
	private Component diffFrame;

	/**
	 * Controller used for staging and unstaging
	 */
	private StageController stageController;

	/**
	 * The translator used for the messages that are displayed to the user
	 */
	private Translator translator;

	public DiffPresenter(FileStatus file, StageController stageController, Translator translator) {
		this.stageController = stageController;
		this.file = file;
		this.translator = translator;
	}

	/**
	 * Perform different actions depending on the file change type. If the file is
	 * a conflict file then a 3-way diff is presented. If the file is a modified
	 * one then a 2-way diff is presented. And if a file is added then the file is
	 * opened
	 */
	public void showDiff() {
		switch (file.getChangeType()) {
		case CONFLICT:
			conflictDiff();
			break;
		case MODIFY:
			diffView();
			break;
		case ADD:
			openFile();
			break;
		case SUBMODULE:
			submoduleDiff();
			break;
		default:
			break;
		}
	}

	private void submoduleDiff() {
		GitAccess.getInstance().submoduleCompare(file.getFileLocation(), true);
		try {
			URL currentSubmoduleCommit = GitRevisionURLHandler.buildURL(GitFile.CURRENT_SUBMODULE, file.getFileLocation());
			URL previouslySubmoduleCommit = GitRevisionURLHandler.buildURL(GitFile.PREVIOUSLY_SUBMODULE, file.getFileLocation());
			((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).openDiffFilesApplication(currentSubmoduleCommit,
					previouslySubmoduleCommit);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens the file in the Oxygen
	 */
	public void openFile() {
		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.open(FileHelper.getFileURL(file.getFileLocation()));
	}

	/**
	 * Presents a 2-way diff
	 */
	private void diffView() {
		URL fileURL = FileHelper.getFileURL(file.getFileLocation());
		URL lastCommitedFileURL = null;

		try {
			lastCommitedFileURL = GitRevisionURLHandler.buildURL(GitFile.LAST_COMMIT, file.getFileLocation());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).openDiffFilesApplication(fileURL,
				lastCommitedFileURL);

	}

	/**
	 * Presents a 3-way diff
	 */
	private void conflictDiff() {
		try {
			// builds the URL for the files
			URL local = GitRevisionURLHandler.buildURL(GitFile.LOCAL, file.getFileLocation());
			URL remote = GitRevisionURLHandler.buildURL(GitFile.REMOTE, file.getFileLocation());
			URL base = GitRevisionURLHandler.buildURL(GitFile.BASE, file.getFileLocation());

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());

			// time stamp used for detecting if the file was changed in the diff view
			final long diffStartedTimeStamp = localCopy.lastModified();

			try {
				// checks whether a base commit exists or not. If not, then the a 2-way
				// diff is presented
				if (GitAccess.getInstance().getLoaderFrom(GitAccess.getInstance().getBaseCommit(),
						file.getFileLocation()) == null) {
					diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.openDiffFilesApplication(local, remote);
				} else {
					diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
							.openDiffFilesApplication(local, remote, base);
				}
			} catch (MissingObjectException e1) {
				e1.printStackTrace();
			} catch (IncorrectObjectTypeException e1) {
				e1.printStackTrace();
			} catch (CorruptObjectException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// checks if the file in conflict has been resolved or not after the diff
			// view was closed
			diffFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					long diffClosedTimeStamp = localCopy.lastModified();

					if (diffClosedTimeStamp == diffStartedTimeStamp) {

						String[] options = new String[] { "   Yes   ", "   No   " };
						int[] optonsId = new int[] { 0, 1 };
						int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
								translator.getTraslation(Tags.CHECK_IF_CONFLICT_RESOLVED_TITLE),
								translator.getTraslation(Tags.CHECK_IF_CONFLICT_RESOLVED), options, optonsId);
						if (response == 0) {
							GitAccess.getInstance().remove(file);
							GitAccess.getInstance().restoreLastCommitFile(file.getFileLocation());
							GitAccess.getInstance().add(file);
							StageState oldState = StageState.UNSTAGED;
							StageState newState = StageState.DISCARD;
							List<FileStatus> files = new ArrayList<FileStatus>();
							files.add(file);
							ChangeEvent changeEvent = new ChangeEvent(newState, oldState, files);
							stageController.stateChanged(changeEvent);
						}
					} else {
						file.setChangeType(GitChangeType.MODIFY);
						StageState oldState = StageState.UNSTAGED;
						StageState newState = StageState.STAGED;
						List<FileStatus> files = new ArrayList<FileStatus>();
						files.add(file);
						ChangeEvent changeEvent = new ChangeEvent(newState, oldState, files);
						stageController.stateChanged(changeEvent);
					}
					diffFrame.removeComponentListener(this);
				}
			});

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}

	public void setFile(FileStatus fileStatus) {
		this.file = fileStatus;
	}
}
