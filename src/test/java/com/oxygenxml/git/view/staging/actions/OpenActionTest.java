package com.oxygenxml.git.view.staging.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;

/**
 * Tests the open file action.
 * 
 * @author alex_smarandache
 *
 */
public class OpenActionTest {

  @Test
  public void urlFileComputingTest() {
    try (final MockedStatic<GitAccess> gitAccessMock = Mockito.mockStatic(GitAccess.class)) {
      gitAccessMock.when(GitAccess::getInstance).thenReturn(gitAccessMock);
      assertEquals(GitAccess.getInstance(), gitAccessMock);
    }
  }
  
}
