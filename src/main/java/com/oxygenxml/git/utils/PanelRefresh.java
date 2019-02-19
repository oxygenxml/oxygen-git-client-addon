package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.ChangesPanel;
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
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(PanelRefresh.class);

  /**
   * Repository status: available or not.
   */
  public enum RepositoryStatus {
    /**
     * Available.
     */
    AVAILABLE,
    /**
     * Unavailable.
     */
    UNAVAILABLE;
  }

	/**
	 * Staging panel to update.
	 */
	private StagingPanel stagingPanel;
	/**
	 * Git access.
	 */
	private GitAccess gitAccess = GitAccess.getInstance();
	/**
	 * The last analyzed project.
	 */
	private String lastSelectedProject = "";
	/**
	 * Translation support.
	 */
	private Translator translator = Translator.getInstance();
	/**
	 * Refresh executor.
	 */
	private ScheduledExecutorService refreshExecutor = new ScheduledThreadPoolExecutor(1);
	/**
	 * Refresh future (representing pending completion of the task).
	 */
	private ScheduledFuture<?> refreshFuture;
	/**
	 * Refresh task.
	 */
	private Runnable refreshRunnable = () -> {
	  logger.debug("Start update on thread.");

	  GitStatus status = GitAccess.getInstance().getStatus();
	  updateFiles(
	      stagingPanel.getUnstagedChangesPanel(), 
	      status.getUnstagedFiles());
	  updateFiles(
	      stagingPanel.getStagedChangesPanel(), 
	      status.getStagedFiles());
	  updateCounters();

	  logger.debug("End update on thread.");
	};
  
  @Override
  public void call() {
	  // Check if the current Oxygen project is a Git repository.
    boolean isAfterRestart = lastSelectedProject.isEmpty(); 
	  boolean repoChanged = loadRepositoryFromOxygenProject();
	  
	  // No point in refreshing if we've just changed the repository.
	  if (!repoChanged || isAfterRestart) {
	    // Check the current repository.
	    try {
	      if (gitAccess.getRepository() != null) {
	        if (refreshFuture != null && !refreshFuture.isDone()) {
	          logger.debug("cancel task");
	          refreshFuture.cancel(true);
	        }
	        
	        if (refreshExecutor.isShutdown()) {
	          // A shutdown operation was canceled.
	          refreshExecutor = new ScheduledThreadPoolExecutor(1);
	        }
	        
	        refreshFuture = refreshExecutor.schedule(refreshRunnable, 500, TimeUnit.MILLISECONDS);
	      }
	    } catch (NoRepositorySelected e) {
	      if (logger.isDebugEnabled()) {
	        logger.debug(e, e);
	      }
	    }
	  }
	}

  /**
   * Checks the current loaded project and:
   * 
   * 1. load it if it contains a Git project.
   * 2. create a new Git repo if the project doesn't contains a Git project and the user agrees.
   * 
   * @return <code>true</code> if the repository changed.
   */
  private boolean loadRepositoryFromOxygenProject() {
    boolean repoChanged = false;
    if (stagingPanel.hasFocus()) {
	    // Do it only when the view has focus.
	    String projectView = PluginWorkspaceProvider.getPluginWorkspace().
	        getUtilAccess().expandEditorVariables("${pd}", null);
	    if (projectView != null 
	        && !projectView.equals(lastSelectedProject)
	        // Fast check to see if this is actually not a Git repository.
	        && !OptionsManager.getInstance().getProjectsTestedForGit().contains(projectView)) {
	      // Mark this project as tested.
	      lastSelectedProject = projectView;
	      try {
	        boolean repoLoaded = checkForGitRepositoriesUpAndDownFrom(projectView);

	        repoChanged = repoLoaded;
	        if (!repoLoaded) {
	          String[] options = new String[] { "   Yes   ", "   No   " };
	          int[] optonsId = new int[] { 0, 1 };
	          String projectName = PluginWorkspaceProvider.getPluginWorkspace().
	              getUtilAccess().expandEditorVariables("${pn}", null) + ".xpr";
	          int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
	              translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
	              MessageFormat.format(translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT), projectName),
	              options,
	              optonsId);
	          if (response == 0) {
	            repoChanged = true;
	            gitAccess.createNewRepository(projectView);
	          }

	          // Don't ask the user again.
	          OptionsManager.getInstance().saveProjectTestedForGit(projectView);
	        }

	      } catch (IOException e) {
	        if (logger.isDebugEnabled()) {
	          logger.debug(e, e);
	        }
	      }
	    }
	  }
    
    return repoChanged;
  }

	/**
	 * Checks the project directory for Git repositories.
	 * 
	 * @param projectDir Project directory.
	 * 
	 * @return <code>true</code> if a Git repository was detected and loaded. <code>false</code>
	 * if no Git repository was detected.
	 * 
	 * @throws FileNotFoundException The project file doesn't exist.
	 * @throws IOException A Git repository was detected but not loaded.
	 */
	private boolean checkForGitRepositoriesUpAndDownFrom(String projectDir) throws IOException {
	  boolean projectPahtIsGit = false;
		String projectName = EditorVariables.expandEditorVariables("${pn}", null);
		String projectXprName = projectName + ".xpr";
		try {
		  // Parse the XML file to detected the referred resources.
		  SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		  SAXParser saxParser = saxParserFactory.newSAXParser();
			XPRHandler handler = new XPRHandler();
			
			File xmlFile = new File(projectDir, projectXprName);
			saxParser.parse(xmlFile, handler);
			List<String> pathsFromProjectView = handler.getPaths();
			for (String path : pathsFromProjectView) {
				File file = null;
				if (FileHelper.isURL(path)) {
					file = new File(path);
				} else if (!".".equals(path)) {
					file = new File(projectDir, path);
				}
				if (file != null) {
					String pathToCheck = file.getAbsolutePath();
					if (checkGitFolder(pathToCheck)) {
					  projectPahtIsGit = true;
					  break;
					}
				}
			}

			if (!projectPahtIsGit) {
			  // The oxygen project might be inside a Git repository.
			  // Look into the ancestors for a Git repository.
			  File file = new File(projectDir);
			  while (file != null) {
			    if (checkGitFolder(file.getAbsolutePath())) {
			      projectPahtIsGit = true;
			      break;
			    }
			    file = file.getParentFile();
			  }
			}
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			if (logger.isDebugEnabled()) {
				logger.debug(e1, e1);
			}
		}
		
		return projectPahtIsGit;
	}

	/**
	 * Checks if the given folder is a Git repository. If it is, then it loads
	 * it into the view.
	 * 
	 * @param pathToCheck Folder path.
	 *  
	 * @return <code>true</code> if the path identifies a Git repository.
	 * 
	 * @throws IOException Problems changing the repository. 
	 */
	private boolean checkGitFolder(String pathToCheck) throws IOException {
	  boolean projectPahtIsGit = false;
		if (FileHelper.isGitRepository(pathToCheck)) {
			projectPahtIsGit = true;
			
			GitAccess.getInstance().setRepository(pathToCheck);
		}
		
		return projectPahtIsGit;
	}
	
	/**
	 * Update the counters presented on the Pull/Push toolbar action.
	 */
	private void updateCounters() {
    // Connect to the remote.
    RepositoryStatus status = RepositoryStatus.AVAILABLE;
    try {
      GitAccess.getInstance().fetch();
    } catch (RepositoryUnavailableException e) {
      status = RepositoryStatus.UNAVAILABLE;
    } catch (Exception e) {
      // Ignore other causes why the fetch might fail.
    }

    final RepositoryStatus fStatus = status;
	  SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        stagingPanel.getCommitPanel().setStatus(fStatus);
        stagingPanel.getToolbarPanel().updateStatus();
      }
    });
	}

	/**
	 * Updates the files in the model. 
	 * 
	 * @param panelToUpdate The panel to update: staged or unstaged resources panel.
	 * @param newfiles The new files to be presented in the panel.
	 */
	private void updateFiles(ChangesPanel panelToUpdate, final List<FileStatus> newfiles) {
	  // The current files presented in the panel.
	  List<FileStatus> filesInModel = panelToUpdate.getFilesStatuses();
	  
	  if (logger.isDebugEnabled()) {
	    logger.debug("New files      " + newfiles);
	    logger.debug("Files in model " + filesInModel);
	  }
	  
	  // Quick change detection.
	  boolean changeDetected = newfiles.size() != filesInModel.size();
	  if (!changeDetected) {
	    // Same size. Sort and compare files.
	    Collections.sort(newfiles, (o1, o2) -> o1.getFileLocation().compareTo(o2.getFileLocation()));
	    List<FileStatus> sortedModel = new ArrayList<>(filesInModel.size());
	    Collections.sort(sortedModel, (o1, o2) -> o1.getFileLocation().compareTo(o2.getFileLocation()));
	    
	    changeDetected = !newfiles.equals(sortedModel);
	  }

	  if (changeDetected) {
	    SwingUtilities.invokeLater(() -> {
	      String rootFolder = StagingPanel.NO_REPOSITORY;
	      try {
	        rootFolder = GitAccess.getInstance().getWorkingCopy().getName();
	      } catch (NoRepositorySelected e) {
	        // Never happens.
	        logger.error(e, e);
	      }
	      panelToUpdate.update(rootFolder, newfiles);
	    });
	  }
	}

	/**
	 * Links the refresh support with the staging panel.
	 * 
	 * @param stagingPanel Staging panel.
	 */
  public void setPanel(StagingPanel stagingPanel) {
		this.stagingPanel = stagingPanel;
	}

  /**
   * Attempts to shutdown any running refresh tasks.
   */
  public void shutdown() {
    if (refreshFuture != null) {
      // Just in case the task isn't running yet.
      refreshFuture.cancel(false);
    }
    refreshExecutor.shutdown();
    try {
      refreshExecutor.awaitTermination(2000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      logger.warn(e);
      // Restore interrupted state...
      Thread.currentThread().interrupt();

    }    
  }

}
