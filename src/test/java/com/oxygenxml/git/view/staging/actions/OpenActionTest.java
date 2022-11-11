package com.oxygenxml.git.view.staging.actions;

import static org.junit.Assert.assertEquals;

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
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;

/**
 * Tests the open file action.
 * 
 * @author alex_smarandache
 *
 */
public class OpenActionTest {

  @Test
  public void urlFileComputingTest() throws MalformedURLException, NoRepositorySelected {
    try (final MockedStatic<GitAccess> gitAccessMock = Mockito.mockStatic(GitAccess.class)) {
      final GitAccess mockedGitAccess = Mockito.mock(GitAccess.class);
      
      final List<FileStatus> unstagedFiles = new ArrayList<>();
      unstagedFiles.add(new FileStatus(GitChangeType.CHANGED, "repo/test.txt"));
      unstagedFiles.add(new FileStatus(GitChangeType.UNTRACKED, "repo/test2.txt"));
      unstagedFiles.add(new FileStatus(GitChangeType.CHANGED, "repo/test3.txt"));
      Mockito.when(mockedGitAccess.getUnstagedFiles()).thenReturn(unstagedFiles);
      
      final List<FileStatus> stagedFiles = new ArrayList<>();
      Mockito.when(mockedGitAccess.getStagedFiles()).thenReturn(stagedFiles);
      
      gitAccessMock.when(GitAccess::getInstance).thenReturn(mockedGitAccess);
      
      assertEquals("aaa", FileStatusUtil.computeFileStatusURL(new FileStatus(GitChangeType.CHANGED, "repo/test.txt")).toExternalForm());
      
      
    } 
  }
  
}
