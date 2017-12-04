package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.FileState;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.util.editorvars.EditorVariables;

/**
 * TODO This should be used more rarely, as we implement and use the GitEventListener
 *      It makes sense for this object to just check things that are not covered by the notifications events,
 *      like external changes to the working copy. 
 * 
 * 
 * 
 * @author alex_jitianu
 *
 */
public class PanelRefresh implements GitRefreshSupport {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(PanelRefresh.class);

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

	@Override
  public void call() {
		if (logger.isDebugEnabled()) {
			logger.debug("Refresh Started");
		}
		projectPahtIsGit = false;
		projectXprExists = true;
		execute();
		if (logger.isDebugEnabled()) {
			logger.debug("Refresh Ended");
		}
	}

	private void execute() {
		String projectView = EditorVariables.expandEditorVariables("${pd}", null);
		if (!projectView.equals(lastSelectedProjectView)) {
			checkForGitRepositoriesUpAndDownFrom(projectView);
			if (stagingPanel.isInFocus()) {
				lastSelectedProjectView = projectView;
			}
			addGitFolder(projectView);
			if (stagingPanel.isInFocus() && !projectPahtIsGit
					&& !OptionsManager.getInstance().getProjectsTestedForGit().contains(projectView) && projectXprExists) {
				String[] options = new String[] { "   Yes   ", "   No   " };
				int[] optonsId = new int[] { 0, 1 };
				int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
						translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
						translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT), options, optonsId);
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
				updateFiles(FileState.UNSTAGED);
				updateFiles(FileState.STAGED);
				updateCounters();
			}
		} catch (NoRepositorySelected e1) {
		  if (logger.isDebugEnabled()) {
		    logger.debug(e1, e1);
		  }
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
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		} catch (SAXException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		} catch (FileNotFoundException e1) {
		  if (logger.isDebugEnabled()) {
		    logger.debug(e1, e1);
		  }
		  // Project file doesn't exist
      projectXprExists = false;
		} catch (IOException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		}
		
		if (projectXprExists) {
		  File file = new File(projectView);
		  while (file.getParent() != null) {
		    String projectParent = file.getParent();
		    addGitFolder(projectParent);
		    file = file.getParentFile();
		  }
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
	
	/**
	 * Git counters provider.
	 */
	private interface GitStatusCountersProvider {
    /**
     * @return the number of pulls behind.
     */
    public int getPullsBehind();
    /**
     * @return the number of pushes ahead.
     */
    public int getPushesAhead();
	}

	private void updateCounters() {
		new SwingWorker<GitStatusCountersProvider, Void>() {
			@Override
			protected GitStatusCountersProvider doInBackground() throws Exception {
			  // Connect to the remote.
			  String status = "available";
			  try {
			    GitAccess.getInstance().fetch();
			  } catch (RepositoryUnavailableException e) {
			    status = "unavailable";
			  } catch (Exception e) {
			    // Ignore other causes why the fetch might fail.
			  }
			  stagingPanel.getCommitPanel().setStatus(status);

			  return new GitStatusCountersProvider() {
          @Override
          public int getPushesAhead() {
            return GitAccess.getInstance().getPushesAhead();
          }
          @Override
          public int getPullsBehind() {
            return GitAccess.getInstance().getPullsBehind();
          }
        };
			}

			@Override
			protected void done() {
			  super.done();
			  try {
			    GitStatusCountersProvider counterProvider = get();
			    // PULL
			    stagingPanel.getToolbarPanel().setPullsBehind(counterProvider.getPullsBehind());
			    stagingPanel.getToolbarPanel().updateInformationLabel();
			    // PUSH
			    stagingPanel.getToolbarPanel().setPushesAhead(counterProvider.getPushesAhead());

			  } catch (InterruptedException e) {
			    if (logger.isDebugEnabled()) {
			      logger.debug(e, e);
			    }
			    Thread.currentThread().interrupt();
			  } catch (ExecutionException e) {
			    if (logger.isDebugEnabled()) {
			      logger.debug(e, e);
			    }
			  }
			}
		}.execute();
	}

	// TODO: maybe remove the state and update all the files, staged and unstaged, at once
	private void updateFiles(final FileState state) {
		new SwingWorker<List<FileStatus>, Integer>() {

			@Override
			protected List<FileStatus> doInBackground() throws Exception {
				if (state == FileState.UNSTAGED) {
					return GitAccess.getInstance().getUnstagedFiles();
				} else {
					return GitAccess.getInstance().getStagedFile();
				}
			}

			@Override
			protected void done() {
				List<FileStatus> files = null;
				List<FileStatus> newFiles = new ArrayList<FileStatus>();
				StagingResourcesTableModel model = null;
				if (state == FileState.UNSTAGED) {
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
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
					Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
					if (logger.isDebugEnabled()) {
						logger.debug(e, e);
					}
				}
				if (!newFiles.equals(filesInModel)) {
					if (state == FileState.UNSTAGED) {
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

	@Override
  public void setPanel(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
	}
}
