package com.oxygenxml.git.view.commit;

import java.io.File;
import java.net.URL;

import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.dialog.internal.MessageDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.CommitAndStatusPanel;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Contains tests for the case when commit and there are unsaved modified files in repository.
 * 
 * @author alex_smarandache
 */
public class CommitWithUnsavedFilesInRepositoryTest extends GitTestBase {

  /**
   * The git controller mock.
   */
  private GitController gitCtrlMock;
  
  /**
   * The string builder for loging saving files.
   */
  private StringBuilder savingFileLogBuilder;

  /**
   * The setup of the test.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    String local1Repository = "target/test-resources/CommitTests/testCommitWithUnsavedFiles-local";
    String remoteRepository = "target/test-resources/CommitTests/testCommitWithUnsavedFiles-remote";
    Repository local1Repo = createRepository(local1Repository);
    Repository remoteRepo = createRepository(remoteRepository);
    bindLocalToRemote(local1Repo, remoteRepo);
    
    // LOCAL 1
    GitAccess gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(local1Repository);
    
    pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    gitCtrlMock = Mockito.mock(GitController.class);
 
    Mockito.when(gitCtrlMock.getGitAccess()).thenReturn(gitAccess);
    
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    savingFileLogBuilder = new StringBuilder();
  }

  /**
   * <p><b>Description:</b> Test if warning dialog appear when the repository has unsaved files.</p>
   * <p><b>Bug ID:</b> EXM-54645</p>
   *
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void test_Detect_UnsavedModifiedFile() throws Exception {
    URL[] mainEditingAreaURLs = new java.net.URL[1];
    URL[] ditamapsEditingURLs = new URL[0];
    File modifiedFile = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile.txt");
    mainEditingAreaURLs[0] = modifiedFile.toURI().toURL();
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[0])).thenReturn(modifiedFile);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(mainEditingAreaURLs);
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.DITA_MAPS_EDITING_AREA)).thenReturn(ditamapsEditingURLs);
    
    addEditorForFile(mainEditingAreaURLs[0], true, PluginWorkspace.MAIN_EDITING_AREA);
    
    CommitAndStatusPanel commitAndStatusPanel = new CommitAndStatusPanel(gitCtrlMock);
    commitAndStatusPanel.getCommitMessageArea().setText("Commit message");
    commitAndStatusPanel.getCommitButton().doClick();
    OKCancelDialog warnDialog = (OKCancelDialog) findDialog(Tags.UNSAVED_FILES_DETECTED);
    assertNotNull(warnDialog);
    warnDialog.getCancelButton().doClick();
    sleep(300);
    assertTrue(savingFileLogBuilder.toString().isEmpty());
  }
  
  /**
   * <p><b>Description:</b> Test if warning dialog not appear when all files are saved.</p>
   * <p><b>Bug ID:</b> EXM-54645</p>
   *
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void test_NoModifiedFilesInRepository() throws Exception {
    URL[] mainEditingAreaURLs = new java.net.URL[2];
    URL[] ditamapsEditingURLs = new java.net.URL[1];
    
    File file = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile1.txt");
    File savedFile = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile3.txt");
    File anotherRepoFile = new File(gitCtrlMock.getGitAccess().getWorkingCopy().getParent() + "anotherrepo", "anotherRepoFile.txt");
    
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(mainEditingAreaURLs);
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.DITA_MAPS_EDITING_AREA)).thenReturn(ditamapsEditingURLs);
    
    mainEditingAreaURLs[0] = anotherRepoFile.toURI().toURL();
    mainEditingAreaURLs[1] = savedFile.toURI().toURL();
    ditamapsEditingURLs[0] = file.toURI().toURL();
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[0])).thenReturn(anotherRepoFile);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[1])).thenReturn(savedFile);
    Mockito.when(utilAccessMock.locateFile(ditamapsEditingURLs[0])).thenReturn(file);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    
    addEditorForFile(mainEditingAreaURLs[0], true, PluginWorkspace.MAIN_EDITING_AREA);
    addEditorForFile(mainEditingAreaURLs[1], false, PluginWorkspace.MAIN_EDITING_AREA);
    addEditorForFile(ditamapsEditingURLs[0], false, PluginWorkspace.DITA_MAPS_EDITING_AREA);
    
    CommitAndStatusPanel commitAndStatusPanel = new CommitAndStatusPanel(gitCtrlMock);
    commitAndStatusPanel.getCommitMessageArea().setText("Commit message");
    commitAndStatusPanel.getCommitButton().doClick();
    MessageDialog warnDialog = (MessageDialog) findDialog(Tags.UNSAVED_FILES_DETECTED);
    assertNull(warnDialog);
  }
  
  /**
   * <p><b>Description:</b> Test if the save all action works properly.</p>
   * <p><b>Bug ID:</b> EXM-54645</p>
   *
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void test_DetectUnsavedModifiedFiles_SaveAllAction() throws Exception {
    URL[] mainEditingAreaURLs = new java.net.URL[3];
    URL[] ditamapsEditingURLs = new java.net.URL[1];
    
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(mainEditingAreaURLs);
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.DITA_MAPS_EDITING_AREA)).thenReturn(ditamapsEditingURLs);
    
    File modifiedFile1 = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile1.txt");
    File savedFile = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile3.txt");
    File modifiedFile2 = new File(gitCtrlMock.getGitAccess().getWorkingCopy(), "myfile2.txt");
    File anotherRepoFile = new File(gitCtrlMock.getGitAccess().getWorkingCopy().getParent() + "anotherrepo", "anotherRepoFile.txt");
    
    mainEditingAreaURLs[0] = modifiedFile1.toURI().toURL();
    mainEditingAreaURLs[1] = anotherRepoFile.toURI().toURL();
    mainEditingAreaURLs[2] = savedFile.toURI().toURL();
    ditamapsEditingURLs[0] = modifiedFile2.toURI().toURL();
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[0])).thenReturn(modifiedFile1);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[1])).thenReturn(anotherRepoFile);
    Mockito.when(utilAccessMock.locateFile(mainEditingAreaURLs[2])).thenReturn(savedFile);
    Mockito.when(utilAccessMock.locateFile(ditamapsEditingURLs[0])).thenReturn(modifiedFile2);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.MAIN_EDITING_AREA)).thenReturn(mainEditingAreaURLs);
    Mockito.when(pluginWSMock.getAllEditorLocations(PluginWorkspace.DITA_MAPS_EDITING_AREA)).thenReturn(ditamapsEditingURLs);
    
    addEditorForFile(mainEditingAreaURLs[0], true, PluginWorkspace.MAIN_EDITING_AREA);
    addEditorForFile(mainEditingAreaURLs[1], true, PluginWorkspace.MAIN_EDITING_AREA);
    addEditorForFile(mainEditingAreaURLs[2], false, PluginWorkspace.MAIN_EDITING_AREA);
    addEditorForFile(ditamapsEditingURLs[0], true, PluginWorkspace.DITA_MAPS_EDITING_AREA);
    
    CommitAndStatusPanel commitAndStatusPanel = new CommitAndStatusPanel(gitCtrlMock);
    commitAndStatusPanel.getCommitMessageArea().setText("Commit message");
    commitAndStatusPanel.getCommitButton().doClick();
    MessageDialog warnDialog = (MessageDialog) findDialog(Tags.UNSAVED_FILES_DETECTED);
    assertNotNull(warnDialog);
    
    assertEquals("title = Unsaved_Files_Detected\n"
        + "iconPath = /images/Warning32.png\n"
        + "targetFiles = [myfile1.txt, myfile2.txt]\n"
        + "message = Cannot_Commit_Because_Unsaved_Files_Message\n"
        + "questionMessage = null\n"
        + "okButtonName = Save_All\n"
        + "cancelButtonName = null\n"
        + "showOkButton = true\n"
        + "showCancelButton = true", 
        warnDialog.toString());
    
    warnDialog.getOkButton().doClick();
    sleep(300);
    
    assertEquals("myfile1.txt\n"
        + "myfile2.txt\n", 
        savingFileLogBuilder.toString());
  }

  /**
   * Add the editor for the given file URL.
   * 
   * @param fileURL       The file URL.
   * @param isModified    <code>true</code> if the current file is modified.
   * @param editingArea   The editing area.
   */
  private void addEditorForFile(URL fileURL, boolean isModified, int editingArea) {
    WSEditor mockedFileEditor = Mockito.mock(WSEditor.class);
    Mockito.when(mockedFileEditor.getEditorLocation()).thenReturn(fileURL);
    Mockito.when(mockedFileEditor.isModified()).thenReturn(isModified);
    Mockito.when(pluginWSMock.getEditorAccess(fileURL, editingArea)).thenReturn(mockedFileEditor);
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        savingFileLogBuilder.append(new File(fileURL.toURI()).getName()).append("\n");
        return null;
      }
    }).when(mockedFileEditor).save();
  }
  
}
