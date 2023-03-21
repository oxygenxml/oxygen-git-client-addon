package com.oxygenxml.git.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.history.HistoryStrategy;
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
    int counter = 0;
    assertTrue(options.getCommitMessages().getMessages().isEmpty());
    counter++;
    assertEquals(PullType.MERGE_FF, options.getDefaultPullType());
    counter++;
    assertEquals("", options.getDefaultSshAgent());
    counter++;
    assertEquals("DestinationPaths [paths=[]]", options.getDestinationPaths().toString());
    counter++;
    assertTrue(options.getDetectAndOpenXprFiles());
    counter++;
    assertEquals("", options.getPassphrase());
    counter++;
    assertTrue(options.getPersonalAccessTokensList().getPersonalAccessTokens().isEmpty());
    counter++;
    assertTrue(options.getProjectsTestsForGit().getPaths().isEmpty());
    counter++;
    assertFalse(options.getRejectCommitOnValidationProblems());
    counter++;
    assertFalse(options.getRejectPushOnValidationProblems());
    counter++;
    assertTrue(options.getRepositoryLocations().getLocations().isEmpty());
    counter++;
    assertEquals("", options.getSelectedRepository());
    counter++;
    assertTrue(options.getSshPromptAnswers().isEmpty());
    counter++;
    assertEquals(ResourcesViewMode.FLAT_VIEW, options.getStagedResViewMode());
    counter++;
    assertEquals(ResourcesViewMode.FLAT_VIEW, options.getUnstagedResViewMode());
    counter++;
    assertTrue(options.getUpdateSubmodulesOnPull());
    counter++;
    assertTrue(options.getUserCredentialsList().getCredentials().isEmpty());
    counter++;
    assertTrue(options.getUseSshAgent());
    counter++;
    assertFalse(options.getValidateFilesBeforeCommit());
    counter++;
    assertFalse(options.getValidateMainFilesBeforePush());
    counter++;
    assertTrue(options.getWarnOnChangeCommitId().isEmpty());
    counter++;
    assertEquals(WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC, options.getWhenRepoDetectedInProject());
    counter++;
    assertEquals(HistoryStrategy.ALL_BRANCHES, options.getHistoryStrategy());
    counter++;
    assertFalse(options.isAutoPushWhenCommitting());
    counter++;
    assertTrue(options.isCheckoutNewlyCreatedLocalBranch());
    counter++;
    assertFalse(options.isNotifyAboutNewRemoteCommits());
    counter++;
    assertTrue(options.getStashIncludeUntracked());
    counter++;
    assertTrue(options.getCreateBranchWhenCheckoutCommit());
    counter++;
    assertFalse(options.getAskUserToCreateNewRepoIfNotExist());
    counter++;
    assertEquals("", options.getCurrentBranch());
    counter++;
    assertEquals("Probably a new option has been added, test its default value in this test and increment the counter.",
        OptionTags.class.getFields().length, counter);
  }

}
