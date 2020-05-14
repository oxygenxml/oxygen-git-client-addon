package com.oxygenxml.git.view;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
   * <p><b>Description:</b> Amend commit that was not yet pushed. Edit the file content.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasNotPushed_editFileContent() throws Exception {
    String localTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editFileContent_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editFileContent_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    // Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    RevCommit firstCommit = getLastCommit();
    
    assertFalse(commitPanel.getCommitButton().isEnabled());
    
    // Change the file again.
    setFileContent(file, "modified");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitMessageArea().setText("REPLACE THIS, PLEASE"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(false));
    flushAWT();
    assertFalse(amendBtn.isSelected());
    assertEquals("REPLACE THIS, PLEASE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitButton().doClick());
    waitForScheluerBetter();
    flushAWT();
    sleep(300);
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    RevCommit lastCommit = getLastCommit();
    final List<DiffEntry> diffs = GitAccess.getInstance().getGit().diff()
        .setOldTree(prepareTreeParser(GitAccess.getInstance().getRepository(), firstCommit.getName()))
        .setNewTree(prepareTreeParser(GitAccess.getInstance().getRepository(), lastCommit.getName()))
        .call();
    assertEquals(1, diffs.size());
    
    DiffEntry diffEntry = diffs.get(0);
    assertEquals("DiffEntry[MODIFY test.txt]", diffEntry.toString());
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was not yet pushed. Edit only the commit message.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasNotPushed_editCommitMessage() throws Exception {
    String localTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editCommitMessage_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatWasNotPushed_editCommitMessage_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "content");
    
    // Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    RevCommit firstCommit = getLastCommit();
    
    assertFalse(commitPanel.getCommitButton().isEnabled());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(false));
    flushAWT();
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    assertTrue(amendBtn.isSelected());
    assertEquals("FIRST COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    assertTrue(commitPanel.getCommitButton().isEnabled());
    
    SwingUtilities.invokeLater(() -> commitPanel.getCommitMessageArea().setText("EDITED MESSAGE"));
    SwingUtilities.invokeLater(() -> commitPanel.getCommitButton().doClick());
    waitForScheluerBetter();
    flushAWT();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    assertFalse(amendBtn.isSelected());
    assertEquals("", commitPanel.getCommitMessageArea().getText());
    
    RevCommit lastCommit = getLastCommit();
    final List<DiffEntry> diffs = GitAccess.getInstance().getGit().diff()
        .setOldTree(prepareTreeParser(GitAccess.getInstance().getRepository(), firstCommit.getName()))
        .setNewTree(prepareTreeParser(GitAccess.getInstance().getRepository(), lastCommit.getName()))
        .call();
    assertEquals(0, diffs.size());
    assertEquals("EDITED MESSAGE", lastCommit.getFullMessage());
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was pushed. Change file content.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasPushed_changeFileContent() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testAmendCommitThatNotPushed_1_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatNotPushed_1_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // >>> Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    // >>> Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    // >>> Push
    GitAccess.getInstance().push("", "");
    waitForScheluerBetter();
    refreshSupport.call();
    waitForScheluerBetter();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    
    // Change the file again.
    setFileContent(file, "modified");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("SECOND COMMIT MESSAGE");
      });
    flushAWT();
    
    PluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    assertTrue(commitPanel.getCommitButton().isEnabled());
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    // The amend was cancelled. We must not see the first commit message.
    assertEquals("SECOND COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    // Still enabled because we have a staged file
    assertTrue(commitPanel.getCommitButton().isEnabled());
    assertFalse(amendBtn.isSelected());
  }
  
  /**
   * <p><b>Description:</b> Amend commit that was pushed. Edit commit message.</p>
   * <p><b>Bug ID:</b> EXM-41392</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasPushed_editCommitMesage() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testAmendCommitThatNotPushed_2_local";
    String remoteTestRepository = "target/test-resources/testAmendCommitThatNotPushed_2_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    
    pushOneFileToRemote(localTestRepository, "init.txt", "hello");
    flushAWT();
   
    // Create a new file
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "content");
    
    // No amend by default
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    JideToggleButton amendBtn = commitPanel.getAmendLastCommitToggle();
    assertFalse(amendBtn.isSelected());
    
    // >>> Stage
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    // >>> Commit the test file
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("FIRST COMMIT MESSAGE");
      commitPanel.getCommitButton().doClick();
      });
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    // >>> Push
    GitAccess.getInstance().push("", "");
    waitForScheluerBetter();
    refreshSupport.call();
    waitForScheluerBetter();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
    
    SwingUtilities.invokeLater(() -> {
      commitPanel.getCommitMessageArea().setText("SECOND COMMIT MESSAGE");
      });
    flushAWT();
    
    PluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.when(pluginWSMock.showConfirmDialog(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any(),
        Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    SwingUtilities.invokeLater(() -> amendBtn.setSelected(true));
    waitForScheluerBetter();
    flushAWT();
    // The amend was cancelled. We must not see the first commit message.
    assertEquals("SECOND COMMIT MESSAGE", commitPanel.getCommitMessageArea().getText());
    // Not enabled because we have don't have staged files.
    assertFalse(commitPanel.getCommitButton().isEnabled());
    assertFalse(amendBtn.isSelected());
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
    // Create repositories
    String localTestRepository = "target/test-resources/test_EXM_45599_local";
    String localTestRepository_2 = "target/test-resources/test_EXM_45599_local_2";
    String remoteTestRepository = "target/test-resources/test_EXM_45599_remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo , remoteRepo);
    bindLocalToRemote(localRepo_2 , remoteRepo);
    
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
  
  /**
   * Prepare tree parser.
   * 
   * @param repository Repository.
   * @param objectId   Commit.
   * 
   * @return Tree iterator.
   * 
   * @throws IOException
   */
  private AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
    try (RevWalk walk = new RevWalk(repository)) {
        RevCommit commit = walk.parseCommit(repository.resolve(objectId));
        RevTree tree = walk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree.getId());
        }
        walk.dispose();
        return treeParser;
    }
  }

}
