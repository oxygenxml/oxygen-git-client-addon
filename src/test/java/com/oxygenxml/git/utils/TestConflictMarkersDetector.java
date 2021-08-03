package com.oxygenxml.git.utils;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.branches.BranchesUtil;

import ro.sync.basic.util.URLUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests the com.oxygenxml.git.utils.RepoUtil.containsConflictMarkers(
 * final List<FileStatus> allSelectedResources, final File workingCopy) API
 * 
 * @author Alex_Smarandache
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginWorkspaceProvider.class})
public class TestConflictMarkersDetector {
  
  /**
   * The Plugin Workspace.
   */
  private StandalonePluginWorkspace pluginWorkspace;

  @Before
  public void initializePluginWorkspace() throws IOException {
    pluginWorkspace = Mockito.mock(StandalonePluginWorkspace.class);
    UtilAccess utilAccess = Mockito.mock(UtilAccess.class);
    Mockito.when(pluginWorkspace.getUtilAccess()).thenReturn(utilAccess);
    Mockito.when(utilAccess.createReader(Mockito.any(URL.class), Mockito.any(String.class))).then((Answer<FileReader>) invocation -> {
      File currentFile = URLUtil.getAbsoluteFileFromFileUrl(((URL)invocation.getArgument(0)));
       return new FileReader(currentFile);
    });
    PowerMockito.mockStatic(PluginWorkspaceProvider.class);
    when(PluginWorkspaceProvider.getPluginWorkspace()).thenReturn(pluginWorkspace);
  }
  
  /**
   * Tests the com.oxygenxml.git.utils.RepoUtil.containsConflictMarkers(
   * final List<FileStatus> allSelectedResources, final File workingCopy) API
   * 
   * @throws IOException
   */
  @Test
  public void testConflictMarkersDetector() throws IOException {
    
    FileStatus file1 = new FileStatus(GitChangeType.CONFLICT, "file1.txt");
    FileStatus file2 = new FileStatus(GitChangeType.CONFLICT, "file2.txt");
    FileStatus file3 = new FileStatus(GitChangeType.CONFLICT, "file3.txt");
    File workingCopy = new File("src/test/resources/EXM-47777");

    List<FileStatus> files = new ArrayList<>();

    files.add(file1);
    assertTrue(RepoUtil.containsConflictMarkers(files, workingCopy, pluginWorkspace.getUtilAccess()));
    files.add(file2);
    assertTrue(RepoUtil.containsConflictMarkers(files, workingCopy, pluginWorkspace.getUtilAccess()));
    files.add(file3);
    assertTrue(RepoUtil.containsConflictMarkers(files, workingCopy, pluginWorkspace.getUtilAccess()));
    files.remove(0);
    assertTrue(RepoUtil.containsConflictMarkers(files, workingCopy, pluginWorkspace.getUtilAccess()));
    files.remove(1);
    assertFalse(RepoUtil.containsConflictMarkers(files, workingCopy, pluginWorkspace.getUtilAccess()));
  }

}
