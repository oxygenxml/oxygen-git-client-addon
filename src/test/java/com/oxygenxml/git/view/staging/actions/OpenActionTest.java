package com.oxygenxml.git.view.staging.actions;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Tests the open file action.
 * 
 * @author alex_smarandache
 *
 */
public class OpenActionTest {

  /**
   * <p><b>Description:</b> Tests computing URL when open a file from contextual menu in Git Staging.</p>
   * <p><b>Bug ID:</b> EXM-51808</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception If it fails.
   */
  @Test
  public void urlFileComputingTest() throws Exception {
    final File wc = new File("/src/test/resources/openactiontest");
    assertTrue(wc.mkdirs());
    try (final MockedStatic<GitAccess> gitAccessStaticMock = Mockito.mockStatic(GitAccess.class)) {
      final GitAccess mockedGitAccess = Mockito.mock(GitAccess.class);
      
      final List<FileStatus> stagedFiles = new ArrayList<>();
      final List<FileStatus> unstagedFiles = new ArrayList<>();
      unstagedFiles.add(new FileStatus(GitChangeType.CHANGED, "repo/test.txt"));
      unstagedFiles.add(new FileStatus(GitChangeType.UNTRACKED, "repo/test2.txt"));
      unstagedFiles.add(new FileStatus(GitChangeType.CHANGED, "repo/test3.txt"));
      
      Mockito.when(mockedGitAccess.getUnstagedFiles()).thenReturn(unstagedFiles);
      Mockito.when(mockedGitAccess.getStagedFiles()).thenReturn(stagedFiles);
      Mockito.when(mockedGitAccess.getWorkingCopy()).thenReturn(wc);    
      
      gitAccessStaticMock.when(GitAccess::getInstance).thenReturn(mockedGitAccess);
      
      // case 1: the file is unstaged but no staged. 
      assertTrue(FileStatusUtil.computeFileStatusURL(new FileStatus(GitChangeType.CHANGED, "repo/test.txt")).toExternalForm().startsWith("file:"));
      
      stagedFiles.add(new FileStatus(GitChangeType.CHANGED, "repo/test.txt"));
      final String expectedExceptionMessage = "unknown protocol: git";
      String actualExceptionMessage = "";
      try { // this exception is throwned because the mocks used in this test
        FileStatusUtil.computeFileStatusURL(new FileStatus(GitChangeType.CHANGED, "repo/test.txt"));
      } catch(MalformedURLException ex) {
        actualExceptionMessage = ex.getMessage();
      }
      // case 2: the file is both unstaged and staged.
      assertTrue(actualExceptionMessage.contains(expectedExceptionMessage));
      
      // case 3: the file is staged but no unstaged. 
      unstagedFiles.clear();
      assertTrue(FileStatusUtil.computeFileStatusURL(new FileStatus(GitChangeType.CHANGED, "repo/test.txt")).toExternalForm().startsWith("file:"));
    } finally {
      wc.delete();
    }
  }
  
}
