package com.oxygenxml.git.view;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;

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
        new ToolbarPanel(stagingPanel.getPushPullController(), refreshSupport, null));
    
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
    toolbarPanel.updateStatus();
    
    assertEquals(
        "Cannot_pull\nNo_remote_branch.",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "<html>Push_to_create_remote_branch</html>",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Remote_branch <b>No_remote_branch</b>.<br></html>",
        toolbarPanel.getRemoteAndBranchInfoLabel().getToolTipText());
    
    // Push to create the remote branch
    stagingPanel.getPushPullController().push();
    waitForScheluerBetter();
    
    toolbarPanel.updateStatus();
    
    // Tooltip texts changed
    assertEquals(
        "Pull_merge_from.\nToolbar_Panel_Information_Status_Up_To_Date",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nNothing_to_push",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Remote_branch <b>origin/new_branch</b>.<br>"
        + "Toolbar_Panel_Information_Status_Up_To_Date<br>Nothing_to_push</html>",
        toolbarPanel.getRemoteAndBranchInfoLabel().getToolTipText());
    
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
    toolbarPanel.updateStatus();
    flushAWT();
    
    // Tooltip texts changed again
    assertEquals(
        "Pull_merge_from.\nOne_commit_behind",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nOne_commit_ahead",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>master</b>.<br>Remote_branch <b>origin/master</b>.<br>"
        + "One_commit_behind<br>One_commit_ahead</html>",
        toolbarPanel.getRemoteAndBranchInfoLabel().getToolTipText());
    
    // Commit a new change locally
    commitOneFile(localTestRepository, "anotherFile.txt", "changed");
    waitForScheluerBetter();
    
    // Commit to remote
    commitOneFile(remoteTestRepository, "anotherFile_2.txt", "changed");
    waitForScheluerBetter();
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    toolbarPanel.updateStatus();
    flushAWT();
    
    // Tooltip texts changed again
    assertEquals(
        "Pull_merge_from.\nCommits_behind",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "Push_to.\nCommits_ahead",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>master</b>.<br>Remote_branch <b>origin/master</b>.<br>"
        + "Commits_behind<br>Commits_ahead</html>",
        toolbarPanel.getRemoteAndBranchInfoLabel().getToolTipText());
  }
  
}
