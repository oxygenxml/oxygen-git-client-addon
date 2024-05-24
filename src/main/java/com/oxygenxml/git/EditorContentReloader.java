package com.oxygenxml.git;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * This class is usefully on reload editor content operations.
 * 
 * @author alex_smarandache
 */
public class EditorContentReloader {

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
   * This method will reload contents(if any change exists) from the editors opened in DMM or main area.
   * 
   * @param pluginWS The plugin workspace.
   */
  public static void reloadCurrentEditor(final StandalonePluginWorkspace pluginWS) {
    reloadEditor(pluginWS, PluginWorkspace.MAIN_EDITING_AREA);
    reloadEditor(pluginWS, PluginWorkspace.DITA_MAPS_EDITING_AREA);
  }

  /**
   * This method reload the current editor with the given identifier.
   * 
   * @param pluginWS          The plugin workspace.
   * @param editorIdentifier  The identifier of the editor.
   */
  private static void reloadEditor(final StandalonePluginWorkspace pluginWS, final int editorIdentifier) {
    // Update it after the 26.1 Oxygen API will be the minimum version required
    final WSEditor editor = pluginWS.getCurrentEditorAccess(editorIdentifier);
    if (editor != null) {
      try {
        final Class<?> editorClass = editor.getClass();
        final Method reloadMethod = editorClass.getMethod("reloadIfChangeOnDiskDetected");
        reloadMethod.invoke(editor);
      } catch (Exception e) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }

}
