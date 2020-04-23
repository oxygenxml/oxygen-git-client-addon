package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.swing.SwingUtilities;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.xml.sax.SAXException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.sax.XPRHandler;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RepositoryUnavailableException;
import com.oxygenxml.git.service.SSHPassphraseRequiredException;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.ChangesPanel;
import com.oxygenxml.git.view.StagingPanel;
import com.oxygenxml.git.view.dialog.PassphraseDialog;

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
	private final GitAccess gitAccess = GitAccess.getInstance();
	/**
	 * The last analyzed project.
	 */
	private String lastSelectedProject = "";
	/**
	 * Translation support.
	 */
	private final Translator translator = Translator.getInstance();
	/**
	 * Refresh executor.
	 */
	private final GitOperationScheduler refreshExecutor = GitOperationScheduler.getInstance();
	/**
	 * Refresh future (representing pending completion of the task).
	 */
	private ScheduledFuture<?> refreshFuture;
	/**
	 * Refresh task.
	 */
	private Runnable refreshRunnable = () -> {
	  logger.debug("Start refresh on thread.");

	  boolean isAfterRestart = lastSelectedProject.isEmpty(); 
	  // No point in refreshing if we've just changed the repository.
	  boolean repoChanged = loadRepositoryFromOxygenProject();
	  if (!repoChanged || isAfterRestart) {
	    try {
	      if (gitAccess.getRepository() != null) {
	        stagingPanel.updateRebasePanelVisibilityBasedOnRepoState();
	        GitStatus status = GitAccess.getInstance().getStatus();
	        updateFiles(
	            stagingPanel.getUnstagedChangesPanel(), 
	            status.getUnstagedFiles());
	        updateFiles(
	            stagingPanel.getStagedChangesPanel(), 
	            status.getStagedFiles());
	        updateCounters();
	      }
	    } catch (NoRepositorySelected e) {
	      if (logger.isDebugEnabled()) {
	        logger.debug(e, e);
	      }
	    }
	  }

	  logger.debug("End refresh on thread.");
	};
  
  @Override
  public void call() {
    if (refreshFuture != null && !refreshFuture.isDone()) {
      logger.debug("cancel refresh task");
      refreshFuture.cancel(true);
    }

    refreshFuture = refreshExecutor.schedule(refreshRunnable);
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
    if (stagingPanel != null && stagingPanel.hasFocus()) {
      StandalonePluginWorkspace pluginWS =
          (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      String projectDir = pluginWS.getUtilAccess().expandEditorVariables("${pd}", null);
      if (projectDir != null 
          && !projectDir.equals(lastSelectedProject)
          // Fast check to see if this is actually not a Git repository.
          && !OptionsManager.getInstance().getProjectsTestedForGit().contains(projectDir)) {
        lastSelectedProject = projectDir;
        File detectedRepo = checkForGitRepositoriesUpAndDownFrom(projectDir);
        String projectName = pluginWS.getUtilAccess().expandEditorVariables("${pn}", null) + ".xpr";
        if (detectedRepo == null) {
          repoChanged = createNewRepoIfUserAgrees(projectDir, projectName);
        } else {
          // A Git repository was detected.
          try {
            File currentRepo = null;
            if (gitAccess.isRepoInitialized()) {
              currentRepo = gitAccess.getRepository().getDirectory().getParentFile();
            }
            if (currentRepo == null || !same(currentRepo, detectedRepo)) {
              repoChanged = switchToProjectRepoIfUserAgrees(getCanonicalPath(detectedRepo));
            }
          } catch (NoRepositorySelected e) {
            logger.warn(e, e);
          }
        }
      }
    }
    return repoChanged;
  }

  /**
   * @param file A file. 
   * 
   * @return The canonical version of the file.
   */
  private String getCanonicalPath(File file) {
    String repoPath;
    try {
      repoPath = file.getCanonicalPath();
    } catch (IOException e) {
      logger.debug(e, e);
      repoPath = file.getAbsolutePath();
    }
    return repoPath;
  }

  /**
   * Checks if the two files are equal.
   * 
   * @param first The first file.
   * @param second The second file.
   * 
   * @return <code>true</code> if the files have the same paths.
   */
  private boolean same(File first, File second) {
    boolean same = false;
    
    try {
      first = first.getCanonicalFile();
      second = second.getCanonicalFile();
          
      same = first.equals(second); 
    } catch (IOException e) {
      logger.error(e, e);
    }
    
    return same;
  }

  /**
   * Switch to the given repository if the user agrees.
   * 
   * @param projectDir  The project directory.
   * 
   * @return <code>true</code> if repository changed.
   */
  private boolean switchToProjectRepoIfUserAgrees(String projectDir) {
    boolean repoChanged = false;
    StandalonePluginWorkspace pluginWS =
        (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    int response = pluginWS.showConfirmDialog(
        translator.getTranslation(Tags.CHANGE_WORKING_COPY),
        MessageFormat.format(
            translator.getTranslation(Tags.CHANGE_TO_PROJECT_REPO_CONFIRM_MESSAGE),
            projectDir),
        new String[] {
            "   " + translator.getTranslation(Tags.YES) + "   ",
            "   " + translator.getTranslation(Tags.NO) + "   "
        },
        new int[] { 0, 1 });
    if (response == 0) {
      GitAccess.getInstance().setRepositoryAsync(projectDir);
      repoChanged = true;
    }
    
    return repoChanged;
  }

  /**
   * Create a new repository if the user agrees.
   * 
   * @param projectDir   Project directory.
   * @param projectName  Project name.
   * 
   * @return <code>true</code> if repository changed.
   */
  private boolean createNewRepoIfUserAgrees(String projectDir, String projectName) {
    boolean repoChanged = false;
    StandalonePluginWorkspace pluginWS =
        (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    int response = pluginWS.showConfirmDialog(
        translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
        MessageFormat.format(translator.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT), projectName),
        new String[] {
            "   " + translator.getTranslation(Tags.YES) + "   ",
            "   " + translator.getTranslation(Tags.NO) + "   "
        },
        new int[] { 0, 1 });
    if (response == 0) {
      try {
        gitAccess.createNewRepository(projectDir);
        repoChanged = true;
      } catch (IllegalStateException | GitAPIException e) {
        logger.debug(e,  e);
        pluginWS.showErrorMessage("Failed to create a new repository.", e);
      }
    }

    // Don't ask the user again.
    OptionsManager.getInstance().saveProjectTestedForGit(projectDir);
    
    return repoChanged;
  }

	/**
	 * Checks the project directory for Git repositories.
	 * 
	 * @param projectDir Project directory.
	 * 
	 * @return the repository or <code>null</code>.
	 * 
	 * @throws FileNotFoundException The project file doesn't exist.
	 * @throws IOException A Git repository was detected but not loaded.
	 */
	private File checkForGitRepositoriesUpAndDownFrom(String projectDir) {
	  File repoDir = null;
		String projectName = EditorVariables.expandEditorVariables("${pn}", null);
		String projectXprName = projectName + ".xpr";
		try {
		  // Parse the XML file to detected the referred resources.
		  SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		  saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING , true);
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
					if (FileHelper.isGitRepository(pathToCheck)) {
					  repoDir = file;
					  break;
					}
				}
			}

			if (repoDir == null) {
			  // The oxygen project might be inside a Git repository.
			  // Look into the ancestors for a Git repository.
			  File file = new File(projectDir);
			  while (file != null) {
			    if (FileHelper.isGitRepository(file.getAbsolutePath())) {
			      repoDir = file;
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
		
		return repoDir;
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
    } catch (SSHPassphraseRequiredException e) {
      status = RepositoryStatus.UNAVAILABLE;
      
      String message = translator.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      String passphrase = new PassphraseDialog(message).getPassphrase();
      if(passphrase != null){
        call();
      }
    } catch (Exception e) {
      logger.error(e);
    }

    final RepositoryStatus fStatus = status;
	  SwingUtilities.invokeLater(() -> {
      stagingPanel.getCommitPanel().setRepoStatus(fStatus);
      stagingPanel.getToolbarPanel().updateStatus();
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
	    SwingUtilities.invokeLater(() -> panelToUpdate.update(newfiles));
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
  }

}
