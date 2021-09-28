package com.oxygenxml.git.view;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.BranchesPanel;
import com.oxygenxml.git.view.staging.StagingPanel;
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
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(LOCAL_REPO).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      stagingPanel.getToolbarPanel().refresh();
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
      BranchesPanel branchesPanel = stagingPanel.getBranchesPanel();
      branchesPanel.refresh();
      sleep(300);
      
      JComboBox<String> branchesCombo = branchesPanel.getBranchNamesCombo();
      assertEquals("main", branchesCombo.getSelectedItem());
      
      branchesCombo.showPopup();
      
      SwingUtilities.invokeLater(() -> {
        branchesCombo.setSelectedItem("LocalBranch");
      });
      
      JDialog focusedWindow = TestUtil.waitForDialog(translator.getTranslation(Tags.SWITCH_BRANCH), this);
      
      JButton yesButton = TestUtil.findButton(focusedWindow, translator.getTranslation(Tags.MOVE_CHANGES));
      yesButton.doClick();
      
      flushAWT();
      
      branchesCombo.hidePopup();
      flushAWT();
      
      // The switch should have failed, and the selected branch shouldn't have changed
      branchesCombo.showPopup();
      flushAWT();
      
      assertEquals("main", branchesCombo.getSelectedItem());
      
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
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      JComboBox<String> wcCombo = stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo();
      String wcPath = new File(LOCAL_REPO).getAbsolutePath();
      wcCombo.addItem(wcPath);
      wcCombo.setSelectedItem(wcPath);
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      stagingPanel.getToolbarPanel().refresh();
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
      BranchesPanel branchesPanel = stagingPanel.getBranchesPanel();
      branchesPanel.refresh();
      sleep(300);
      
      JComboBox<String> branchesCombo = branchesPanel.getBranchNamesCombo();
      assertEquals("main", branchesCombo.getSelectedItem());
      
      branchesCombo.showPopup();
      
      SwingUtilities.invokeLater(() -> {
        branchesCombo.setSelectedItem("LocalBranch");
      });
      flushAWT();
      
      JDialog focusedWindow = TestUtil.waitForDialog(translator.getTranslation(Tags.SWITCH_BRANCH), this);
      
      JButton stashButton = TestUtil.findButton(focusedWindow, Tags.STASH_CHANGES);
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
  
  
}
