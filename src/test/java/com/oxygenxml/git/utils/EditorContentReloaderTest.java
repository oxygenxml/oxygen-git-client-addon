package com.oxygenxml.git.utils;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;

import org.mockito.Mockito;

import com.oxygenxml.git.EditorContentReloader;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.actions.AuthorActionsProvider;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.actions.TextActionsProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Contains tests for editor content reloading.
 * 
 * @author alex_smarandache
 */
public class EditorContentReloaderTest extends JFCTestCase { 
  
  /**
   * <p><b>Description:</b> This test cover the current editor reload cases.</p>
   * 
   * <p><b>Bug ID:</b> EXM-47707</p>
   *
   * @author alex_smarandache
   *
   */
  public void testReloadCurrentEditor() {
    final int[] textPageReloadCalls = { 0 };
    final int[] authorPageReloadCalls = { 0 };
    final WSEditor editor = Mockito.mock(WSEditor.class);
    
    final WSAuthorEditorPage authorPage = Mockito.mock(WSAuthorEditorPage.class);
    Mockito.when(editor.getCurrentPage()).thenReturn(authorPage);
    final AuthorActionsProvider authorActionsProvider = Mockito.mock(AuthorActionsProvider.class);
    final AbstractAction authorReloadAct = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        authorPageReloadCalls[0]++;
      }
    };
    final Map<String, Object> authorCommonActions = new HashMap<>();
    authorCommonActions.put(EditorContentReloader.RELOAD_ACTION_ID, authorReloadAct);
    Mockito.when(authorActionsProvider.getAuthorCommonActions()).thenReturn(authorCommonActions);
    Mockito.when(authorPage.getActionsProvider()).thenReturn(authorActionsProvider);
    
    final StandalonePluginWorkspace spw = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(spw.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(editor);
    
    assertEquals(0, textPageReloadCalls[0]);
    assertEquals(0, authorPageReloadCalls[0]);
   
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(0, textPageReloadCalls[0]);
    assertEquals(1, authorPageReloadCalls[0]);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(0, textPageReloadCalls[0]);
    assertEquals(2, authorPageReloadCalls[0]);
    
    final WSTextEditorPage textPage = Mockito.mock(WSTextEditorPage.class);
    final TextActionsProvider textActionsProvider = Mockito.mock(TextActionsProvider.class);
    final AbstractAction textReloadAct = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        textPageReloadCalls[0]++;
      }
    };
    final Map<String, Object> textCommonActions = new HashMap<>();
    textCommonActions.put(EditorContentReloader.RELOAD_ACTION_ID, textReloadAct);
    Mockito.when(textActionsProvider.getTextActions()).thenReturn(textCommonActions);
    Mockito.when(textPage.getActionsProvider()).thenReturn(textActionsProvider);
    Mockito.when(editor.getCurrentPage()).thenReturn(textPage);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(1, textPageReloadCalls[0]);
    assertEquals(2, authorPageReloadCalls[0]);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(2, textPageReloadCalls[0]);
    assertEquals(2, authorPageReloadCalls[0]);
    
    Mockito.when(editor.getCurrentPage()).thenReturn(null);
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(2, textPageReloadCalls[0]);
    assertEquals(2, authorPageReloadCalls[0]);
  }

}
