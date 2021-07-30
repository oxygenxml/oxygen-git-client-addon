package com.oxygenxml.git.view;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.StagingPanel;

import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Toolbar panel tests.
 */
public class ToolbarPanelTest extends GitTestBase {

  private final static String LOCAL_REPO = "target/test-resources/GitAccessCheckoutNewBranch/localRepository";
  private final static String REMOTE_REPO = "target/test-resources/GitAccessCheckoutNewBranch/remoteRepository";
  private final static String LOCAL_BRANCH = "LocalBranch";

  private GitAccess gitAccess;
  private Repository remoteRepository;
  private Repository localRepository;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();

    //Creates the remote repository.
    createRepository(REMOTE_REPO);
    remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(LOCAL_REPO);
    localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository , remoteRepository);
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
      StagingPanel stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
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
      SplitMenuButton branchSplitMenuButton = stagingPanel.getToolbarPanel().getBranchSplitMenuButton();
      branchSplitMenuButton.setPopupMenuVisible(true);
      final JRadioButtonMenuItem firstItem = (JRadioButtonMenuItem) branchSplitMenuButton.getMenuComponent(0);
      
      SwingUtilities.invokeLater(() -> {
        firstItem.setSelected(true);
        firstItem.getAction().actionPerformed(null);
      });
      
      sleep(500);
      Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      
      JButton yesButton = TestUtil.findButton(focusedWindow, "Yes");
      yesButton.doClick();
      
      sleep(500);
      
      branchSplitMenuButton.setPopupMenuVisible(false);
      flushAWT();
      
      // The switch should have failed, and the selected branch shouldn't have changed
      branchSplitMenuButton.setPopupMenuVisible(true);
      flushAWT();
      JRadioButtonMenuItem firstItem2 = (JRadioButtonMenuItem) branchSplitMenuButton.getMenuComponent(0);
      assertFalse(firstItem2.isSelected());
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }


}
