package com.oxygenxml.git.view;

import java.awt.Component;

import javax.swing.JPopupMenu.Separator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.standalone.ui.SplitMenuButton;

/**
 * Test cases.
 */
public class FlatView7Test extends FlatViewTestBase {
  
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  /**
   * <p><b>Description:</b> Test the tooltips of the pull/push buttons and branch label.</p>
   * <p><b>Bug ID:</b> EXM-45599</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testToolbarComponentsTooltips() throws Exception {
    // Set toolbar panel
    stagingPanel.setToolbarPanelFromTests(
        new ToolbarPanel(stagingPanel.getPushPullController(), refreshSupport, null, null));
    
    // Create repositories
    String localTestRepository = "target/test-resources/test_EXM_45599_local";
    String localTestRepository_2 = "target/test-resources/test_EXM_45599_local_2";
    String remoteTestRepository = "target/test-resources/test_EXM_45599_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_2 , remoteRepo);
    sleep(500);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create local branch
    Git git = GitAccess.getInstance().getGit();
    git.branchCreate().setName("new_branch").call();
    GitAccess.getInstance().setBranch("new_branch");
    
    ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
    toolbarPanel.refresh();
    
    assertEquals(
        "Cannot_pull\nNo_remote_branch.",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to_create_and_track_remote_branch",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>No_upstream_branch</b>.<br></html>",
        toolbarPanel.getBranchSplitMenuButton().getToolTipText());
    
    // Push to create the remote branch
    stagingPanel.getPushPullController().push();
    waitForScheluerBetter();
    
    toolbarPanel.refresh();
    
    // Tooltip texts changed
    assertEquals(
        "Pull_merge_from.\nToolbar_Panel_Information_Status_Up_To_Date",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nNothing_to_push",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>origin/new_branch</b>.<br>"
        + "Toolbar_Panel_Information_Status_Up_To_Date<br>Nothing_to_push</html>",
        toolbarPanel.getBranchSplitMenuButton().getToolTipText());
    
    gitAccess.setBranch("master");
    flushAWT();
    
    // Commit a new file locally
    commitOneFile(localTestRepository, "anotherFile.txt", "");
    waitForScheluerBetter();
    
    // Commit to remote
    commitOneFile(remoteTestRepository, "anotherFile_2.txt", "");
    waitForScheluerBetter();
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    flushAWT();
    toolbarPanel.refresh();
    flushAWT();
    
    // Tooltip texts changed again
    assertEquals(
        "Pull_merge_from.\nOne_commit_behind",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nOne_commit_ahead",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>master</b>.<br>Upstream_branch <b>origin/master</b>.<br>"
        + "One_commit_behind<br>One_commit_ahead</html>",
        toolbarPanel.getBranchSplitMenuButton().getToolTipText());
    
    // Commit a new change locally
    commitOneFile(localTestRepository, "anotherFile.txt", "changed");
    waitForScheluerBetter();
    
    // Commit to remote
    commitOneFile(remoteTestRepository, "anotherFile_2.txt", "changed");
    waitForScheluerBetter();
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    toolbarPanel.refresh();
    flushAWT();
    
    // Tooltip texts changed again
    assertEquals(
        "Pull_merge_from.\nCommits_behind",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nCommits_ahead",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>master</b>.<br>Upstream_branch <b>origin/master</b>.<br>"
        + "Commits_behind<br>Commits_ahead</html>",
        toolbarPanel.getBranchSplitMenuButton().getToolTipText());
  }
  
  /**
   * <p><b>Description:</b> list the Settings menu actions.</p>
   * <p><b>Bug ID:</b> EXM-46442</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSettingsMenu() throws Exception {
    // Set toolbar panel
    stagingPanel.setToolbarPanelFromTests(
        new ToolbarPanel(
            stagingPanel.getPushPullController(),
            refreshSupport,
            null,
            null));
    
    ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
    SplitMenuButton settingsMenuButton = toolbarPanel.getSettingsMenuButton();
    Component[] menuComponents = settingsMenuButton.getMenuComponents();
    assertEquals(3, menuComponents.length);
    assertTrue(menuComponents[0].toString().contains("Reset_all_credentials"));
    assertTrue(menuComponents[1] instanceof Separator);
    assertTrue(menuComponents[2].toString().contains("Preferences"));
  }
  
}
