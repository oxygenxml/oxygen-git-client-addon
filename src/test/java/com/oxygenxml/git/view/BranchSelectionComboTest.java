package com.oxygenxml.git.view;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.OxygenGitPluginExtension;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.refresh.PanelRefresh;
import com.oxygenxml.git.view.staging.BranchSelectionCombo;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.stash.StashChangesDialog;

import ro.sync.basic.io.FileSystemUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Contains tests for branch combo selector.
 * 
 * @author alex_smarandache
 */
public class BranchSelectionComboTest extends GitTestBase {

  /**
   * The git access unique instance.
   */
  private final GitAccess gitAccess = GitAccess.getInstance();
  
  /**
   * The staging panel.
   */
  private StagingPanel stagingPanel;
  

  /**
   * <p><b>Description:</b> when trying to switch to another branch from the branches menu
   * and the checkout fails, keep the previous branch selected.</p>
   * <p><b>Bug ID:</b> EXM-46826</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testKeepCurrentBranchSelectedWhenSwitchFails() throws Exception {

    File testDir = new File(String.format("target/test-resources/ToolbarPanelTest/%s", this.getName()));
    String local = String.format("target/test-resources/ToolbarPanelTest/%s/localRepository", this.getName());
    String remote = String.format("target/test-resources/ToolbarPanelTest/%s/remoteRepository", this.getName());
    String LOCAL_BRANCH = "LocalBranch";

    //Creates the remote repository.
    createRepo(remote, local);

    //Make the first commit for the local repository
    File file = new File(local, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(remote);
    file = new File(remote, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(local);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    JFrame frame = new JFrame();
    try {
      // Init UI
      GitController gitCtrl = new GitController();
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(local).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshSupport.call();
      flushAWT();

      // Commit a change
      file = new File(local, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();

      // Create local change
      setFileContent(file, "new 3");
      refreshSupport.call();
      flushAWT();

      // Try to switch to another branch
      BranchSelectionCombo branchesCombo = stagingPanel.getBranchesCombo();
      branchesCombo.refresh();
      String currentBranch = (String) branchesCombo.getSelectedItem();
      assertEquals("main", currentBranch);

      // select the "Local Branch" (aka the broken one)
      branchesCombo.setSelectedIndex(0);

      // wait for swith dialog to appear  
      JDialog switchBranchDialog = TestUtil.waitForDialog(translator.getTranslation(Tags.SWITCH_BRANCH), this);
      JButton yesButton = TestUtil.findButton(switchBranchDialog, translator.getTranslation(Tags.MOVE_CHANGES));
      yesButton.doClick();
      flushAWT();

      String currentBranchAfterSwitchFailed = (String) branchesCombo.getSelectedItem();
      assertEquals("main", currentBranchAfterSwitchFailed);
    } finally {
      frame.setVisible(false);
      frame.dispose();
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }

  /**
   * <p><b>Description:</b> tests if the branches combo can change the current branch.</p>
   * <p><b>Bug ID:</b> EXM-49328</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testCanSwitchBranches() throws Exception {

    final File testDir = new File(
        String.format("target/test-resources/ToolbarPanelTest/%s", 
        this.getName()));
    final String local = String.format("target/test-resources/ToolbarPanelTest/%s/localRepository", 
        this.getName());
    final String remote = String.format("target/test-resources/ToolbarPanelTest/%s/remoteRepository",
        this.getName());
    final String LOCAL_BRANCH = "LocalBranch";

    //Creates the remote repository.
    createRepo(remote, local);

    //Make the first commit for the local repository
    File file = new File(local, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(remote);
    file = new File(remote, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(local);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    final JFrame frame = new JFrame();
    try {
      // Init UI
      final GitController gitCtrl = new GitController();
      final GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      final JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      final String wcPath = new File(local).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshSupport.call();
      flushAWT();

      // Commit a change
      file = new File(local, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();

      // Try to switch to another branch
      final BranchSelectionCombo branchesCombo = stagingPanel.getBranchesCombo();
      branchesCombo.refresh();
      String currentBranch = (String) branchesCombo.getSelectedItem();
      assertEquals("main", currentBranch);

      // select the "Local Branch" (aka the broken one)
      branchesCombo.setSelectedIndex(0);

      final String currentBranchAfterSwitchFailed = (String) branchesCombo.getSelectedItem();
      assertEquals(LOCAL_BRANCH, currentBranchAfterSwitchFailed);
    } finally {
      frame.setVisible(false);
      frame.dispose();
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }

  /**
   * <p><b>Description:</b> if the branch is changed from an external git client, 
   * on windows returns this branch should be updated.</p>
   * <p><b>Bug ID:</b> EXM-49947</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  @SuppressWarnings("serial")
  public void testRefreshAfterExternalBranchChanges() throws Exception {

    final File testDir = new File(String.format(
        "target/test-resources/ToolbarPanelTest/%s", 
        this.getName()));
    final String local = String.format(
        "target/test-resources/ToolbarPanelTest/%s/localRepository", 
        this.getName());
    final String remote = String.format(
        "target/test-resources/ToolbarPanelTest/%s/remoteRepository", 
        this.getName());
    final String LOCAL_BRANCH = "LocalBranch";

    final PanelRefresh refreshManager = new PanelRefresh(null) {
      protected int getScheduleDelay() { 
        return 1; 
       }
    };

    //Creates the remote repository.
    createRepo(remote, local);

    //Make the first commit for the local repository
    File file = new File(local, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(remote);
    file = new File(remote, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(local);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    final JFrame frame = new JFrame();
    try {
      Mockito.when(PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()).then((Answer<Object>) 
          invocation -> {
            return frame;
          }); 

      final OxygenGitPluginExtension pluginExtension = new OxygenGitPluginExtension();
      final GitController gitCtrl = new GitController();
      final GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshManager) {
        public void refreshActionsStates() {
          // nothing
        }
      };
      final StagingPanel localStagingPanel = new StagingPanel(refreshManager, gitCtrl, 
          Mockito.mock(HistoryController.class), 
          gitActionsManager) {
        @Override
        public boolean isShowing() {
          return true;
        }
      };
      pluginExtension.applicationStarted((StandalonePluginWorkspace)
          PluginWorkspaceProvider.getPluginWorkspace());
      pluginExtension.setGitRefreshSupport(refreshManager);
      pluginExtension.setStagingPanel(localStagingPanel);
      refreshManager.setStagingPanel(localStagingPanel);

      final JComboBox<String> wcCombo = localStagingPanel.getWorkingCopySelectionPanel()
          .getWorkingCopyCombo();
      final String wcPath = new File(local).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(localStagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshManager.call();
      flushAWT();

      // Commit a change
      file = new File(local, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();

      // Try to switch to another branch
      final BranchSelectionCombo branchesCombo = localStagingPanel.getBranchesCombo();
      branchesCombo.refresh();
      final String currentBranch = (String) branchesCombo.getSelectedItem();
      assertEquals("main", currentBranch);

      // disable the window and change branch.
      frame.setVisible(false);
      GitAccess.getInstance().getGit().checkout().setName(LOCAL_BRANCH).call();
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
        LOCAL_BRANCH.equals( GitAccess.getInstance().getBranchInfo().getBranchName()));
      assertEquals("main", branchesCombo.getSelectedItem());
     
      // return to the window and test if the branch was updated
      frame.setVisible(true);
      Awaitility.await().atMost(Duration.TWO_SECONDS).until(() -> 
        LOCAL_BRANCH.equals( branchesCombo.getSelectedItem()));
    } finally {
      frame.setVisible(false);
      frame.dispose();
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }

  /**
   * This method is used to create a repository.
   * 
   * @param remote The remote path.
   * @param local  The local path.
   * 
   * @throws Exception When problems occur.
   */
  private void createRepo(String remote, String local) throws Exception {
    createRepository(remote);
    Repository remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(local);
    Repository localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }

