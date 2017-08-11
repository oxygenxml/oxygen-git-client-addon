package com.oxygenxml.git.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.OptionsManager;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.util.editorvars.EditorVariables;

public class Refresh {

	private StagingPanel stagingPanel;
	private GitAccess gitAccess;

	public Refresh(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
		this.gitAccess = GitAccess.getInstance();
	}

	public void call() {
		String projectView = EditorVariables.expandEditorVariables("${pd}", null);
		if (FileHelper.isGitRepository(projectView)) {
			if (!OptionsManager.getInstance().getRepositoryEntries().contains(projectView)) {
				WorkingCopySelectionPanel workingCopySelectionPanel = stagingPanel.getWorkingCopySelectionPanel();
				OptionsManager.getInstance().addRepository(projectView);
				workingCopySelectionPanel.getWorkingCopySelector().addItem(projectView);
				if (workingCopySelectionPanel.getWorkingCopySelector().getItemCount() == 1) {
					workingCopySelectionPanel.getWorkingCopySelector().setSelectedIndex(0);
					try {
						gitAccess.setRepository(workingCopySelectionPanel.getWorkingCopySelector().getItemAt(0));
					} catch (RepositoryNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		try {
			if (gitAccess.getRepository() != null) {

				updateFiles(StageState.UNSTAGED);
				updateFiles(StageState.STAGED);
				updateCounter(Command.PULL);
				updateCounter(Command.PUSH);
				stagingPanel.getToolbarPanel().updateInformationLabel();
			}
		} catch (NoRepositorySelected e1) {
			return;
		}
	}

	private void updateCounter(final Command command) {
		new SwingWorker<Integer, Integer>() {
			protected Integer doInBackground() throws Exception {
				if (command == Command.PULL) {
					GitAccess.getInstance().fetch();
					return GitAccess.getInstance().getPullsBehind();
				} else {
					return GitAccess.getInstance().getPushesAhead();
				}
			}

			@Override
			protected void done() {
				super.done();
				try {
					int counter = get();
					if (command == Command.PULL) {
						stagingPanel.getToolbarPanel().setPullsBehind(counter);
					} else {
						stagingPanel.getToolbarPanel().setPushesAhead(counter);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}

	private void updateFiles(final StageState state) {
		new SwingWorker<List<FileStatus>, Integer>() {

			@Override
			protected List<FileStatus> doInBackground() throws Exception {
				if (state == StageState.UNSTAGED) {
					return GitAccess.getInstance().getUnstagedFiles();
				} else {
					return GitAccess.getInstance().getStagedFile();
				}
			}

			@Override
			protected void done() {
				List<FileStatus> files = new ArrayList<FileStatus>();
				List<FileStatus> newFiles = new ArrayList<FileStatus>();
				StagingResourcesTableModel model = null;
				if (state == StageState.UNSTAGED) {
					model = (StagingResourcesTableModel) stagingPanel.getUnstagedChangesPanel().getFilesTable().getModel();
				} else {
					model = (StagingResourcesTableModel) stagingPanel.getStagedChangesPanel().getFilesTable().getModel();
				}
				List<FileStatus> filesInModel = model.getUnstagedFiles();
				try {
					files = get();
					for (FileStatus fileStatus : filesInModel) {
						if (files.contains(fileStatus)) {
							newFiles.add(fileStatus);
							files.remove(fileStatus);
						}
					}
					newFiles.addAll(files);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				if (!newFiles.equals(filesInModel)) {
					if (state == StageState.UNSTAGED) {
						stagingPanel.getUnstagedChangesPanel().updateFlatView(newFiles);
						stagingPanel.getUnstagedChangesPanel().createTreeView(OptionsManager.getInstance().getSelectedRepository(),
								newFiles);
					} else {
						stagingPanel.getStagedChangesPanel().updateFlatView(newFiles);
						stagingPanel.getStagedChangesPanel().createTreeView(OptionsManager.getInstance().getSelectedRepository(),
								newFiles);
					}
				}
			}

		}.execute();
	}
}
