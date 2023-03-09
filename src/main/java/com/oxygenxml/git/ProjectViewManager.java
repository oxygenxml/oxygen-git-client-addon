package com.oxygenxml.git;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Manager for the Project view.
 * 
 * @author Beniamin Savu
 */
public class ProjectViewManager {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectViewManager.class.getName());
  
  /**
   * Hiden constructor.
   */
  private ProjectViewManager() {
    // Nothing.
  }

	/**
	 * Add a pop-up menu customizer to the Project view's contextual menu. Add git specific actions.
	 * 
	 * @param gitActionsProvider Actions provider.
	 */
	public static void addPopUpMenuCustomizer(ProjectMenuGitActionsProvider gitActionsProvider) {
	  StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
		try {
		  pluginWS.getProjectManager().addPopUpMenuCustomizer(popUp -> {
        // Proceed only if the selected files pertain to a Git repository
        if (areSelectedFilesFromGitRepo()) {
          UIUtil.addGitActions((JPopupMenu) popUp, gitActionsProvider.getActionsForProjectViewSelection());
        }
      });
		} catch (Exception e) {
		  if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug(e.getMessage(), e);
		  }
		} 
	}

	/**
   * Check if the selected files from the Project view pertain to a Git repository.
   * 
   * @return <code>true</code> if the selected files pertain to a Git repository,
   *  <code>false</code> otherwise.
   */
  private static boolean areSelectedFilesFromGitRepo() {
    boolean isGit = false;
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    File[] selectedFiles = pluginWS.getProjectManager().getSelectedFiles();
    for (int i = 0; i < selectedFiles.length; i++) {
      isGit = FileUtil.isFromGitRepo(new File(selectedFiles[i].getAbsolutePath())); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
      if (!isGit) {
        // If one of the selected project files does not pertain to a Git repository,
        // it means that the project is not a Git one
        break;
      }
    }
    return isGit;
  }

	/**
	 * Get all the selected files + the files from inside the selected directories in the Project view.
	 * 
	 * @return the selected files and all the files from inside the selected directories in the Project view.
	 */
	public static Set<String> getSelectedFilesDeep(){
	  Set<String> files = new HashSet<>();

	  StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
	  File[] selectedFiles = pluginWS.getProjectManager().getSelectedFiles();
	  for (int i = 0; i < selectedFiles.length; i++) {
	    files.addAll(FileUtil.getAllFilesFromPath(selectedFiles[i].getAbsolutePath()));
	  }

	  return files;
	}
	
}