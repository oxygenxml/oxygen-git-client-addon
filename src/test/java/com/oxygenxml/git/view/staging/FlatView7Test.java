package com.oxygenxml.git.view.staging;

import java.awt.Component;

import javax.swing.JPopupMenu.Separator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

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
   * <p><b>Bug ID:</b> EXM-45599, EXM-44564</p>
   *
   * @author sorin_carbunaru
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testToolbarComponentsTooltips() throws Exception {
    // Set toolbar panel
    stagingPanel.setToolbarPanelFromTests(
        new ToolbarPanel(
            (GitController) stagingPanel.getGitController(),
            refreshSupport,
            null,
            null));
    
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
    
    BranchesPanel branchesPanel = stagingPanel.getBranchesPanel();
    
    ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
    toolbarPanel.refresh();
    flushAWT();
    sleep(200);
    
    assertEquals(
        "<html>Cannot_pull<br>No_remote_branch.</html>",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "<html>Push_to_create_and_track_remote_branch</html>",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>No_upstream_branch</b>.<br></html>",
        branchesPanel.getToolTipText());
    
    // Push to create the remote branch
    ((GitController) stagingPanel.getGitController()).push();
    waitForScheluerBetter();
    
    toolbarPanel.refresh();
    
    // Tooltip texts changed
    assertEquals(
        "<html>Pull_merge_from.<br>Toolbar_Panel_Information_Status_Up_To_Date<br><br></html>",
        toolbarPanel.getPullMenuButton().getToolTipText());
    assertEquals(
        "<html>Push_to.<br>Nothing_to_push<br><br></html>",
        toolbarPanel.getPushButton().getToolTipText());
    assertEquals(
        "<html>Local_branch <b>new_branch</b>.<br>Upstream_branch <b>origin/new_branch</b>.<br>"
        + "Toolbar_Panel_Information_Status_Up_To_Date<br>Nothing_to_push</html>",
        branchesPanel.getToolTipText());
    
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
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
    String expected = "<html>Pull_merge_from.<br>One_commit_behind<br><br>&#x25AA; Date, Hour &ndash; AlexJitianu (2 files)"
        + "<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
    String regexDate = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
    String regexHour = "(\\d\\d:\\d\\d)";
    String actual = toolbarPanel.getPullMenuButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    ); 
    
    expected = "<html>Push_to.<br>One_commit_ahead<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
    actual = toolbarPanel.getPushButton().getToolTipText();
   
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );  
   
    assertEquals(
        "<html>Local_branch <b>" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>Upstream_branch <b>origin/" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>"
        + "One_commit_behind<br>One_commit_ahead</html>",
        branchesPanel.getToolTipText());
    
    // Commit a new change locally
    commitOneFile(localTestRepository, "anotherFile.txt", "changed");
    waitForScheluerBetter();
    
    // Commit to remote
    commitOneFile(remoteTestRepository, "anotherFile_2.txt", "changed");
    waitForScheluerBetter();
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    toolbarPanel.refresh();
    flushAWT();
    sleep(200);
    
    expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour &ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
    actual = toolbarPanel.getPullMenuButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    ); 
    
    expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
    actual = toolbarPanel.getPushButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );  
    
    assertEquals(
        "<html>Local_branch <b>" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>Upstream_branch <b>origin/" + GitAccess.DEFAULT_BRANCH_NAME + "</b>.<br>"
        + "Commits_behind<br>Commits_ahead</html>",
        branchesPanel.getToolTipText());
    
    // Commit a new change locally
    commitOneFile(localTestRepository, "anotherFile200000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000"
        + ".txt", "changed2");
    waitForScheluerBetter();
    
    // Commit to remote
    commitOneFile(remoteTestRepository, "anotherFile300000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        + "000000000000000000000000000000000000000000000000000"
        + ".txt", "changed3");
    waitForScheluerBetter();
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    toolbarPanel.refresh();
    flushAWT();
    sleep(200);
    
    expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile300000000000000000000000000000000000000...<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br></html>";
    actual = toolbarPanel.getPullMenuButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );  
    
    expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile200000000000000000000000000000000000000...<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br></html>";
    actual = toolbarPanel.getPushButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );  
    
    String[] filesForCommit = {
        "6anotherFile45.txt",
        "5anotherFile45.txt",
        "4anotherFile45.txt",
        "3anotherFile45.txt",
        "2anotherFile45.txt",
        "1anotherFile45.txt",
        "anotherFil233e45.txt",
        "anotherFil333e45.txt",
        "anotherFileee45.txt",
        "anotherFile45w.txt"
    };
    
    for (int i = 0; i < filesForCommit.length; i++) {
      // Commit a new change locally
      commitOneFile(localTestRepository, filesForCommit[i], "changed");
      waitForScheluerBetter();
      
      // Commit to remote
      commitOneFile(remoteTestRepository, "_" + filesForCommit[i], "changed");
      waitForScheluerBetter();
    }
    
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository);
    toolbarPanel.refresh();
    flushAWT();
    sleep(500);
    
    expected =  "<html>Pull_merge_from.<br>Commits_behind<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFile45w.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFileee45.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFil333e45.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: _anotherFil233e45.txt<br>&#x25AA; [...] "
        + "&ndash; N_More_Commits<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (2 files)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile_2.txt<br><br>See_all_commits_in_Git_History</html>";
    actual = toolbarPanel.getPullMenuButton().getToolTipText();
    assertEquals(
        expected,
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );  
    
    expected = "<html>Push_to.<br>Commits_ahead<br><br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile45w.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFileee45.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFil333e45.txt<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFil233e45.txt<br>&#x25AA; [...] "
        + "&ndash; N_More_Commits<br>&#x25AA; Date, Hour "
        + "&ndash; AlexJitianu (1 file)<br>&nbsp;&nbsp;&nbsp;New file: anotherFile.txt<br><br>See_all_commits_in_Git_History</html>";
    actual = toolbarPanel.getPushButton().getToolTipText();
    assertEquals(
        expected.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour"),
        actual.replaceAll(regexDate, "Date").replaceAll(regexHour, "Hour")
    );      
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
            (GitController) stagingPanel.getGitController(),
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
