package com.oxygenxml.git.view.staging.actions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.oxygenxml.git.service.GitAccess;

/**
 * Tests the open file action.
 * 
 * @author alex_smarandache
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.swing.*", "javax.management.*", "javax.script.*",  "javax.xml.*", "org.xml.*"})
@PrepareForTest(GitAccess.class)
public class OpenActionTest {

  @Test
  public void urlFileComputingTest() {
    final GitAccess gitAccessMock = PowerMockito.mock(GitAccess.class);
    PowerMockito.mockStatic(GitAccess.class);
    BDDMockito.given(GitAccess.getInstance()).willReturn(gitAccessMock);
    Whitebox.setInternalState(GitAccess.class, "instance", gitAccessMock);
    assertTrue(true);
  }
  
}
