package com.oxygenxml.git.view.blame;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.util.URLUtil;

/**
 * Manages all blame requests.
 */
public class BlameManager {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(BlameManager.class);
  /**
   * Singleton isntance.
   */
  private static BlameManager instance;
  /**
   * All active blames. 
   */
  private Map<URL, BlamePerformer> activeBlames = new HashMap<>();
  
  /**
   * Private constructor.
   */
  private BlameManager() {
    PluginWorkspaceProvider.getPluginWorkspace().addEditorChangeListener(new WSEditorChangeListener() {
      @Override
      public void editorClosed(URL editorLocation) {
        dispose(editorLocation);
      }
    }, PluginWorkspace.MAIN_EDITING_AREA);
    
    GitAccess.getInstance().addGitListener(new GitEventAdapter() {
      @Override
      public void repositoryChanged() {
        // Dispose all blames from the previous repository.
        Iterator<URL> iterator = activeBlames.keySet().iterator();
        while (iterator.hasNext()) {
          URL url = iterator.next();
          BlamePerformer remove = activeBlames.remove(url);
          if (remove != null) {
            remove.dispose();
          }
          
        }
      }
    });
  }
  
  /**
   * @return The singleton instance.
   */
  public static BlameManager getInstance() {
    if (instance == null) {
      instance = new BlameManager();
    }
    
    return instance;
  }
  
  /**
   * Start a blame.
   * 
   * @param filePath The path of the file for which to compute the blame.
   * @param historyController Interface to the history view.
   * 
   * @throws IOException Unable to read from the given file.
   * @throws GitAPIException Git related exceptions.
   */
  public  void doBlame(
      String filePath, 
      HistoryController historyController) throws IOException, GitAPIException {
    
    try {
      URL url = new File(GitAccess.getInstance().getWorkingCopy(), filePath).toURI().toURL();
      // Check if another blame is already active and dispose it.
      dispose(url);

      // Imposing the text page will open even a DITA MAp inside the main editing area.
      boolean open = PluginWorkspaceProvider.getPluginWorkspace().open(url, EditorPageConstants.PAGE_TEXT, null);
      
      if (open) {
        WSEditor editor = PluginWorkspaceProvider.getPluginWorkspace().getEditorAccess(url, PluginWorkspace.MAIN_EDITING_AREA);
        // Currently we only support text page highlights.
        editor.changePage(EditorPageConstants.PAGE_TEXT);

        BlamePerformer showBlame = new BlamePerformer();
        showBlame.doit(GitAccess.getInstance().getRepository(), filePath, editor, historyController);
        
        activeBlames.put(URLUtil.correct(url), showBlame);
      }
    } catch (NoRepositorySelected e) {
      LOGGER.error(e, e);
    }
  }

  /**
   * Remove all blame related data for the given editor.
   *  
   * @param editorLocation Editor location.
   */
  private void dispose(URL editorLocation) {
    try {
      BlamePerformer blame = activeBlames.remove(URLUtil.correct(editorLocation));
      if (blame != null) {
        blame.dispose();
      }
    } catch (MalformedURLException e) {
      LOGGER.error(e, e);
    }
  }
}
