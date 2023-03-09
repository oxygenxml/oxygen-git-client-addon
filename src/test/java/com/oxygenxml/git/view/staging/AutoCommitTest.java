package com.oxygenxml.git.view.staging;

import java.io.File;

import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.validation.ValidationManager;
import com.oxygenxml.git.validation.internal.IPreOperationValidation;
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
    SwingUtilities.invokeLater(() -> {
      stagingPanel.getCommitPanel().getCommitMessageArea().setText("Commit message");
      stagingPanel.getCommitPanel().getCommitButton().doClick();
    });   
    waitForSchedulerBetter();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(
        () -> 1 ==  GitAccess.getInstance().getPushesAhead()
    );

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
    waitForSchedulerBetter();
    Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(
        () -> 0 ==  GitAccess.getInstance().getPushesAhead()
    );
  }


  /**
   * <p><b>Description:</b> Tests if the validation works when automatically push when committing.</p>
   * <p><b>Bug ID:</b> EXM-50785</p>
   *
   * @author alex_Smarandache
   *
   * @throws Exception
   */
  public void testAutoPushValidationWhenCommit() throws Exception {
    try {
      final String localTestRepository = "target/test-resources/testAutoPushWhenCommit_local";
      final String remoteTestRepository = "target/test-resources/testAutoPushWhenCommit_remote";

      // Enable pre-push validation 
      OptionsManager.getInstance().setValidateMainFilesBeforePush(true);
      OptionsManager.getInstance().setRejectPushOnValidationProblems(false);

      // set a custom pre-push validator to verify if is checked properly.
      final boolean flags[] = new boolean[2];
      flags[0] = flags[1] = false;

      ValidationManager.getInstance().setPrePushValidator(new IPreOperationValidation() {

        @Override
        public boolean isEnabled() {
          flags[0] |= true;
          return flags[0];
        }

        @Override
        public boolean checkValid() {
          flags[1] |= true;
          return flags[1];
        }
      });

      // Create repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo = createRepository(localTestRepository);
      bindLocalToRemote(localRepo , remoteRepo);

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
      waitForSchedulerBetter();
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
      waitForSchedulerBetter();
      flushAWT();

      assertEquals(0, GitAccess.getInstance().getPushesAhead());
      assertTrue(flags[0]);
      assertTrue(flags[1]);  
    } finally {
      OptionsManager.getInstance().setValidateMainFilesBeforePush(false);
      OptionsManager.getInstance().setRejectPushOnValidationProblems(false);
    }

  }

}
