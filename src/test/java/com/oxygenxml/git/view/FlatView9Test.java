package com.oxygenxml.git.view;

import java.io.File;

import javax.swing.SwingUtilities;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases.
 */
public class FlatView9Test extends FlatViewTestBase {
  
  
  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger logger = LogManager.getLogger(FlatView9Test.class.getName());

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was pushed. Change file content.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasPushed_changeFileContent() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testAmendCommitThatWasPushed_1_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasPushed_1_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // >>> Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    // >>> Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    // >>> Push
    GitAccess.getInstance().push("", "");
    waitForScheluerBetter();
    refreshSupport.call();
    waitForScheluerBetter();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    
    // Change the file again.
    setFileContent(file, "modified");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("SECOND COMMIT MESSAGE");
      });
    flushAWT();
    
    PluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    assertTrue(commitPanel.getCommitButton().isEnabled());
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    // The amend was cancelled. We must not see the first commit message.
    assertEquals("SECOND COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    // Still enabled because we have a staged file
    assertTrue(commitPanel.getCommitButton().isEnabled());
    assertFalse(amendBtn.isSelected());
  }

}
