package com.oxygenxml.git.utils;

import java.io.File;
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
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.WorkingCopySelectionPanel;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.StageState;

import ro.sync.util.editorvars.EditorVariables;

public class StagingPanelRefresh implements Refresh {

	private StagingPanel stagingPanel;
	private GitAccess gitAccess;
	private String lastSelectedProjectView = "";

	public StagingPanelRefresh() {
		this.gitAccess = GitAccess.getInstance();
	}

	public void call(JComponent stagingPanel) {
		this.stagingPanel = (StagingPanel) stagingPanel;
		execute();
	}

	private void execute() {
		String projectView = EditorVariables.expandEditorVariables("${pd}", null);
		if (!projectView.equals(lastSelectedProjectView)) {
			checkForGitRepositories(projectView);
			lastSelectedProjectView = new String(projectView);
			if (FileHelper.isGitRepository(projectView)) {
				WorkingCopySelectionPanel workingCopySelectionPanel = stagingPanel.getWorkingCopySelectionPanel();
				if (!OptionsManager.getInstance().getRepositoryEntries().contains(projectView)) {
					OptionsManager.getInstance().addRepository(projectView);
					workingCopySelectionPanel.getWorkingCopySelector().addItem(projectView);
				}
				OptionsManager.getInstance().saveSelectedRepository(projectView);
				workingCopySelectionPanel.getWorkingCopySelector().setSelectedItem(projectView);
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

	private void checkForGitRepositories(String projectView) {
		String projectName = EditorVariables.expandEditorVariables("${pn}", null);
		projectName += ".xpr";
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser;
		try {
			saxParser = saxParserFactory.newSAXParser();
			XPRHandler handler = new XPRHandler();
			String xprPath = FileHelper.findXPR(projectView);
			if (!"".equals(xprPath)) {
				File xmlFile = new File(projectView, projectName);
				saxParser.parse(xmlFile, handler);
			}
			List<String> pathsFromProjectView = handler.getPaths();
			for (String path : pathsFromProjectView) {
				File file = null;
				if (FileHelper.isURL(path)) {
					file = new File(path);
				} else if (!".".equals(path)) {
					file = new File(projectView, path);
				} 
				if(file != null){
					String pathToCheck = file.getAbsolutePath();
					if (FileHelper.isGitRepository(pathToCheck)) {
						if (!OptionsManager.getInstance().getRepositoryEntries().contains(pathToCheck)) {
							OptionsManager.getInstance().addRepository(file.getAbsolutePath());
							stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector().addItem(pathToCheck);
						}
					}
				}
			}

		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
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
