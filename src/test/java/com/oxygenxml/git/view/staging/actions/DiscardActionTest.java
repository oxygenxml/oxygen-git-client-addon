package com.oxygenxml.git.view.staging.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;

import de.schlichtherle.io.File;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

public class DiscardActionTest extends GitTestBase{
  /**
   * The repository where the tests take place.
   */
  private File repo = new File("EXM-50739");
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    repo.mkdirs();
  }
  
  @Override
  public void tearDown() throws Exception {
    repo.delete();
    
    super.tearDown();
  }
  
  
  /**
   * <p><b>Description:</b> Refresh folders in Project view after the discard operation.</p>
   * <p><b>Bug ID:</b> EXM-50739</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testNotifyProject() throws Exception {
    repo.mkdirs();
    OptionsManager.getInstance().saveSelectedRepository(repo.getAbsolutePath());
    
    StandalonePluginWorkspace mock = Mockito.mock(StandalonePluginWorkspace.class);
    StringBuffer b = new StringBuffer();
    Semaphore s = new Semaphore(0);
    
    ProjectController projectController = Mockito.mock(ProjectController.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        java.io.File[] argument = invocation.getArgument(0);
        b.append("Refresh: " + Arrays.asList(argument).toString()).append(" on thread " + Thread.currentThread().getName());
        s.release();
        return null;
      }
    }).when(projectController).refreshFolders(Mockito.any());
    Mockito.when(mock.getProjectManager()).thenReturn(projectController);
    
    Mockito.when(mock.showConfirmDialog(
        translator.getTranslation(Tags.DISCARD),
        translator.getTranslation(Tags.DISCARD_CONFIRMATION_MESSAGE),
        new String[] { 
            "   " + translator.getTranslation(Tags.YES) + "   ",
            "   " + translator.getTranslation(Tags.NO) + "   "}, 
        new int[] { 0, 1 })).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(mock);
    
    
    GitAccess gitAccess = Mockito.mock(GitAccess.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        b.append("Discard: ").append(invocation.getArgument(0).toString()).append("\n");
        s.release();
        return null;
      }
    }).when(gitAccess).restoreLastCommitFile(Mockito.anyList());
    
    GitController controller = new GitController(gitAccess);
    final List<FileStatus> asList = new ArrayList<>();
    
    DiscardAction action = new DiscardAction(new SelectedResourcesProvider() {
      @Override
      public List<FileStatus> getOnlySelectedLeaves() {
        return null;
      }
      @Override
      public List<FileStatus> getAllSelectedResources() {
        return asList;
        
      }
    }, controller);
    
    asList.add(new FileStatus(GitChangeType.REMOVED, "test.txt"));
    action.actionPerformed(null);
    s.acquire(2);
    String expected = "Discard: [test.txt]\n"
        + "Refresh: [" + repo.getAbsolutePath()
        + "] on thread AWT-EventQueue-0";
    assertEquals(expected, b.toString());
    
    asList.clear();
    b.setLength(0);
    asList.add(new FileStatus(GitChangeType.MISSING, "test.txt"));
    action.actionPerformed(null);
    s.acquire(2);
    assertEquals(expected, b.toString());
    
    assertTrue(new File(repo, "test.txt").createNewFile());
    asList.clear();
    b.setLength(0);
    asList.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    action.actionPerformed(null);
    s.acquire(2);
    assertEquals(expected, b.toString());
    
    assertTrue(new File(repo, "test.txt").createNewFile());
    asList.clear();
    b.setLength(0);
    asList.add(new FileStatus(GitChangeType.UNTRACKED, "test.txt"));
    action.actionPerformed(null);
    s.acquire(2);
    assertEquals(expected, b.toString());
  }
}
