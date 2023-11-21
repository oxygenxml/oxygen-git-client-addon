package com.oxygenxml.git;

import java.util.Map;

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * This class is usefully on reload editor content operations.
 * 
 * @author alex_smarandache
 */
public class EditorContentReloader {
  
  /**
   * The reload action identifier.
   */
  public static final String RELOAD_ACTION_ID = "File/Reload";
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(EditorContentReloader.class);
  
  
  /**
   * Private constructor.
   * 
   * @throws UnsupportedOperationException when invoked.
   */
  private EditorContentReloader() {
    throw new UnsupportedOperationException("Instantiation of this class is not allowed!");
  }
  
  /**
   * Reload the current editor content.
   * 
   * @param pluginWS The plugin workspace.
   */
  public static void reloadCurrentEditor(final StandalonePluginWorkspace pluginWS) {
    AbstractAction reloadAction = null;
    final WSEditor editor = pluginWS.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
    final WSEditorPage page = editor.getCurrentPage();
    if(page instanceof WSAuthorEditorPage) {
        final WSAuthorEditorPage authorPage = (WSAuthorEditorPage)page;
        final Map<String, Object> commonActions = authorPage.getActionsProvider().getAuthorCommonActions();
        final Object actionObj = commonActions.get(RELOAD_ACTION_ID);
        if(actionObj instanceof AbstractAction) {
          reloadAction = (AbstractAction) actionObj;
        }
    } else if(page instanceof WSTextEditorPage) {
        WSTextEditorPage authorPage = (WSTextEditorPage)page;
        Map<String, Object> commonActions = authorPage.getActionsProvider().getTextActions();
        final Object actionObj = commonActions.get(RELOAD_ACTION_ID);
        if(actionObj instanceof AbstractAction) {
          reloadAction = (AbstractAction) actionObj;
        }
    } 
    
    if(reloadAction != null) {
      reloadAction.actionPerformed(null);
    } else {
      final Exception ex = new Exception("The reload action not found.");
      LOGGER.error(ex.getMessage(), ex);
    }
  }

}
