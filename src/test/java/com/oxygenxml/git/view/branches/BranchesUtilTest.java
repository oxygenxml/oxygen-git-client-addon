package com.oxygenxml.git.view.branches;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;
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
   * <p><b>Description:</b> Fixes up fetch value to use wildcard. </p>
   * <p><b>Bug ID:</b> EXM-47079</p>
   *
   * @author alex_jitianu
   * 
   * @throws IOException If it fails.
   */
  @Test
  public void testFetchFixup() throws IOException {
    Optional<String> newValue = BranchesUtil.fixupFetch("+refs/heads/hot:refs/remotes/origin/hot");
    assertTrue(newValue.isPresent());
    assertEquals("+refs/heads/*:refs/remotes/origin/*", newValue.get());
    
    newValue = BranchesUtil.fixupFetch("refs/heads/hot:refs/remotes/origin/hot");
    assertTrue(newValue.isPresent());
    assertEquals("refs/heads/*:refs/remotes/origin/*", newValue.get());
    
    newValue = BranchesUtil.fixupFetch("+refs/heads/*:refs/remotes/origin/*");
    assertFalse(newValue.isPresent());
    
    final boolean[] set = new boolean[1];
    StoredConfig config = new StoredConfig() {
      @Override
      public void load() throws IOException, ConfigInvalidException {
        // Not used.
      }

      @Override
      public void save() throws IOException {
        set[0] = true;
      }
    };
    config.setString(
        ConfigConstants.CONFIG_REMOTE_SECTION, 
        Constants.DEFAULT_REMOTE_NAME, 
        ConfigConstants.CONFIG_FETCH_SECTION, 
        "+refs/heads/hot:refs/remotes/origin/hot");
    
    BranchesUtil.fixupFetchInConfig(config);
    String value = config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, Constants.DEFAULT_REMOTE_NAME, ConfigConstants.CONFIG_FETCH_SECTION);
    assertEquals("+refs/heads/*:refs/remotes/origin/*", value);
    
    
    config = new StoredConfig() {
      @Override
      public void load() throws IOException, ConfigInvalidException {
        // Not used.
      }

      @Override
      public void save() throws IOException {
        throw new IOException("Can't save!");
      }
    };
    config.setString(
        ConfigConstants.CONFIG_REMOTE_SECTION, 
        Constants.DEFAULT_REMOTE_NAME, 
        ConfigConstants.CONFIG_FETCH_SECTION, 
        "+refs/heads/hot:refs/remotes/origin/hot");
    
    IOException expected = null;
    try {
      BranchesUtil.fixupFetchInConfig(config);
    } catch (IOException ex) {
      expected = ex;
    }
    
    assertNotNull(expected);
    assertEquals("Failed to update fetch configuration to use wildcards. "
        + "Changes in the remote branch will not be visible. "
        + "Edit the .git/config file and replace +refs/heads/hot:refs/remotes/origin/hot with +refs/heads/*:refs/remotes/origin/*", 
        expected.getMessage());
  }

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
