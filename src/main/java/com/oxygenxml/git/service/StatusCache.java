package com.oxygenxml.git.service;

import java.net.URL;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;

import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;

/**
 * A cache intended to avoid reading the file system too often.
 * @author alex_jitianu
 */
public class StatusCache {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(StatusCache.class);
  /**
   * Inner cache.
   */
  private GitStatus cache = null;
  /**
   * A supplier of a newly computed status.
   */
  private Supplier<Git> statusComputer;
  
  /**
   * Constructor.
   * 
   * @param listeners The repository for installing Git  event listeners.
   * @param statusComputer A supplier of a newly computed status.
   */
  public StatusCache(GitListeners listeners, Supplier<Git> statusComputer) {
    this.statusComputer = statusComputer;
    listeners.addGitPriorityListener(new GitEventAdapter() {
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if (// If you push changes the state of the working copy doesn't change.
            info.getGitOperation() != GitOperation.PUSH &&
            // If another branch is removed the state of the working copy doesn't change.
            info.getGitOperation() != GitOperation.DELETE_BRANCH) {
          resetCache();
        }
      }
    });
  }
  
  /**
   * @return A status of the currently loaded Git repository.
   */
  public synchronized GitStatus getStatus() {
    if (cache == null) {
      cache = new GitStatusCommand(statusComputer).getStatus();
    }
    return cache;
  }

  /**
   * Reset inner cache.
   */
  public synchronized void resetCache() {
    logger.debug("Reset cahche", new Exception());
    cache = null;
    
  }

  /**
   * Install hooks on the editing area to invalidate inner cache when files from 
   * the repository are edited.
   * 
   * @param pluginWorkspace Workspace access.
   * @param gitRefreshSupport Plugin's refresh support.
   */
  public void installEditorsHook(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.addEditorChangeListener(
        new WSEditorChangeListener() {
          @Override
          public void editorOpened(final URL editorLocation) {
            addEditorSaveHook(pluginWorkspace.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA));
          }
        },
        PluginWorkspace.MAIN_EDITING_AREA);
  }

  /**
   * Adds a hook to refresh the models if the editor is part of the Git working copy.
   * 
   * @param editorLocation Editor to check.
   * @param gitRefreshSupport Plugin's refresh support.
   */
  private void addEditorSaveHook(WSEditor editorAccess) {
    if (editorAccess != null) {
      editorAccess.addEditorListener(new WSEditorListener() {
        @Override
        public void editorSaved(int operationType) {
          if (RepoUtil.isFileFromRepository(editorAccess.getEditorLocation())) {
            resetCache();
          }
        }
      });
    }
  }
}
