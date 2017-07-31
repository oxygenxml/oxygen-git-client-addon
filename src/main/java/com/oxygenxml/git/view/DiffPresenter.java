package com.oxygenxml.git.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFrame;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class DiffPresenter {

	private FileStatus file;

	public DiffPresenter(FileStatus file) {
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

	private void openFile() {
		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
				.open(GitAccess.getInstance().getFileContent(file.getFileLocation()));
	}

	private void diffView() {
		URL fileURL = GitAccess.getInstance().getFileContent(file.getFileLocation());
		URL lastCommitedFileURL = null;

		try {
			lastCommitedFileURL = new URL("git://LastCommit/" + file.getFileLocation());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).openDiffFilesApplication(fileURL,
				lastCommitedFileURL);

	}

	private void conflictDiff() {
		try {
		  // TODO Add some methods to build/break such URLs. Maybe in GitRevisionURLHandler
			URL local = new URL("git://Local/" + file.getFileLocation());
			URL remote = new URL("git://Remote/" + file.getFileLocation());
			URL base = new URL("git://Base/" + file.getFileLocation());

			String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
			final File localCopy = new File(selectedRepository, file.getFileLocation());
			final long diffStartedTimeStamp = localCopy.lastModified();

			final JFrame diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
					.openDiffFilesApplication(local, remote, base);

			diffFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					long diffClosedTimeStamp = localCopy.lastModified();

					if (diffClosedTimeStamp == diffStartedTimeStamp) {

						String[] options = new String[] { "   Yes   ", "   No   " };
						int[] optonsId = new int[] { 0, 1 };
						int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
								.showConfirmDialog("Conflict Warning", "Conflict Resolved?", options, optonsId);
						if (response == 0) {
							GitAccess.getInstance().restoreLastCommit(file.getFileLocation());
							GitAccess.getInstance().add(file);
						}
					} else {
						GitAccess.getInstance().add(file);
					}
					diffFrame.removeComponentListener(this);
				}
			});
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
}
