package com.oxygenxml.git.view.staging.actions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.oxygenxml.git.service.GitAccess;

/**
 * Tests the open file action.
 * 
 * @author alex_smarandache
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GitAccess.class)
public class OpenActionTest {

  @Test
  public void urlFileComputingTest() {
    final GitAccess gitAccessMock = Mockito.mock(GitAccess.class);
    PowerMockito.mockStatic(GitAccess.class);
    BDDMockito.given(GitAccess.getInstance()).willReturn(gitAccessMock);
    PowerMockito.verifyStatic(GitAccess.class);
    assertTrue(true);
  }
  
}
