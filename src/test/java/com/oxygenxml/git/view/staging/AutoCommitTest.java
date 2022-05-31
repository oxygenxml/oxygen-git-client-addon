package com.oxygenxml.git.view.staging;

import java.io.File;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class AutoCommitTest extends FlatViewTestBase {
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  /**
   * <p><b>Description:</b> Automatically push when committing.</p>
   * <p><b>Bug ID:</b> EXM-44915</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testAutoPushWhenCommit() throws Exception {
    String localTestRepository = "target/test-resources/testAutoPushWhenCommit_local";
    String remoteTestRepository = "target/test-resources/testAutoPushWhenCommit_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    sleep(500);
    
    pushOneFileToRemote(localTestRepository, "test_second_local.txt", "hellllo");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    // Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    flushAWT();
    
    // No auto push
    JideToggleButton autoPushBtn = stagingPanel.getCommitPanel().getAutoPushWhenCommittingToggle();
    assertFalse(autoPushBtn.isSelected());
    
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    stagingPanel.getCommitPanel().getCommitMessageArea().setText("Commit message");
    stagingPanel.getCommitPanel().getCommitButton().doClick();
    waitForScheluerBetter();
    flushAWT();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    // Change the file again.
    setFileContent(file, "modified again");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> autoPushBtn.setSelected(true));
    flushAWT();
    assertTrue(autoPushBtn.isSelected());
    
    SwingUtilities.invokeLater(() -> {
      stagingPanel.getCommitPanel().getCommitMessageArea().setText("Another commit message");
      stagingPanel.getCommitPanel().getCommitButton().doClick();
      });
    waitForScheluerBetter();
    flushAWT();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
  }

}
