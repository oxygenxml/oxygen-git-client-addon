package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
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

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.util.editorvars.EditorVariables;

/**
 * Synchronize the models with the Git repository state. 
 * 
 * @author alex_jitianu
 */
public class PanelRefresh implements GitRefreshSupport {

  /**
   * Repository status: available or not.
   */
  public static final class RepositoryStatus {
    /**
     * Hidden constructor.
     */
    private RepositoryStatus() {
      // Nada
    }
    /**
     * Available.
     */
    public static final String AVAILABLE = "available";
    /**
     * Unavailable.
     */
    public static final String UNAVAILABLE = "unavailable";
  }
  
	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(PanelRefresh.class);
	
	private Timer timer = new Timer(true);
	

	private StagingPanel stagingPanel;
	private GitAccess gitAccess = GitAccess.getInstance();
	private String lastSelectedProjectView = "";
	private Translator translator = Translator.getInstance();
	private boolean projectPahtIsGit;
	private boolean projectXprExists;

  private TimerTask task;

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
	  String projectView = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().expandEditorVariables("${pd}", null);
		if (projectView != null && !projectView.equals(lastSelectedProjectView)) {
			checkForGitRepositoriesUpAndDownFrom(projectView);
			if (stagingPanel.hasFocus()) {
				lastSelectedProjectView = projectView;
			}
			addGitFolder(projectView);
			if (stagingPanel.hasFocus() && !projectPahtIsGit
					&& !OptionsManager.getInstance().getProjectsTestedForGit().contains(projectView) && projectXprExists) {
				String[] options = new String[] { "   Yes   ", "   No   " };
				int[] optonsId = new int[] { 0, 1 };
				int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
						translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
						translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT), options, optonsId);
				if (response == 0) {
					gitAccess.createNewRepository(projectView);
					OptionsManager.getInstance().addRepository(projectView);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo().addItem(projectView);
					OptionsManager.getInstance().saveSelectedRepository(projectView);
					stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo().setSelectedItem(projectView);
				}
				OptionsManager.getInstance().saveProjectTestedForGit(projectView);
			}

		}
		try {
			if (gitAccess.getRepository() != null) {
			  
			  if (task != null) {
			    logger.info("cancel task");
		      task.cancel();
		    }
		    
		    task = new TimerTask() {
		      @Override
		      public void run() {
		        if (logger.isDebugEnabled()) {
		          logger.debug("Start update on thread.");
		        }
		        
		        updateFiles(true);
		        updateFiles(false);
		        updateCounters();
		        
		        if (logger.isDebugEnabled()) {
              logger.debug("End update on thread.");
            }
		      }
		    };
			  
		    timer.schedule(task, 500);
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
				stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo().addItem(pathToCheck);
			}
			OptionsManager.getInstance().saveSelectedRepository(pathToCheck);
			stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo().setSelectedItem(pathToCheck);
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

	/**
	 * Update the counters presented on the Pull/Push toolbar action.
	 */
	private void updateCounters() {
    // Connect to the remote.
    String status = RepositoryStatus.AVAILABLE;
    try {
      GitAccess.getInstance().fetch();
    } catch (RepositoryUnavailableException e) {
      status = RepositoryStatus.UNAVAILABLE;
    } catch (Exception e) {
      // Ignore other causes why the fetch might fail.
    }
    stagingPanel.getCommitPanel().setStatus(status);

    final GitStatusCountersProvider counterProvider = new GitStatusCountersProvider() {
      @Override
      public int getPushesAhead() {
        return GitAccess.getInstance().getPushesAhead();
      }
      @Override
      public int getPullsBehind() {
        return GitAccess.getInstance().getPullsBehind();
      }
    };
	  
	  SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // PULL
        stagingPanel.getToolbarPanel().setPullsBehind(counterProvider.getPullsBehind());
        stagingPanel.getToolbarPanel().updateInformationLabel();
        // PUSH
        stagingPanel.getToolbarPanel().setPushesAhead(counterProvider.getPushesAhead());
      
      }
    });
	}

	/**
	 * Updates the files in the model. 
	 * 
	 * @param unstaged <code>true</code> to update the local Working Copy files.
	 * <code>false</code> to update the INDEX.
	 */
	private void updateFiles(final boolean unstaged) {
	  List<FileStatus> nfiles = null;
	  if (unstaged) {
	    nfiles = GitAccess.getInstance().getUnstagedFiles();
	  } else {
	    nfiles = GitAccess.getInstance().getStagedFile();
	  }
	  
	  final List<FileStatus> files = nfiles;
	  SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        List<FileStatus> newFiles = new ArrayList<FileStatus>();
        List<FileStatus> filesInModel = null;
        if (unstaged) {
          filesInModel = stagingPanel.getUnstagedChangesPanel().getFilesStatuses();
        } else {
          filesInModel = stagingPanel.getStagedChangesPanel().getFilesStatuses();
        }

        for (FileStatus fileStatus : filesInModel) {
          if (files.contains(fileStatus)) {
            newFiles.add(fileStatus);
            files.remove(fileStatus);
          }
        }
        newFiles.addAll(files);

        if (logger.isDebugEnabled()) {
          logger.debug("New files      " + newFiles);
          logger.debug("Files in model " + filesInModel);
        }

        if (!newFiles.equals(filesInModel)) {
          String rootFolder = StagingPanel.NO_REPOSITORY;
          try {
            rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
          } catch (NoRepositorySelected e) {
            // Never happens.
            logger.error(e, e);
          }
          if (unstaged) {
            stagingPanel.getUnstagedChangesPanel().update(rootFolder, newFiles);
          } else {
            stagingPanel.getStagedChangesPanel().update(rootFolder, newFiles);
          }
        }
      }
    });
	}

	/**
	 * Links the refresh support with the staging panel.
	 * 
	 * @param stagingPanel Staging panel.
	 */
  public void setPanel(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
	}
}
