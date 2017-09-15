package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.util.editorvars.EditorVariables;

public class PanelRefresh implements Refresh {

	private StagingPanel stagingPanel;
	private GitAccess gitAccess;
	private String lastSelectedProjectView = "";
	private Translator translator;
	private boolean projectPahtIsGit;
	private boolean projectXprExists;

	public PanelRefresh(Translator translator) {
		this.gitAccess = GitAccess.getInstance();
		this.translator = translator;
	}

	public void call() {
		projectPahtIsGit = false;
		projectXprExists = true;
		execute();
	}

	private void execute() {
		String projectView = EditorVariables.expandEditorVariables("${pd}", null);
		if (!projectView.equals(lastSelectedProjectView)) {
			checkForGitRepositoriesUpAndDownFrom(projectView);
			if (stagingPanel.isInFocus()) {
				lastSelectedProjectView = new String(projectView);
			}
			addGitFolder(projectView);
			if (stagingPanel.isInFocus() && !projectPahtIsGit
					&& !OptionsManager.getInstance().getProjectsTestedForGit().contains(projectView) && projectXprExists) {
				String[] options = new String[] { "   Yes   ", "   No   " };
				int[] optonsId = new int[] { 0, 1 };
				int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
						translator.getTraslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
						translator.getTraslation(Tags.CHECK_PROJECTXPR_IS_GIT), options, optonsId);
				if (response == 0) {
					gitAccess.createNewRepository(projectView);
					OptionsManager.getInstance().addRepository(projectView);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().addItem(projectView);
					OptionsManager.getInstance().saveSelectedRepository(projectView);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(projectView);
				}
				OptionsManager.getInstance().saveProjectTestedForGit(projectView);
			}

		}
		try {
			if (gitAccess.getRepository() != null) {
				updateFiles(StageState.UNSTAGED);
				updateFiles(StageState.STAGED);
				updateCounter(Command.PULL);
				updateCounter(Command.PUSH);
				String path = gitAccess.getRepository().getWorkTree().getAbsolutePath();
				String workingCopyCurrentPath = (String) stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().getSelectedItem();

				if (FileHelper.isGitSubmodule(path)) {
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setEditable(true);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(path);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setEditable(false);
					stagingPanel.requestFocus();
				} else if (FileHelper.isGitRepository(path) && !path.equals(workingCopyCurrentPath)){
					OptionsManager.getInstance().addRepository(path);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().addItem(path);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(path);
				}
			}
		} catch (NoRepositorySelected e1) {
			return;
		}
	}

	private void checkForGitRepositoriesUpAndDownFrom(String projectView) {
		String projectName = EditorVariables.expandEditorVariables("${pn}", null);
		String projectXprName = projectName + ".xpr";
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser;
		try {
			saxParser = saxParserFactory.newSAXParser();
			XPRHandler handler = new XPRHandler();
			File xmlFile = new File(projectView, projectXprName);
			saxParser.parse(xmlFile, handler);
			List<String> pathsFromProjectView = handler.getPaths();
			for (String path : pathsFromProjectView) {
				File file = null;
				if (FileHelper.isURL(path)) {
					file = new File(path);
				} else if (!".".equals(path)) {
					file = new File(projectView, path);
				}
				if (file != null) {
					String pathToCheck = file.getAbsolutePath();
					addGitFolder(pathToCheck);
				}
			}

		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			if (e1 instanceof FileNotFoundException) {
				projectXprExists = false;
				return;
			}
			e1.printStackTrace();
		}
		File file = new File(projectView);
		while (file.getParent() != null) {
			String projectParent = file.getParent();
			addGitFolder(projectParent);
			file = file.getParentFile();
		}
	}

	private void addGitFolder(String pathToCheck) {
		if (FileHelper.isGitRepository(pathToCheck)) {
			projectPahtIsGit = true;
			if (!OptionsManager.getInstance().getRepositoryEntries().contains(pathToCheck)) {
				OptionsManager.getInstance().addRepository(pathToCheck);
				stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().addItem(pathToCheck);
			}
			OptionsManager.getInstance().saveSelectedRepository(pathToCheck);
			stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().setSelectedItem(pathToCheck);
		}
	}

	private void updateCounter(final Command command) {
		new SwingWorker<Integer, Integer>() {
			protected Integer doInBackground() throws Exception {
				if (command == Command.PULL) {
					GitAccess.getInstance().fetch();
					if (GitAccess.getInstance().isUnavailable()) {
						stagingPanel.getCommitPanel().setStatus("unavailable");
					} else {
						stagingPanel.getCommitPanel().setStatus("availbale");
					}
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
						stagingPanel.getToolbarPanel().updateInformationLabel();
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

	public void setPanel(JComponent stagingPanel) {
		this.stagingPanel = (StagingPanel) stagingPanel;
	}
	
}
