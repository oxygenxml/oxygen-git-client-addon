package com.oxygenxml.git.validation;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.UtilAccess;

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
  private Optional<Method> getMainFilesMethod = Optional.empty();

  /**
   * The project controller.
   */
  private ProjectController projectController;
  
  /**
   * The util access.
   */
  private UtilAccess utilAccess;
  
  /**
   * Get content type method.
   */
  private Optional<Method> getContentTypeMethod = Optional.empty();
  

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
  public static OxygenAPIWrapper getInstance() {
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
        if(projectController != null) {
          getMainFilesMethod = Optional.ofNullable(projectController.getClass()
              .getMethod("getMainFileResourcesIterator"));
          getMainFilesMethod.get().setAccessible(true);
        }
        utilAccess = pluginWorkspaceAccess.getUtilAccess();
        if(utilAccess != null) {
          Class<? extends UtilAccess> utilAccessClazz = utilAccess.getClass();
          getContentTypeMethod = Optional.ofNullable(utilAccessClazz.getMethod("getContentType", String.class));
        }
      } catch (Throwable e) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * @return An iterator over a list with project main files or an empty iterator.
   */
  @NonNull
  public Iterator<URL> getMainFileResourcesIterator() {
    if(getMainFilesMethod.isPresent()) {
      try {
        return (Iterator<URL>) getMainFilesMethod.get().invoke(projectController);
      } catch (Throwable e) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }          
      }
    }
   
    return Collections.emptyIterator();
  }

  /**
   * @return <code>true</code> if the main files are accessible.
   */
  public boolean isAvailable() {
    return getMainFilesMethod.isPresent();
  }
  
  /**
   * Get the content type for the given URL. The content type is detected from the file extension based on the file extension 
   * associations saved in the application preferences.<br> 
   * 
   * @param systemID The systemID to get the content type for.
   * 
   * @return the content type string or null if there is no mapping. 
   * The content type is returned as a mime type value, for example "text/xml" for XML documents
   * 
   * @since 24.0
   */
  public String getContentType(final String systemID) throws Throwable {
    if(!getContentTypeMethod.isPresent()) {
      throw new Exception("Method is not available");
    }
    final Object invokeResult =  getContentTypeMethod.get().invoke(utilAccess, systemID);
    return Optional.ofNullable(invokeResult).filter(
        val -> val.getClass().isAssignableFrom(String.class)).map(String.class::cast).orElse(null);
  }
  
}
