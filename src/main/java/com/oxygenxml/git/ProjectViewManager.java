package com.oxygenxml.git;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Manager for the Project view.
 * 
 * @author Beniamin Savu
 */
public class ProjectViewManager {

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
	    Translator translator,
	    GitActionsProvider gitActionsProvider) {
		// try to get method from 19.1 version
		try {
			// get the getProjectManager method
			Method getProjectManager = pluginWorkspaceAccess.getClass().getMethod("getProjectManager");

			// get the projectManager class
			Class projectManagerClass = getProjectManager.getReturnType();

			// get the projectPopupMenuCustomizer interface
			Class projectPopupMenuCustomizerClass = 
			    Class.forName("ro.sync.exml.workspace.api.standalone.project.ProjectPopupMenuCustomizer");
			
			// create a ProxyInstance of projectPopupMenuCustomizer
			Object proxyProjectPopupMenuCustomizerImpl = Proxy.newProxyInstance(
					projectPopupMenuCustomizerClass.getClassLoader(), new Class[] { projectPopupMenuCustomizerClass },
					new ProjectPopupMenuCustomizerInvocationHandler(pluginWorkspaceAccess, translator, gitActionsProvider));

			// get the project manager object
			Object projectManager = getProjectManager.invoke(pluginWorkspaceAccess);

			// get the addPopUpMenuCustomizer method
			Method addPopUpMenuCustomizerMethod = projectManagerClass.getMethod("addPopUpMenuCustomizer",
					new Class[] { projectPopupMenuCustomizerClass });
			// invoke addPopUpMenuCustomizer method
			addPopUpMenuCustomizerMethod.invoke(projectManager, proxyProjectPopupMenuCustomizerImpl);

		} catch (IllegalAccessException e2) {
		} catch (IllegalArgumentException e2) {
		} catch (InvocationTargetException e2) {
		}	catch (ClassNotFoundException e) {
		  // The method wasn't found because it's used a older version
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
	}

	/**
	 * Get the selected files and directories from the Project view. This method does not
	 * return the files from inside the selected directories.
	 * 
	 * @param pluginWorkspaceAccess  Plug-in workspace access.
	 * 
	 * @return the selected files.
	 */
	public static File[] getSelectedFilesAndDirsShallow(StandalonePluginWorkspace pluginWorkspaceAccess) {
	  File[] toReturn = null;
		try {
			// get the getProjectManager method
			Method getProjectManager = pluginWorkspaceAccess.getClass().getMethod("getProjectManager");

			// get the projectManager class
			Class projectManagerClass = getProjectManager.getReturnType();

			// get the projectManager
			Object projectManager = getProjectManager.invoke(pluginWorkspaceAccess);

			// get the getSelectedFiles method
			Method getSelectedFiles = projectManagerClass.getMethod("getSelectedFiles");

			// get the selected files
			toReturn = (File[]) getSelectedFiles.invoke(projectManager);
			
		} catch (IllegalAccessException e2) {
		} catch (IllegalArgumentException e2) {
		} catch (InvocationTargetException e2) {
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
		
		return toReturn;
	}
	
	/**
	 * Get all the selected files + the files from inside the selected directories in the Project view.
	 * 
	 * @param pluginWorkspaceAccess  Plug-in workspace access.
	 * 
	 * @return the selected files and all the files from inside the selected directories in the Project view.
	 */
	public static Set<String> getSelectedFilesDeep(StandalonePluginWorkspace pluginWorkspaceAccess){
		File[] selectedFiles = getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
		Set<String> files = new HashSet<String>();
		
		for (int i = 0; i < selectedFiles.length; i++) {
			files.addAll(FileHelper.getAllFilesFromPath(selectedFiles[i].getAbsolutePath()));
		}
		
		return files;
	}

}