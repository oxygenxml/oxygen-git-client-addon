package com.oxygenxml.git.view;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.ToolbarPanel;
import com.oxygenxml.git.view.stash.StashChangesDialog;

/**
 * Toolbar panel tests.
 */
public class ToolbarPanelTest extends GitTestBase {

  private final static String LOCAL_REPO = "target/test-resources/GitAccessCheckoutNewBranch/localRepository";
  private final static String REMOTE_REPO = "target/test-resources/GitAccessCheckoutNewBranch/remoteRepository";
  private final static String LOCAL_BRANCH = "LocalBranch";

  private GitAccess gitAccess;
  private StagingPanel stagingPanel;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();

    //Creates the remote repository.
    createRepository(REMOTE_REPO);
    Repository remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(LOCAL_REPO);
    Repository localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }
  

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
    //Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    JFrame frame = new JFrame();
    try {
      // Init UI
      GitController gitCtrl = new GitController();
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(LOCAL_REPO).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshSupport.call();
      flushAWT();
      
      // Commit a change
      file = new File(LOCAL_REPO, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();
      
      // Create local change
      setFileContent(file, "new 3");
      refreshSupport.call();
      flushAWT();
      
      // Try to switch to another branch
      JComboBox<String> branchesCombo = stagingPanel.getBranchesPanel().getBranchNamesCombo();
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
    }
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
    //Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");

    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.setRepositorySynchronously(REMOTE_REPO);
    file = new File(REMOTE_REPO, "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");

    // Switch back to local repo and create local branch
    gitAccess.setRepositorySynchronously(LOCAL_REPO);
    gitAccess.createBranch(LOCAL_BRANCH);
    gitAccess.fetch();

    JFrame frame = new JFrame();
    try {
      // Init UI
      GitController gitCtrl = new GitController();
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(LOCAL_REPO).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      refreshSupport.call();
      flushAWT();
      
      // Commit a change
      file = new File(LOCAL_REPO, "local.txt");
      setFileContent(file, "new 2");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("First remote commit.");
      flushAWT();
      
      // Create local change
      setFileContent(file, "new 3");
      refreshSupport.call();
      flushAWT();
      
      // Try to switch to another branch
      JComboBox<String> branchesCombo = stagingPanel.getBranchesPanel().getBranchNamesCombo();
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
    }
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
  public void testButtonsWhenNoRepo() throws Exception {
    gitAccess.cleanUp();
    GitController gitCtrl = new GitController();
    GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
    stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
    refreshSupport.setStagingPanel(stagingPanel);
    flushAWT();
    
    ToolbarPanel toolbar = stagingPanel.getToolbarPanel();
    assertFalse(toolbar.getPullMenuButton().isEnabled());
    assertFalse(toolbar.getPushButton().isEnabled());
    assertFalse(toolbar.getStashButton().isEnabled());

    //Creates repos
    Repository remoteRepository = createRepository(REMOTE_REPO);
    Repository localRepository = createRepository(LOCAL_REPO);
    bindLocalToRemote(localRepository, remoteRepository);
    flushAWT();

    assertTrue("Pull Merge action should be enabled.", gitActionsManager.getPullMergeAction().isEnabled());
    assertTrue("Pull Rebase action should be enabled.", gitActionsManager.getPullRebaseAction().isEnabled());
    assertTrue(toolbar.getPullMenuButton().isEnabled());
    
    assertTrue("Push action should be enabled.", gitActionsManager.getPushAction().isEnabled());
    assertTrue(toolbar.getPushButton().isEnabled());
    
    assertFalse(toolbar.getStashButton().isEnabled());
  }
  
}
