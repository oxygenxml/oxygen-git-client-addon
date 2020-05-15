package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.jidesoft.swing.JideToggleButton;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.event.GitCommand;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class FlatViewTest extends FlatViewTestBase {
  
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  
  protected final void switchToView(ResourcesViewMode viewMode) {
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(viewMode);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(viewMode);
  }
  
  /**
   * Resolve a conflict using my copy.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testConflict_resolveUsingMine() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testConflict_resolveUsingMine_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testConflict_resolveUsingMine_remote";
    
    
    String localTestRepository2 = localTestRepository + "2";
    File file2 = new File(localTestRepository2 + "/test.txt");

    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo1 = createRepository(localTestRepository);
    
    Repository localRepo2 = createRepository(localTestRepository2);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo2 , remoteRepo);
    
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo1 , remoteRepo);

    // Create a new file and push it.
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = gitAccess.push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();

    assertTableModels("CONFLICT, test.txt", "");
    
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    
    waitForScheduler();
    
    waitForScheluerBetter();
    
    assertTableModels("", "");

    // Check the commit.
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    assertEquals("Commit_to_merge", commitPanel.getCommitMessageArea().getText());
    
    commitPanel.getCommitButton().doClick();
    flushAWT();
    
    // TODO Alex What should it assert here?
    assertEquals("", "");
  }
  
  /**
   * Resolve a conflict using "their" copy, restart merge, and resolve again.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testConflict_resolveUsingTheirsAndRestartMerge() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testConflict_resolveUsingTheirs_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testConflict_resolveUsingTheirs_remote";
    
    
    String localTestRepository2 = localTestRepository + "2";
    File file2 = new File(localTestRepository2 + "/test.txt");

    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo1 = createRepository(localTestRepository);
    
    Repository localRepo2 = createRepository(localTestRepository2);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo2 , remoteRepo);
    
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo1 , remoteRepo);

    // Create a new file and push it.
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "content");
    
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = gitAccess.push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTableModels("CONFLICT, test.txt", "");
    
    // Resolve using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    waitForScheduler();
    
    assertTableModels("", "CHANGED, test.txt");
    
    // Restart merge
    ScheduledFuture<?> restartMerge = gitAccess.restartMerge();
    restartMerge.get();
    
    flushAWT();
    assertTableModels("CONFLICT, test.txt", "");
    
    // Resolve again using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    waitForScheduler();
    
    assertTableModels("", "CHANGED, test.txt");
    
    // Commit
    // TODO Alex What should it assert here?
    gitAccess.commit("commit");
    assertTableModels("", "");
  }
  
  /**
   * Saving a remote file doesn't fail with exceptions. The event should be ignored.
   * 
   * EXM-40662
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testSaveRemoteURL() throws Exception {

    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testSave_local";

    // Create repositories
    createRepository(localTestRepository);
    
    assertTableModels("", "");

    URL remoteResource = new URL("http://oxygenxml.com");

    for (Iterator<WSEditorChangeListener> iterator = editorChangeListeners.iterator(); iterator.hasNext();) {
      WSEditorChangeListener wsEditorChangeListener = iterator.next();
      wsEditorChangeListener.editorOpened(remoteResource);
    }



    for (Iterator<WSEditorListener> iterator = editorListeners.iterator(); iterator.hasNext();) {
      WSEditorListener wsEditorChangeListener = iterator.next();
      wsEditorChangeListener.editorSaved(WSEditorListener.SAVE_OPERATION);
    }


    for (Iterator<WSEditorChangeListener> iterator = editorChangeListeners.iterator(); iterator.hasNext();) {
      WSEditorChangeListener wsEditorChangeListener = iterator.next();
      wsEditorChangeListener.editorClosed(remoteResource);
    }

    assertTableModels("", "");
  }
  
  /**
   * <p><b>Description:</b> don't activate the submodule selection button every time you push/pull.</p>
   * <p><b>Bug ID:</b> EXM-40740</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testDontEnableSubmoduleButtonForEveryPushOrPull() throws Exception {
    // ================= No submodules ====================
    Future<?> pull2 = stagingPanel.getPushPullController().pull();
    pull2.get();
    
    assertFalse(stagingPanel.getToolbarPanel().getSubmoduleSelectButton().isEnabled());
    
    // ================= Set submodule ====================
    stagingPanel.setToolbarPanelFromTests(new ToolbarPanel(stagingPanel.getPushPullController(), refreshSupport, null) {
      @Override
      boolean gitRepoHasSubmodules() {
        return true;
      }
    });
    Future<?> pull = stagingPanel.getPushPullController().pull();
    pull.get();
    flushAWT();
    
    
    assertTrue(stagingPanel.getToolbarPanel().getSubmoduleSelectButton().isEnabled());
  }

  /**
   * <p><b>Description:</b> Tests switching between view modes.</p>
   * <p><b>Bug ID:</b> EXM-43553</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testSwitchViewModes() throws Exception {
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    try {
      String localTestRepository = "target/test-resources/testSwitchViewModes_local";
      String remoteTestRepository = "target/test-resources/testSwitchViewModes_remote";

      // Create repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo = createRepository(localTestRepository);
      bindLocalToRemote(localRepo , remoteRepo);

      // Step 1. Creaet new files and refresh the table view.
      createNewFile(localTestRepository, "test.txt", "test");
      createNewFile(localTestRepository, "test2.txt", "test2");
      refreshViews();
      
      assertTableModels(
          "UNTRACKED, test.txt\n" + 
          "UNTRACKED, test2.txt", "");
      
      // Step 3. Switch to tree view.
      switchToView(ResourcesViewMode.TREE_VIEW);
      
      // The tree view initialized itself from the previous view.
      assertTreeModels(
          "UNTRACKED, test.txt\n" + 
          "UNTRACKED, test2.txt",
          "");
      
      // Step 4. Create a new file while in tree view and refresh the tree to pick up the change.
      createNewFile(localTestRepository, "test3.txt", "test3");
      refreshViews();
      
      // Assert that the tree has picked up the changes.
      assertTreeModels(
          "UNTRACKED, test.txt\n" + 
          "UNTRACKED, test2.txt\n" + 
          "UNTRACKED, test3.txt",
          "");
      
      // Step 6. Switch to table view. The table view should initialize itself from the previous view.
      switchToView(ResourcesViewMode.FLAT_VIEW);
      assertTableModels(
          "UNTRACKED, test.txt\n" + 
          "UNTRACKED, test2.txt\n" + 
          "UNTRACKED, test3.txt", "");
      
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    }
  }
  
  /**
   * <p><b>Description:</b> When a commit fails, files do not disappear from the view.</p>
   * <p><b>Bug ID:</b> EXM-45438</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testFailedCommit() throws Exception {
    String localTestRepository = "target/test-resources/testFailedCommit";
    
    // Create repositories
    createRepository(localTestRepository);
    
    createNewFile(localTestRepository, "test.txt", "content");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    
    // Don't give a message to force an exception.
    try {
      gitAccess.commit(null);
      fail("Exception expected.");
    } catch (NoMessageException e) {
      // Expected.
    }
    
    assertTableModels("", "ADD, test.txt");
  }
  
  /**
   * <p><b>Description:</b> Automatically push when committing.</p>
   * <p><b>Bug ID:</b> EXM-44915</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAutoPushWhenCommit() throws Exception {
    String localTestRepository = "target/test-resources/testAutoPushWhenCommit_local";
    String remoteTestRepository = "target/test-resources/testAutoPushWhenCommit_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    bindLocalToRemote(localRepo , remoteRepo);
    sleep(500);
    
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
    SwingUtilities.invokeLater(() -> stagingPanel.getCommitPanel().getCommitButton().doClick());
    waitForScheluerBetter();
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    // Change the file again.
    setFileContent(file, "modified again");
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
    flushAWT();
    
    SwingUtilities.invokeLater(() -> autoPushBtn.setSelected(true));
    flushAWT();
    assertTrue(autoPushBtn.isSelected());
    
    SwingUtilities.invokeLater(() -> stagingPanel.getCommitPanel().getCommitButton().doClick());
    waitForScheluerBetter();
    assertEquals(0, GitAccess.getInstance().getPushesAhead());
  }

}
