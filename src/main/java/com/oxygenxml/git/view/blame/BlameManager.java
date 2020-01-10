package com.oxygenxml.git.view.blame;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.google.common.io.Files;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;

/**
 * Manages all blame requests.
 */
public class BlameManager {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(BlameManager.class);
  /**
   * Singleton instance.
   */
  private static BlameManager instance;
  /**
   * All active blames. 
   */
  private Map<String, BlamePerformer> activeBlames = new HashMap<>();
  
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
        Iterator<String> iterator = activeBlames.keySet().iterator();
        while (iterator.hasNext()) {
          String url = iterator.next();
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
      File file = new File(GitAccess.getInstance().getWorkingCopy(), filePath);
      URL url = file.toURI().toURL();
      // Check if another blame is already active and dispose it.
      dispose(url);

      String ext = Files.getFileExtension(file.getName());
      boolean isProjectExt = "xpr".equals(ext);
      
      boolean open = PluginWorkspaceProvider.getPluginWorkspace().open(
          url,
          // Imposing the text page will open even a DITA Map inside the main editing area.
          EditorPageConstants.PAGE_TEXT,
          // EXM-44423: open project as XML.
          isProjectExt ? "text/xml" : null);
      if (open) {
        WSEditor editor = PluginWorkspaceProvider.getPluginWorkspace().getEditorAccess(url, PluginWorkspace.MAIN_EDITING_AREA);
        if (editor != null) {
          // Currently we only support text page highlights.
          editor.changePage(EditorPageConstants.PAGE_TEXT);

          BlamePerformer blamePerformer = new BlamePerformer();
          blamePerformer.doit(GitAccess.getInstance().getRepository(), filePath, editor, historyController);

          String key = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().correctURL(url.toExternalForm());
          activeBlames.put(key, blamePerformer);
        } else {
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Failed to open editor: " + url);
          LOGGER.error("Editor not found: " + url);
        }
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
    String key = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().correctURL(editorLocation.toExternalForm());
    BlamePerformer blame = activeBlames.remove(key);
    if (blame != null) {
      blame.dispose();
    }
  }
}
