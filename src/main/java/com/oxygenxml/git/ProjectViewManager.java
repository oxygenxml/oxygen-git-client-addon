package com.oxygenxml.git;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.oxygenxml.git.utils.FileHelper;

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
  private static final Logger logger = Logger.getLogger(ProjectViewManager.class.getName());
  
  /**
   * The "getProjectManager()" method name.
   */
  private static final String GET_PROJECT_MANAGER_METHOD_NAME = "getProjectManager";

  /**
   * Hiden constructor.
   */
  private ProjectViewManager() {
    // Nothing.
  }

	/**
	 * Add a pop-up menu customizer to the Project view's contextual menu. Add git specific actions.
	 * <br/><br/>
	 * oXygen 19.1+. For older versions, do nothing.
	 * 
	 * @param pluginWorkspaceAccess
	 *          The StandalonePluginWorkspace.
	 * @param menuItem
	 *          The item to be added
	 */
	public static void addPopUpMenuCustomizer(
	    StandalonePluginWorkspace pluginWorkspaceAccess,
	    GitMenuActionsProvider gitActionsProvider) {
		// try to get method from 19.1 version
		try {
			// get the getProjectManager method
			Method getProjectManager = pluginWorkspaceAccess.getClass().getMethod(GET_PROJECT_MANAGER_METHOD_NAME);

			// get the projectManager class
			Class<?> projectManagerClass = getProjectManager.getReturnType();

			// get the projectPopupMenuCustomizer interface
			Class<?> projectPopupMenuCustomizerClass = 
			    Class.forName("ro.sync.exml.workspace.api.standalone.project.ProjectPopupMenuCustomizer");
			
			// create a ProxyInstance of projectPopupMenuCustomizer
			Object proxyProjectPopupMenuCustomizerImpl = Proxy.newProxyInstance(
					projectPopupMenuCustomizerClass.getClassLoader(), new Class[] { projectPopupMenuCustomizerClass },
					new ProjectPopupMenuCustomizerInvocationHandler(pluginWorkspaceAccess, gitActionsProvider));

			// get the project manager object
			Object projectManager = getProjectManager.invoke(pluginWorkspaceAccess);

			// get the addPopUpMenuCustomizer method
			Method addPopUpMenuCustomizerMethod = projectManagerClass.getMethod("addPopUpMenuCustomizer",
					projectPopupMenuCustomizerClass);
			// invoke addPopUpMenuCustomizer method
			addPopUpMenuCustomizerMethod.invoke(projectManager, proxyProjectPopupMenuCustomizerImpl);

		} catch (Exception e) {
		  if (logger.isDebugEnabled()) {
		    logger.debug(e, e);
		  }
		} 
	}

	/**
	 * Get the selected files and directories from the Project view. This method does not
	 * return the files from inside the selected directories.
	 * 
	 * @param pluginWorkspaceAccess  Plug-in workspace access.
	 * 
	 * @return the selected files or an empty array. Never null.
	 */
	public static File[] getSelectedFilesAndDirsShallow(StandalonePluginWorkspace pluginWorkspaceAccess) {
	  File[] toReturn = null;
		try {
			// get the getProjectManager method
			Method getProjectManager = pluginWorkspaceAccess.getClass().getMethod(GET_PROJECT_MANAGER_METHOD_NAME);

			// get the projectManager class
			Class<?> projectManagerClass = getProjectManager.getReturnType();

			// get the projectManager
			Object projectManager = getProjectManager.invoke(pluginWorkspaceAccess);

			// get the getSelectedFiles method
			Method getSelectedFiles = projectManagerClass.getMethod("getSelectedFiles");

			// get the selected files
			toReturn = (File[]) getSelectedFiles.invoke(projectManager);
			
		} catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    } 
		
		return toReturn == null ? new File[0] : toReturn;
	}
	
	/**
	 * Get all the selected files + the files from inside the selected directories in the Project view.
	 * 
	 * @param pluginWorkspaceAccess  Plug-in workspace access.
	 * 
	 * @return the selected files and all the files from inside the selected directories in the Project view.
	 */
	public static Set<String> getSelectedFilesDeep(StandalonePluginWorkspace pluginWorkspaceAccess){
	  Set<String> files = new HashSet<>();

	  File[] selectedFiles = getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
	  if (selectedFiles != null) {
	    for (int i = 0; i < selectedFiles.length; i++) {
	      files.addAll(FileHelper.getAllFilesFromPath(selectedFiles[i].getAbsolutePath()));
	    }
	  }

	  return files;
	}
	
	/**
	 * Refresh the given folders from the Project view.
	 * 
	 * @param projectFolders The project folders.
	 */
	public static void refreshFolders(File[] projectFolders) {
	  try {
	    // The "ProjectController#refreshFolders()" API was introduced in 19.1,
	    // but the plug-in should be compatible with older versions.
	    StandalonePluginWorkspace workspaceAccess = 
	        (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      Method getProjectManagerMethod = StandalonePluginWorkspace.class.getMethod(GET_PROJECT_MANAGER_METHOD_NAME);
      Object projectManager = getProjectManagerMethod.invoke(workspaceAccess);
      Class<?> projectControllerClass = Class.forName(
          "ro.sync.exml.workspace.api.standalone.project.ProjectController");
      Method refreshFoldersMethod = projectControllerClass.getMethod("refreshFolders", File[].class);
      refreshFoldersMethod.invoke(projectManager, (Object) projectFolders);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException 
        | IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e, e);
      }
    }
	}

}