package com.oxygenxml.git.watcher;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.utils.GitOperationScheduler;

import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
public class RepositoryChangeWatcher {
  /**
   * Add a listener for the editing areas and schedule the first search for changes in the remote repository
   * @param standalonePluginWorkspace
   */
  public static void initialize(StandalonePluginWorkspace standalonePluginWorkspace) {
    
    RepositoryChangeWatcher.addListeners4EditingAreas(standalonePluginWorkspace);
    String value = OptionsManager.getInstance().getWarnOnUpstreamChange();
    GitOperationScheduler.getInstance().schedule(() -> RepositoryChangeListeners.task(value), 400);
  }
  
  /**
   * Creates a new listener to supervise the remote changes and adds it to the editing areas
   * @param standalonePluginWorkspace
   */
  public static void addListeners4EditingAreas(StandalonePluginWorkspace standalonePluginWorkspace) {
    WSEditorChangeListener editorListenerAlways = RepositoryChangeListeners.createWatcherListener();
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.MAIN_EDITING_AREA);
    standalonePluginWorkspace.addEditorChangeListener(editorListenerAlways, StandalonePluginWorkspace.DITA_MAPS_EDITING_AREA);
  }
}
