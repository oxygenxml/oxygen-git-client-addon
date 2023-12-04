package com.oxygenxml.git;

import java.util.Collections;
import java.util.Map;

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.ditamap.WSDITAMapEditorPage;
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
   * Reload the current editor content and DMM editor.
   * 
   * @param pluginWS The plugin workspace.
   */
  public static void reloadCurrentEditor(final StandalonePluginWorkspace pluginWS) {
    reloadEditorContent(pluginWS.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA));
    reloadEditorContent(pluginWS.getCurrentEditorAccess(PluginWorkspace.DITA_MAPS_EDITING_AREA));
  }

  /**
   * Reload the content for the given editor.
   * 
   * @param editor The current editor to reload its content.
   */
  private static void reloadEditorContent(final WSEditor editor) {
    if(editor == null) {
      return;
    }
    
    AbstractAction reloadAction = null;
    final WSEditorPage page = editor.getCurrentPage();
    Map<String, Object> actions = Collections.emptyMap();
    
    if(page instanceof WSDITAMapEditorPage) {
      final WSDITAMapEditorPage mapPage = (WSDITAMapEditorPage)page;
      actions = mapPage.getActionsProvider().getActions();
    } else if(page instanceof WSAuthorEditorPage) {
      final WSAuthorEditorPage authorPage = (WSAuthorEditorPage)page;
      actions = authorPage.getActionsProvider().getAuthorCommonActions();
    } else if(page instanceof WSTextEditorPage) {
      final WSTextEditorPage authorPage = (WSTextEditorPage)page;
      actions = authorPage.getActionsProvider().getTextActions();
    } 
    
    final Object actionObj = actions.get(RELOAD_ACTION_ID);
    if(actionObj instanceof AbstractAction) {
      reloadAction = (AbstractAction) actionObj;
      if(reloadAction != null) {
        reloadAction.actionPerformed(null);
      } 
    } else {
      final Exception ex = new Exception("The reload action not found.");
      LOGGER.error(ex.getMessage(), ex);
    }
    
  }

}