  /**
   * <p><b>Description:</b> when trying to switch to another branch from the branches menu
   * and the checkout fails, tests the dialog</p>
   * <p><b>Bug ID:</b> EXM-48502</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testCanStashChangesIfSwitchBranchFailsBecauseUncommitedFiles() throws Exception {

    File testDir = new File(String.format("target/test-resources/ToolbarPanelTest/%s", this.getName()));
    String local = String.format("target/test-resources/ToolbarPanelTest/%s/localRepository", this.getName());
    String remote = String.format("target/test-resources/ToolbarPanelTest/%s/remoteRepository", this.getName());
    String LOCAL_BRANCH = "LocalBranch";

    //Creates the remote repository.
    createRepo(remote, local);

    //Make the first commit for the local repository
    File file = new File(local, "local.txt");
    file.createNewFile();

    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(remote);
    file = new File(remote, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(local);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    JFrame frame = new JFrame();
    try {
      // Init UI
      GitController gitCtrl = new GitController();
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(local).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshSupport.call();
      flushAWT();

      // Commit a change
      file = new File(local, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();

      // Create local change
      setFileContent(file, "new 3");
      refreshSupport.call();
      flushAWT();

      // Try to switch to another branch
      BranchSelectionCombo branchesCombo = stagingPanel.getBranchesCombo();
      branchesCombo.refresh();
      flushAWT();
      String currentBranch = (String) branchesCombo.getSelectedItem();
      assertEquals("main", currentBranch);

      // select other branch
      branchesCombo.setSelectedIndex(0);

      JDialog switchBranchDialog = TestUtil.waitForDialog(translator.getTranslation(Tags.SWITCH_BRANCH), this);
      JButton stashButton = TestUtil.findButton(switchBranchDialog, Tags.STASH_CHANGES);
      stashButton.doClick();
      flushAWT();
      StashChangesDialog stashChangesDialog = (StashChangesDialog) findDialog(Tags.STASH_CHANGES);
      assertNotNull(stashChangesDialog);

      stashChangesDialog.setVisible(false);
      stashChangesDialog.dispose();

    } finally {
      frame.setVisible(false);
      frame.dispose();
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }

}
