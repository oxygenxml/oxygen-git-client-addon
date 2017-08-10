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

import com.oxygenxml.git.protocol.GitFile;
import com.oxygenxml.git.protocol.GitRevisionURLHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.StageController;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class DiffPresenter {

	private FileStatus file;
	private Component diffFrame;
	private StageController stageController;

	public DiffPresenter(FileStatus file, StageController stageController) {
		this.stageController = stageController;
		this.file = file;
	}

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
		default:
			break;
		}
	}

	public void openFile() {
		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.open(FileHelper.getFileURL(file.getFileLocation()));
	}

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

	private void conflictDiff() {
		try {
			URL local = GitRevisionURLHandler.buildURL(GitFile.LOCAL, file.getFileLocation());
			URL remote = GitRevisionURLHandler.buildURL(GitFile.REMOTE, file.getFileLocation());
			URL base = GitRevisionURLHandler.buildURL(GitFile.BASE, file.getFileLocation());

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());
			final long diffStartedTimeStamp = localCopy.lastModified();

			try {
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
			diffFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					long diffClosedTimeStamp = localCopy.lastModified();

					if (diffClosedTimeStamp == diffStartedTimeStamp) {

						String[] options = new String[] { "   Yes   ", "   No   " };
						int[] optonsId = new int[] { 0, 1 };
						int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.showConfirmDialog("Conflict Resolution", "You haven't modified the local version. Do you want to resolve the conflict by keeping your version?", options, optonsId);
						if (response == 0) {
							GitAccess.getInstance().remove(file);
							GitAccess.getInstance().restoreLastCommit(file.getFileLocation());
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
}
