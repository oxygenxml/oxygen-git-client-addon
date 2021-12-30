package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.SubmoduleAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.refresh.PanelRefresh;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;
import com.oxygenxml.git.view.staging.actions.DiscardAction;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

/**
 * Test cases for refreshing the Project view.
 * 
 * @author sorin_carbunaru
 */
public class RefreshProjectTest extends TestCase {
  
  PanelRefresh refreshSupport = new PanelRefresh(null) {
    @Override
    protected int getScheduleDelay() {
      // Execute refresh events immediately from tests.
      return 1;
    }
  };
  
  private File refreshedFolder;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    refreshedFolder = null;
    
    StandalonePluginWorkspace pluginWorkspace = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWorkspace.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    
    OptionsManager optMngMock = PowerMockito.mock(OptionsManager.class);
    Whitebox.setInternalState(OptionsManager.class, "instance", optMngMock);
    PowerMockito.when(optMngMock.getSelectedRepository()).thenReturn(
        new File(localTestRepoPath).getAbsolutePath());
    
    ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);
    Mockito.when(pluginWorkspace.getProjectManager()).thenReturn(projectCtrlMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        File[] filesToRefresh = (File[]) invocation.getArguments()[0];
        refreshedFolder = filesToRefresh[0];
        return null;
      }
    }).when(projectCtrlMock).refreshFolders(Mockito.any());
    
  }
  /**
   * Local repo path.
   */
  private String localTestRepoPath = "target/test-resources/testDiscard_NewFile_local/";
  
  /**
   * Refresh on discard. Only one "added" resource discarded.
   * 
   * @throws Exception
   */
  
  @Test
  public void testRefreshProjectOnDiscard_1() throws Exception {
    File repoDir = new File(localTestRepoPath);
    repoDir.mkdirs();
    
    File file = new File(localTestRepoPath, "test.txt");
    file.createNewFile();
    file.deleteOnExit();

    try {
      DiscardAction discardAction = new DiscardAction(
          new SelectedResourcesProvider() {
            @Override
            public List<FileStatus> getOnlySelectedLeaves() {
              return null;
            }
            
            @Override
            public List<FileStatus> getAllSelectedResources() {
              return Arrays.asList(new FileStatus(GitChangeType.ADD, "test.txt"));
            }
          },
          // A mock that does nothing.
          Mockito.mock(GitControllerBase.class));
      discardAction.actionPerformed(null);
      
      assertEquals(
          repoDir.getCanonicalFile().getAbsolutePath(),
          refreshedFolder.getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }
  
  /**
   * Refresh on discard. Multiple "untracked" resources discarded
   * 
   * @throws Exception
   */
  
  @Test
  public void testRefreshProjectOnDiscard_2() throws Exception {
    File repoDir = new File(localTestRepoPath);
    repoDir.mkdirs();
    
    File file = new File(localTestRepoPath, "test.txt");
    file.createNewFile();
    file.deleteOnExit();
    
    new File(localTestRepoPath + "/subFolder").mkdir();
    File file2 = new File(localTestRepoPath, "subFolder/test2.txt");
    file2.createNewFile();
    file2.deleteOnExit();

    try {
      DiscardAction discardAction = new DiscardAction(
          new SelectedResourcesProvider() {
            @Override
            public List<FileStatus> getOnlySelectedLeaves() {
              return null;
            }
            
            @Override
            public List<FileStatus> getAllSelectedResources() {
              return Arrays.asList(new FileStatus(GitChangeType.UNTRACKED, "test.txt"),
                  new FileStatus(GitChangeType.UNTRACKED, "subFolder/test2.txt"));
            }
          },
          // A mock that does nothing.
          Mockito.mock(GitControllerBase.class));
      discardAction.actionPerformed(null);

      assertEquals(
          repoDir.getCanonicalFile().getAbsolutePath(),
          refreshedFolder.getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }
  
  /**
   * Refresh on submodule discard.
   * 
   * @throws Exception
   */
  
  @PrepareForTest({ GitAccess.class})
  @Test
  public void testRefreshProjectOnDiscard_3() throws Exception {
    File repoDir = new File(localTestRepoPath);
    repoDir.mkdirs();
    
    File subModule = new File(localTestRepoPath, "subModule");
    subModule.mkdir();

    try {
      GitAccess gitAccessMock = PowerMockito.mock(GitAccess.class);
      Whitebox.setInternalState(GitAccess.class, "instance", gitAccessMock);
      
      SubmoduleAccess submoduleAccess = Mockito.mock(SubmoduleAccess.class);
      Mockito.doNothing().when(submoduleAccess).discardSubmodule();
      
      PowerMockito.when(gitAccessMock.getSubmoduleAccess()).thenReturn(submoduleAccess);
      
      DiscardAction discardAction = new DiscardAction(
          new SelectedResourcesProvider() {
            @Override
            public List<FileStatus> getOnlySelectedLeaves() {
              return null;
            }
            
            @Override
            public List<FileStatus> getAllSelectedResources() {
              return Arrays.asList(new FileStatus(GitChangeType.SUBMODULE, "subModule"));
            }
          },
          // A mock that does nothing.
          Mockito.mock(GitControllerBase.class));
      discardAction.actionPerformed(null);

      assertEquals(
          subModule.getCanonicalFile().getAbsolutePath(),
          refreshedFolder.getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }

}
