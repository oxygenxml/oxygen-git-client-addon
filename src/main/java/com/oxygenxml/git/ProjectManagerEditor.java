package com.oxygenxml.git;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JMenu;

import com.oxygenxml.git.utils.FileHelper;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class ProjectManagerEditor {

	/**
	 * For 19.1 oxygen version add a MenuItem with given action in contextual menu
	 * of project manager. For older version than 19.1 do nothing.
	 * 
	 * @param pluginWorkspaceAccess
	 *          The StandalonePluginWorkspace.
	 * @param menuItem
	 *          The item to be added
	 */
	public static void addPopUpMenuCustomizer(StandalonePluginWorkspace pluginWorkspaceAccess, JMenu menuItem) {
		// try to get method from 19.1 version
		try {
			// get the getProjectManager method
			Method getProjectManager = pluginWorkspaceAccess.getClass().getMethod("getProjectManager");

			// get the projectManager class
			Class projectManagerClass = getProjectManager.getReturnType();

			// get the projectPopupMenuCustomizer interface
			Class projectPopupMenuCustomizerClass = Class
					.forName("ro.sync.exml.workspace.api.standalone.project.ProjectPopupMenuCustomizer");
			
			// create a ProxyInstance of projectPopupMenuCustomizer
			Object proxyProjectPopupMenuCustomizerImpl = Proxy.newProxyInstance(
					projectPopupMenuCustomizerClass.getClassLoader(), new Class[] { projectPopupMenuCustomizerClass },
					new ProjectPopupMenuCustomizerInvocationHandler(menuItem, pluginWorkspaceAccess));

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
		}
		// The method wasn't found because it's used a older version
		catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
	}

	public static File[] getSelectedFiles(StandalonePluginWorkspace pluginWorkspaceAccess) {
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
			File[] selectedFiles = (File[]) getSelectedFiles.invoke(projectManager);
			return selectedFiles;
			
		} catch (IllegalAccessException e2) {
		} catch (IllegalArgumentException e2) {
		} catch (InvocationTargetException e2) {
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
		return null;
	}
	
	public static Set<String> getAllFiles(StandalonePluginWorkspace pluginWorkspaceAccess){
		File[] selectedFiles = getSelectedFiles(pluginWorkspaceAccess);
		Set<String> files = new HashSet<String>();
		
		for (int i = 0; i < selectedFiles.length; i++) {			
			files.addAll(FileHelper.search(selectedFiles[i].getAbsolutePath()));
		}
		
		return files;
	}

}