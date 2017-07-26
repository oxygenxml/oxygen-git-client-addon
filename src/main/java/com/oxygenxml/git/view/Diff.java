package com.oxygenxml.git.view;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.JFrame;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class Diff {

	private FileStatus file;

	public Diff(FileStatus file) {
		this.file = file;
	}

	public void fire() {
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
			URL local = new URL("git://Local/" + file.getFileLocation());
			URL remote = new URL("git://Remote/" + file.getFileLocation());
			URL base = new URL("git://Base/" + file.getFileLocation());

			JFrame diffFrame = (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
					.openDiffFilesApplication(local, remote, base);
			
			diffFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					File f = new File(OptionsManager.getInstance().getSelectedRepository() + "/" + file.getFileLocation());
					long lastModified = f.lastModified();
					Date date = new Date(lastModified);
					System.out.println(date);
				}
			});
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
}
