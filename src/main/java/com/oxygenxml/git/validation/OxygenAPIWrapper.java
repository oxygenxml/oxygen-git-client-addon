package com.oxygenxml.git.validation;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

/**
 * This class is a wrapper for some Oxygen APIs extracted using reflexion.
 * 
 * TODO Replace this class with real APIs when Oxygen 25 will be the minimum version needed to run latest Git Client.
 * 
 * @author alex_smarandache
 *
 */
public class OxygenAPIWrapper {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(OxygenAPIWrapper.class);

  /**
   * Get all main files from the current project opened in "Project" view.
   */
  private Method getMainFilesMethod;

  /**
   * The project controller.
   */
  private ProjectController projectController;
  
  /**
   * <code>true</code> if the method to get main files is accessible.
   */
  private boolean isGetMainFilesAccessible = true;

  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class SingletonHelper {
    static final OxygenAPIWrapper INSTANCE = new OxygenAPIWrapper();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  static OxygenAPIWrapper getInstance() {
    return SingletonHelper.INSTANCE;
  }

  /**
   * Hidden constructor.
   */
  private OxygenAPIWrapper() {
    final PluginWorkspace pluginWorkspaceAccess = PluginWorkspaceProvider.getPluginWorkspace();
    if(pluginWorkspaceAccess instanceof StandalonePluginWorkspace) {
      final StandalonePluginWorkspace standalonePluginWorkspace = 
          (StandalonePluginWorkspace) pluginWorkspaceAccess;
      projectController = standalonePluginWorkspace.getProjectManager();
      try {
        getMainFilesMethod = projectController.getClass().getMethod("getMainFileResourcesIterator");
        getMainFilesMethod.setAccessible(true);
      } catch (NoSuchMethodException | SecurityException e) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }
        isGetMainFilesAccessible = false;
      }
    }
  }

  /**
   * @return An iterator over a list with project main files or an empty iterator.
   */
  @NonNull
  public Iterator<URL> getMainFileResourcesIterator() {
    try {
      return (Iterator<URL>) getMainFilesMethod.invoke(projectController);
    } catch (Exception e) {
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }          
      return Collections.emptyIterator();
    }
  }

  /**
   * @return <code>true</code> if the main files are accessible.
   */
  public boolean isGetMainFilesAccessible() {
    return isGetMainFilesAccessible;
  }
  
}
