package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.oxygenxml.git.ProjectViewManager;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitCommandState;
import com.oxygenxml.git.view.event.StageController;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases for refreshing the Project view.
 * 
 * @author sorin_carbunaru
 */
@RunWith(PowerMockRunner.class)
public class RefreshProjectTest extends TestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
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
  
  @PrepareForTest({ ProjectViewManager.class})
  @Test
  public void testRefreshProjectOnDiscard_1() throws Exception {
    File repoDir = new File(localTestRepoPath);
    repoDir.mkdirs();
    
    File file = new File(localTestRepoPath, "test.txt");
    file.createNewFile();
    file.deleteOnExit();

    try {
      PowerMockito.mockStatic(ProjectViewManager.class);

      ArgumentCaptor<File[]> captor = ArgumentCaptor.forClass(File[].class);
      PowerMockito.doNothing().when(
          ProjectViewManager.class, "refreshFolders", (Object[])captor.capture());

      DiscardAction discardAction = new DiscardAction(
          Arrays.asList(new FileStatus(GitChangeType.ADD, "test.txt")),
          new StageController() {
            @Override
            public void doGitCommand(List<FileStatus> filesStatus, GitCommandState action) {
              // Do nothing
            }
          });
      discardAction.actionPerformed(null);

      File[] value = captor.getValue();
      assertNotNull(value);
      assertEquals(1, value.length);
      assertEquals(
          repoDir.getCanonicalFile().getAbsolutePath(),
          value[0].getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }
  
  /**
   * Refresh on discard. Multiple "untracked" resources discarded
   * 
   * @throws Exception
   */
  
  @PrepareForTest({ ProjectViewManager.class})
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
      PowerMockito.mockStatic(ProjectViewManager.class);

      ArgumentCaptor<File[]> captor = ArgumentCaptor.forClass(File[].class);
      PowerMockito.doNothing().when(
          ProjectViewManager.class, "refreshFolders", (Object[])captor.capture());

      DiscardAction discardAction = new DiscardAction(
          Arrays.asList(new FileStatus(GitChangeType.UNTRACKED, "test.txt"),
              new FileStatus(GitChangeType.UNTRACKED, "subFolder/test2.txt")),
          new StageController() {
            @Override
            public void doGitCommand(List<FileStatus> filesStatus, GitCommandState action) {
              // Do nothing
            }
          });
      discardAction.actionPerformed(null);

      File[] value = captor.getValue();
      assertNotNull(value);
      assertEquals(1, value.length);
      assertEquals(
          repoDir.getCanonicalFile().getAbsolutePath(),
          value[0].getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }
  
  /**
   * Refresh on submodule discard.
   * 
   * @throws Exception
   */
  
  @PrepareForTest({ ProjectViewManager.class, GitAccess.class})
  @Test
  public void testRefreshProjectOnDiscard_3() throws Exception {
    File repoDir = new File(localTestRepoPath);
    repoDir.mkdirs();
    
    File subModule = new File(localTestRepoPath, "subModule");
    subModule.mkdir();

    try {
      PowerMockito.mockStatic(ProjectViewManager.class);
      ArgumentCaptor<File[]> captor = ArgumentCaptor.forClass(File[].class);
      PowerMockito.doNothing().when(
          ProjectViewManager.class, "refreshFolders", (Object[])captor.capture());
      
      GitAccess gitAccessMock = PowerMockito.mock(GitAccess.class);
      Whitebox.setInternalState(GitAccess.class, "instance", gitAccessMock);
      PowerMockito.doNothing().when(gitAccessMock).discardSubmodule();
      
      DiscardAction discardAction = new DiscardAction(
          Arrays.asList(new FileStatus(GitChangeType.SUBMODULE, "subModule")),
          new StageController() {
            @Override
            public void doGitCommand(List<FileStatus> filesStatus, GitCommandState action) {
              // Do nothing
            }
          });
      discardAction.actionPerformed(null);

      File[] value = captor.getValue();
      assertNotNull(value);
      assertEquals(1, value.length);
      assertEquals(
          subModule.getCanonicalFile().getAbsolutePath(),
          value[0].getAbsolutePath());
    } finally {
      FileUtils.deleteDirectory(repoDir);
    }
  }

}
