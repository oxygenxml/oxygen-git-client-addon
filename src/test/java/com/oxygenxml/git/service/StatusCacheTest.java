package com.oxygenxml.git.service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.OxygenGitPluginExtension;
import com.oxygenxml.git.utils.GitAddonSystemProperties;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Mock the required classes and check that various workspace events reset the cache.
 * 
 * @author alex_jitianu
 */
public class StatusCacheTest extends GitTestBase {
  /**
   * Path for the test repository.
   */
  private static final String REPOSITORY_PATH = "target/test-resources//StatusCacheTest";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // AVOID executing this code from OxygenGitPluginExtension:
//    if (!"true".equals(System.getProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS))) {
//      org.eclipse.jgit.transport.SshSessionFactory.setInstance(
//          new org.eclipse.jgit.transport.sshd.SshdSessionFactory(null, new ResolvingProxyDataFactory()));
//    }
    // because it fails with "Caused by: java.lang.SecurityException: class "org.eclipse.jgit.transport.JschConfigSessionFactory"'s signer information does not match signer information of other classes in the same package"
    // It might be related with the mocks being created in this class.
    System.setProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS, "true");
    createRepository(REPOSITORY_PATH);
  }
  
  @Override
  public void tearDown() throws Exception {
    System.setProperty(GitAddonSystemProperties.USE_JSCH_FOR_SSH_OPERATIONS, "false");
    
    super.tearDown();
  }

  /**
   * <p><b>Description:</b> Test that a window activated event drops the cache.</p>
   * <p><b>Bug ID:</b> EXM-49363</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testWindowActivatedEvent() throws Exception {
    OxygenGitPluginExtension extension = new OxygenGitPluginExtension();
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    JFrame mockFrame = new JFrame();
    Mockito.when(pluginWorkspace.getParentFrame()).thenReturn(mockFrame);
    
    extension.applicationStarted((StandalonePluginWorkspace) pluginWorkspace);
    
    GitStatus status = GitAccess.getInstance().getStatus();
    
    Arrays.stream(mockFrame.getWindowListeners()).forEach(l -> l.windowActivated(null));
    
    GitStatus newstatus = GitAccess.getInstance().getStatus();
    
    assertFalse("A window activated event should drop the cache", status == newstatus);
  }
  
  /**
   * <p><b>Description:</b> A git event resets the cache.</p>
   * <p><b>Bug ID:</b> EXM-49363</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testGitEvent() throws Exception {
    OxygenGitPluginExtension extension = new OxygenGitPluginExtension();
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    JFrame mockFrame = new JFrame();
    Mockito.when(pluginWorkspace.getParentFrame()).thenReturn(mockFrame);
    
    // These operations do not affect a status.
    List<GitOperation> exceptions = Arrays.asList(GitOperation.DELETE_BRANCH, GitOperation.PUSH);
    
    extension.applicationStarted((StandalonePluginWorkspace) pluginWorkspace);
    
    Arrays.stream(GitOperation.values()).forEach(op -> {
      GitStatus status = GitAccess.getInstance().getStatus();
      
      GitEventAdapter listener = new GitEventAdapter() {
        @Override
        public void operationSuccessfullyEnded(GitEventInfo info) {
          // When a normal listener receives a git event, if it requests the status it
          // should receive a newly computed status, not the stale one from cache.
          assertStatus(exceptions, op, status);
        }};
      GitListeners.getInstance().addGitListener(listener);
      GitListeners.getInstance().fireOperationSuccessfullyEnded(new GitEventInfo(op));
      GitListeners.getInstance().removeGitListener(listener);
      
      assertStatus(exceptions, op, status);
    });
  }

  /**
   * Asserts that the new status is different from the old status.
   * 
   * @param exceptions Operations that do not affect a 'git status' so it is OK for the 
   * new status to be the same with the old one.
   * @param op The operation that finished.
   * @param oldStatus
   */
  private void assertStatus(List<GitOperation> exceptions, GitOperation op, GitStatus oldStatus) {
    GitStatus newStatus = GitAccess.getInstance().getStatus();
    
    if (exceptions.contains(op)) {
      assertTrue("Event " + op + " should have drop the cache", oldStatus == newStatus);
    } else {
      assertFalse("Event " + op + " should have drop the cache", oldStatus == newStatus);
    }
  }

  /**
   * <p><b>Description:</b> Test that an editor save event drops the cache and 
   * the status is recomputed.</p>
   * <p><b>Bug ID:</b> EXM-49363</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testEditorSaveEvent() throws Exception {
    // Set up. Create required mocks.
    OxygenGitPluginExtension extension = new OxygenGitPluginExtension();
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    JFrame mockFrame = new JFrame();
    Mockito.when(pluginWorkspace.getParentFrame()).thenReturn(mockFrame);
    List<WSEditorChangeListener> listeners = new ArrayList<>();
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        listeners.add(invocation.getArgument(0));
        return null;
      }
    }).when(pluginWorkspace).addEditorChangeListener((WSEditorChangeListener) Mockito.any(), Mockito.anyInt());
    
    // Intercept save event listeners.
    URL inRepoEditorLocation = Paths.get(REPOSITORY_PATH, "a.txt").toAbsolutePath().toUri().toURL();
    URL outsideRepoEditorLocation = Paths.get(REPOSITORY_PATH, "../b.txt").normalize().toAbsolutePath().toUri().toURL();
    List<WSEditorListener> inRepoListeners = installEditorSaveMock(pluginWorkspace, inRepoEditorLocation);
    List<WSEditorListener> outsideRepoListeners = installEditorSaveMock(pluginWorkspace, outsideRepoEditorLocation);
    
    // Make Git Client install all of its listeners on the workspace.
    extension.applicationStarted((StandalonePluginWorkspace) pluginWorkspace);
    
    GitStatus status = GitAccess.getInstance().getStatus();
    
    // Simulate editor open events for both files so save listeners are added on both.
    listeners.stream().forEach(l -> l.editorOpened(outsideRepoEditorLocation));
    listeners.stream().forEach(l -> l.editorOpened(inRepoEditorLocation));
    
    // The file outside the repo is saved.
    outsideRepoListeners.stream().forEach(l -> l.editorSaved(WSEditorListener.SAVE_OPERATION));
    
    GitStatus newstatus = GitAccess.getInstance().getStatus();
    
    assertTrue("The saved file is not from the repo. The status remains the same.", status == newstatus);
    
    // The file in the repo is saved.
    inRepoListeners.stream().forEach(l -> l.editorSaved(WSEditorListener.SAVE_OPERATION));
    
    newstatus = GitAccess.getInstance().getStatus();
    
    assertFalse("The saved file is from the repo. The status is recomputed.", status == newstatus);
  }

  /**
   * Installs a editor mock for the URL and intercepts added editor listeners.
   * 
   * @param pluginWorkspace Plugin workspace.
   * @param editorLocation Editor location.
   * 
   * @return All the listeners that are added on the editor.
   */
  private List<WSEditorListener> installEditorSaveMock(PluginWorkspace pluginWorkspace, URL editorLocation) {
    List<WSEditorListener> l = new ArrayList<>(2);
    WSEditor editorMock = Mockito.mock(WSEditor.class);
    Mockito.when(pluginWorkspace.getEditorAccess(editorLocation, 0)).thenReturn(editorMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        l.add(invocation.getArgument(0));
        return null;
      }
    }).when(editorMock).addEditorListener(Mockito.any());
    Mockito.doReturn(editorLocation).when(editorMock).getEditorLocation();
    
    return l;
  }
}
