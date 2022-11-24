package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Tests all default values for options.
 * 
 * @author alex_smarandache
 *
 */
public class DefaultOptionsValuesTest {
  
  /**
   * Test default values for JAXB Options.
   */
  @Test
  public void testJAXBDefaultOpptions() {
    final JaxbOptions options = new JaxbOptions();
    assertAllDefaultOptions(options);
  }
  
  /**
   * Test default values for Tag Based Options.
   */
  @Test
  public void testTagBasedDefaultOpptions() {
    final TagBasedOptions options = new TagBasedOptions(new WSOptionsStorageTestAdapter());
    assertAllDefaultOptions(options);
  }

  /**
   * Checks if all default options are expected.
   * 
   * @param options The options to check.
   */
  private void assertAllDefaultOptions(final Options options) {
    assertTrue(options.getCommitMessages().getMessages().isEmpty());
    assertEquals(PullType.MERGE_FF, options.getDefaultPullType());
    assertEquals("", options.getDefaultSshAgent());
    assertEquals("DestinationPaths [paths=[]]", options.getDestinationPaths().toString());
    assertEquals(false, options.getDetectAndOpenXprFiles());
    assertEquals("", options.getPassphrase());
    assertTrue(options.getPersonalAccessTokensList().getPersonalAccessTokens().isEmpty());
    assertTrue(options.getProjectsTestsForGit().getPaths().isEmpty());
    assertFalse(options.getRejectCommitOnValidationProblems());
    assertFalse(options.getRejectPushOnValidationProblems());
    assertTrue(options.getRepositoryLocations().getLocations().isEmpty());
    assertEquals("", options.getSelectedRepository());
    assertTrue(options.getSshPromptAnswers().isEmpty());
    assertEquals(ResourcesViewMode.FLAT_VIEW, options.getStagedResViewMode());
    assertEquals(ResourcesViewMode.FLAT_VIEW, options.getUnstagedResViewMode());
    assertTrue(options.getUpdateSubmodulesOnPull());
    assertTrue(options.getUserCredentialsList().getCredentials().isEmpty());
    assertTrue(options.getUseSshAgent());
    assertFalse(options.getValidateFilesBeforeCommit());
    assertFalse(options.getValidateMainFilesBeforePush());
    assertTrue(options.getWarnOnChangeCommitId().isEmpty());
    assertEquals(WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC, options.getWhenRepoDetectedInProject());
    assertFalse(options.isAutoPushWhenCommitting());
    assertTrue(options.isCheckoutNewlyCreatedLocalBranch());
    assertFalse(options.isNotifyAboutNewRemoteCommits());
  }

}
