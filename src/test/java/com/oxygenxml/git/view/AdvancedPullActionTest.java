package com.oxygenxml.git.view;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.internal.PullConfig;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.dialog.AdvancedPullDialog;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.remotes.CurrentBranchRemotesDialog;
import com.oxygenxml.git.view.remotes.RemoteBranchItem;
import com.oxygenxml.git.view.staging.StagingPanel;

/**
 * A class that tests the advanced pull action functionality in a Git repository.
 * 
 * <p>
 * This class extends {@link GitTestBase} and contains tests for making pull requests
 * from custom branches, as well as verifying the configuration of the remote repository.
 * </p>
 * 
 * <p>
 * The class uses a local Git repository and interacts with multiple remote repositories 
 * to simulate real-world scenarios.
 * </p>
 * 
 * @see GitTestBase
 */
public class AdvancedPullActionTest extends GitTestBase {

  private static final String LOCAL_REPO = "target/test-resources/GitAccessRemote/localRepository";
  private static final String REMOTE_REPO = "target/test-resources/GitAccessRemote/remoteRepository";
  private static final String REMOTE_REPO2 = "target/test-resources/GitAccessRemote/remoteRepository2";
  private static final String LOCAL_BRANCH = "LocalBranch";
  private final GitAccess gitAccess = GitAccess.getInstance();
  private StagingPanel stagingPanel;
  private Repository localRepository;

  /**
   * Set up the test environment.
   * 
   * <p>
   * Creates a local and remote repository, then binds the local repository to the 
   * remote repository.
   * </p>
   * 
   * @throws Exception if any error occurs during setup
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Create the remote repository
    createRepository(REMOTE_REPO);
    Repository remoteRepository = gitAccess.getRepository();

    // Create the local repository
    createRepository(LOCAL_REPO);
    localRepository = gitAccess.getRepository();

    // Bind the local repository to the remote repository
    bindLocalToRemote(localRepository, remoteRepository);
  }

  /**
   * <p><b>Description:</b> Tests making a pull request from a custom branch in the remote repository. 
   * <br>
   * Verifies if the pull configuration can be set to update from a given branch and checks if the correct remote repository is selected. 
   * <br>
   * It also ensures that the Git configuration remains unchanged after the operation.</p>
   * <p><b>Bug ID:</b> EXM-46209</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception if any error occurs during the test
   */ 
  public void testMakePullFromCustomBranch() throws Exception {

    final String remoteRepo2Branch = "branch_remote_repo_2";
    final String remote2 = "remote2_name";

    // Create the second remote repository and add a branch to it
    createRepository(REMOTE_REPO2);
    Repository remoteRepository = gitAccess.getRepository();
    addRemote(localRepository, remoteRepository, remote2);
    gitAccess.createBranch(remoteRepo2Branch);

    // Add content to the second remote repository and commit
    File file = new File(REMOTE_REPO2, "remote2.txt");
    file.createNewFile();
    setFileContent(file, "remote2content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote2.txt"));
    gitAccess.commit("First remote2 commit.");

    // Make the first commit for the local repository
    file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    // Make the first commit for the original remote repository
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to the local repository and create a local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    JFrame frame = new JFrame();

    PullConfig[] pullConfiguration = new PullConfig[1];

    try {
      // Initialize GitController and related UI components
      GitController gitCtrl = new GitController() {
        @Override
        public Future<?> pull(PullConfig pullConfig, Optional<IGitViewProgressMonitor> progressMonitor) {
          pullConfiguration[0] = pullConfig;
          return super.pull(pullConfig, progressMonitor);
        }
      };
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      refreshSupport.call();
      flushAWT();

      // Trigger advanced pull action
      SwingUtilities.invokeLater(() -> gitActionsManager.getAdvancedPullAction().actionPerformed(null));

      flushAWT();

      // Interact with the Advanced Pull dialog
      AdvancedPullDialog advancedPullDialog = (AdvancedPullDialog) findDialog(Tags.PULL);
      assertNotNull(advancedPullDialog);
      flushAWT();

      // Verify that the correct remote repositories are listed
      JComboBox<com.oxygenxml.git.view.remotes.RemoteBranchItem> remoteBranches = advancedPullDialog.getRemoteBranchItems();
      assertNotNull(remoteBranches);
      assertEquals(2, remoteBranches.getItemCount());
      assertEquals("origin", gitAccess.getRemoteFromCurrentBranch());

      // Select the second remote repository
      RemoteBranchItem currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("origin/main", currentSelected.toString());

      remoteBranches.setSelectedIndex(1);
      currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("remote2_name/main", currentSelected.toString());

      flushAWT();
      sleep(500);

      // Confirm the selection and pull
      SwingUtilities.invokeLater(() -> advancedPullDialog.getOkButton().doClick());

      sleep(200);
      Awaitility.await().atMost(Duration.TWO_HUNDRED_MILLISECONDS).untilAsserted(() -> assertNotNull(pullConfiguration[0]));
      assertEquals("refs/heads/main", pullConfiguration[0].getBranchName().orElse(null));
      assertEquals("remote2_name", pullConfiguration[0].getRemote().orElse(null));
      assertEquals(PullType.MERGE_FF, pullConfiguration[0].getPullType());

      // Ensure that the configuration file is unchanged after user confirmation
      SwingUtilities.invokeLater(() -> gitActionsManager.getTrackRemoteBranchAction().actionPerformed(null));
      flushAWT();
      CurrentBranchRemotesDialog trackRemotesDialog = (CurrentBranchRemotesDialog) findDialog(Tags.CONFIGURE_REMOTE_FOR_BRANCH);
      assertNotNull(trackRemotesDialog);
      flushAWT();

      remoteBranches = trackRemotesDialog.getRemoteBranchItems();
      assertNotNull(remoteBranches);
      assertEquals("origin", gitAccess.getRemoteFromCurrentBranch());
      currentSelected = (RemoteBranchItem) remoteBranches.getSelectedItem();
      assertEquals("origin/main", currentSelected.toString());

    } finally {
      // Cleanup
      frame.setVisible(false);
      frame.dispose();
    }
  }
}
