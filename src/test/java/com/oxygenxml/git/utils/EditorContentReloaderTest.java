package com.oxygenxml.git.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import org.mockito.Mockito;

import com.oxygenxml.git.EditorContentReloader;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.documenttype.DocumentTypeInformation;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.transformation.TransformationFeedback;
import ro.sync.exml.workspace.api.editor.transformation.TransformationScenarioNotFoundException;
import ro.sync.exml.workspace.api.editor.validation.OperationInProgressException;
import ro.sync.exml.workspace.api.editor.validation.ValidationProblemsFilter;
import ro.sync.exml.workspace.api.editor.validation.ValidationScenarioNotFoundException;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.listeners.WSEditorPageChangedListener;
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
    final StandalonePluginWorkspace spw = Mockito.mock(StandalonePluginWorkspace.class);
   
    final int[] mainAreaReloadCalls = { 0 };
    final WSEditorAdapter mainAreaEditor = new WSEditorAdapter();
    mainAreaEditor.setReloadIfChangeOnDiskDetectedAction(() -> mainAreaReloadCalls[0]++);
    Mockito.when(spw.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(mainAreaEditor);
    
    final int[] dmmReloadCalls = { 0 };
    final WSEditorAdapter dmmEditor = new WSEditorAdapter();
    Mockito.when(spw.getCurrentEditorAccess(PluginWorkspace.DITA_MAPS_EDITING_AREA)).thenReturn(dmmEditor);
    
    assertEquals(0, mainAreaReloadCalls[0]);
    assertEquals(0, dmmReloadCalls[0]);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(1, mainAreaReloadCalls[0]);
    assertEquals(0, dmmReloadCalls[0]);
    
    dmmEditor.setReloadIfChangeOnDiskDetectedAction(() -> dmmReloadCalls[0]++);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(2, mainAreaReloadCalls[0]);
    assertEquals(1, dmmReloadCalls[0]);
    
    mainAreaEditor.setReloadIfChangeOnDiskDetectedAction(null);
    
    EditorContentReloader.reloadCurrentEditor(spw);
    assertEquals(2, mainAreaReloadCalls[0]);
    assertEquals(2, dmmReloadCalls[0]);
  }
  
  
  /**
   * An adaptor for a WS Editor.
   * 
   * @author alex_smarandache
   */
  public class WSEditorAdapter implements WSEditor {
    
    private Runnable reloadIfChangeOnDiskDetectedAction;

    @Override
    public String getEncodingForSerialization() {
      return null;
    }

    @Override
    public URL getEditorLocation() {
      return null;
    }

    @Override
    public void save() {
    }

    @Override
    public void saveAs(URL location) { 
    }

    @Override
    public boolean close(boolean askForSave) {
      return false;
    }

    @Override
    public void setModified(boolean modified) {
    }

    @Override
    public boolean isNewDocument() {
      return false;
    }

    @Override
    public Reader createContentReader() {
      return null;
    }

    @Override
    public InputStream createContentInputStream() throws IOException {
      return null;
    }

    @Override
    public void reloadContent(Reader reader) {
    }

    @Override
    public void reloadContent(Reader reader, boolean discardUndoableEdits) {
    }

    @Override
    public void setEditorTabText(String tabText) {
    }

    @Override
    public void setEditorTabTooltipText(String tabTooltip) { 
    }

    @Override
    public DocumentTypeInformation getDocumentTypeInformation() {
      return null;
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public void runTransformationScenarios(String[] scenarioNames, TransformationFeedback transformationFeedback)
        throws TransformationScenarioNotFoundException {  
    }

    @Override
    public void runTransformationScenario(String scenarioName, Map<String, String> scenarioParameters,
        TransformationFeedback transformationFeedback) throws TransformationScenarioNotFoundException {
    }

    @Override
    public void stopCurrentTransformationScenario() { 
    }

    @Override
    public Thread runValidationScenarios(String[] scenarioNames)
        throws ValidationScenarioNotFoundException, OperationInProgressException {
      return null;
    }

    @Override
    public WSEditorPage getCurrentPage() {
      return null;
    }

    @Override
    public String getCurrentPageID() {
      return null;
    }

    @Override
    public void addPageChangedListener(WSEditorPageChangedListener pageChangedListener) {
    }

    @Override
    public void removePageChangedListener(WSEditorPageChangedListener pageChangedListener) {
    }

    @Override
    public void addEditorListener(WSEditorListener editorListener) {
    }

    @Override
    public WSEditorListener[] getEditorListeners() {
      return null;
    }

    @Override
    public void removeEditorListener(WSEditorListener editorListener) {
    }

    @Override
    public void changePage(String pageID) {
    }

    @Override
    public void addValidationProblemsFilter(ValidationProblemsFilter validationProblemsFilter) {
    }

    @Override
    public void removeValidationProblemsFilter(ValidationProblemsFilter validationProblemsFilter) {
    }

    @Override
    public boolean checkValid() {
      return false;
    }

    @Override
    public boolean checkValid(boolean automatic) {
      return false;
    }

    @Override
    public Object getComponent() {
      return null;
    }

    @Override
    public void setEditable(boolean editable) { 
    }

    @Override
    public boolean isEditable() {
      return false;
    }

    @Override
    public String getContentType() {
      return null;
    }

    public void reloadIfChangeOnDiskDetected() {
      if(reloadIfChangeOnDiskDetectedAction != null) {
        reloadIfChangeOnDiskDetectedAction.run();
      }
    }
    
    public void setReloadIfChangeOnDiskDetectedAction(Runnable reloadIfChangeOnDiskDetectedAction) {
      this.reloadIfChangeOnDiskDetectedAction = reloadIfChangeOnDiskDetectedAction;
    }
    
  }

}
