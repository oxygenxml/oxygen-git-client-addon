package com.oxygenxml.git.view.branches;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.TestCase;

/**
 * Test cases.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BranchesUtil.class})
public class BranchesUtilTest extends TestCase {

  /**
   * <p><b>Description:</b> check if branch already exists.</p>
   * <p><b>Bug ID:</b> EXM-47204</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testBranchAlreadyExists() throws Exception {
    Method getLocalBranchesMethod = PowerMockito.method(BranchesUtil.class, "getLocalBranches");
    PowerMockito.stub(getLocalBranchesMethod).toReturn(Arrays.asList("my_branch"));
    
    assertTrue(BranchesUtil.doesBranchAlreadyExist("my_branch"));
    assertTrue(BranchesUtil.doesBranchAlreadyExist("my_Branch"));
    assertFalse(BranchesUtil.doesBranchAlreadyExist("my_branch_2"));
  }

}
