package com.oxygenxml.git.view.staging;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;

import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.ChangesPanel;
import com.oxygenxml.git.view.staging.CommitAndStatusPanel;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.util.TreeUtil;

import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class TreeViewTest extends FlatViewTestBase {
  
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.TREE_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.TREE_VIEW);
  }
  
  /**
   * Invokes the change button on the view.
   * 
   * @param stage <code>true</code> to mode files from UnStage to the IDNEX.
   * <code>false</code> to move files out of the INDEX.
   * @param index Index in the table of the file to move.
   */
  private void change(boolean stage, String fileToSelect) {
    ChangesPanel changesPanel = null;
    if (stage) {
      changesPanel = stagingPanel.getUnstagedChangesPanel();
    } else {
      changesPanel = stagingPanel.getStagedChangesPanel();
    }
    
    JTree filesTree = changesPanel.getTreeView();
    
    JButton ssButton = changesPanel.getChangeSelectedButton();
    
    expandAll(filesTree);
    
    TreePath treePath = TreeUtil.getTreePath(filesTree.getModel(), fileToSelect);
    filesTree.getSelectionModel().setSelectionPath(treePath);
    flushAWT();
    
    assertTrue(ssButton.isEnabled());
    ssButton.doClick();
    
    waitForScheduler();
  }
  
  private static void expandAll(JTree tree) {
    TreeUtil.expandAllNodes(tree, 0, tree.getRowCount());
  }
  
  /**
   * Stage/Unstage all files from the model.
   * 
   * @param stage <code>true</code> to stage. <code>false</code> to un-stage.
   */
  private void changeAll(boolean stage) {
    ChangesPanel changesPanel = null;
    if (stage) {
      changesPanel = stagingPanel.getUnstagedChangesPanel();
    } else {
      changesPanel = stagingPanel.getStagedChangesPanel();
    }
    
    JButton ssButton = changesPanel.getChangeAllButton();
    assertTrue(ssButton.isEnabled());
    ssButton.doClick();
    
    waitForScheduler();
  }
  
  /**
   * Stage and UnStage a newly created file.
   *  
   * @throws Exception If it fails.
   */
  @Test
  public void testStageUnstage_NewFile() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testStageUnstage_NewFile_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testStageUnstage_NewFile_remote";
    
    // Create a new test file.
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // The newly created file is present in the model.
    assertTreeModels(
        "UNTRACKED, test.txt",
        "");

    //---------------
    // Stage.
    //---------------
    change(true, "test.txt");
    // The file has moved to the INDEX.
    assertTreeModels("", "ADD, test.txt");

    //---------------
    // Back to unStaged
    //---------------
    change(false, "test.txt");
    assertTreeModels("UNTRACKED, test.txt", "");
  }
  
  /**
   * Stage All and UnStage All on newly created files.
   *  
   * @throws Exception If it fails.
   */
  @Test
  public void testStageUnstage_NewMultipleFiles() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testStageUnstage_NewMultipleFiles_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testStageUnstage_NewMultipleFiles_remote";
    
    
    // Create a new test file.
    new File(localTestRepository).mkdirs();
    createNewFile(localTestRepository, "test.txt", "");
    
    createNewFile(localTestRepository, "test2.txt", "");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // The newly created file is present in the model.
    assertTreeModels(
        "UNTRACKED, test.txt\n" + 
        "UNTRACKED, test2.txt", 
        "");
    
    changeAll(true);
    
    // The newly created file is present in the model.
    assertTreeModels(
        "", 
        "ADD, test.txt\n" + 
        "ADD, test2.txt");
    
    //--------------
    // Back to unStaged
    //---------------
    changeAll(false);
    flushAWT();
    
    
    // The newly created file is present in the model.
    assertTreeModels(
        "UNTRACKED, test.txt\n" + 
        "UNTRACKED, test2.txt", 
        "");
  }

  /**
   * Stage and UnStage a newly created file. At some point in the INDEX we have an old version
   * and in the UnStaged area we have a newer version.
   *  
   * @throws Exception If it fails.
   */
  @Test
  public void testStageUnstage_NewFile_2() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testStageUnstage_NewFile_2_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testStageUnstage_NewFile_2_remote";
    
    
    // Create a new test file.
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // The newly created file is present in the model.
    assertTreeModels("UNTRACKED, test.txt","");

    change(true, "test.txt");
    
    // The file has moved to the INDEX.
    assertTreeModels("", "ADD, test.txt");
  
    //----------------
    // Change the file again.
    //----------------
    setFileContent(file, "new content");
    assertTreeModels("MODIFIED, test.txt", "ADD, test.txt");
    
    //--------------
    // Back to unstaged
    //---------------
    change(false, "test.txt");
    
    assertTreeModels("UNTRACKED, test.txt","");
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
    File file = new File(localTestRepository + "/test.txt");
    createNewFile(localTestRepository, "test.txt", "content");
    
    add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    stagingPanel.getGitController().asyncResolveUsingMine(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    
    waitForScheduler();
    
    waitForScheluerBetter();
    
    assertTreeModels("", "");
    
    // Check the commit.
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    assertEquals("Commit_to_merge", commitPanel.getCommitMessageArea().getText());
    
    commitPanel.getCommitButton().doClick();
    waitForScheduler();

    // TODO What should it assert here?
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
    
    add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    // Resolve using theirs
    stagingPanel.getGitController().asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    waitForScheduler();
    
    assertTreeModels("", "CHANGED, test.txt");
    
    // Restart merge
    ScheduledFuture<?> restartMerge = gitAccess.restartMerge();
    restartMerge.get();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    // Resolve again using theirs
    stagingPanel.getGitController().asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    waitForScheduler();
    
    assertTreeModels("", "CHANGED, test.txt");
    
    // Commit
    gitAccess.commit("commit");
    flushAWT();
    assertTreeModels("", "");
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
    
    assertTreeModels("", "");

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

    assertTreeModels("", "");
  }
  
  /**
   * <p><b>Description:</b> stage / unstage folder.</p>
   * <p><b>Bug ID:</b> EXM-40615</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testStageUnstage_Folder() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_Folder_local";
    String remoteTestRepository = "target/test-resources/testStageUnstage_Folder_remote";
    
    // Create a new test file.
    new File(localTestRepository + "/folder").mkdirs();
    createNewFile(localTestRepository, "/folder/test.txt", "");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // The newly created file is present in the model.
    assertTreeModels("UNTRACKED, folder/test.txt", "");

    //---------------
    // Stage.
    //---------------
    change(true, "folder");
    assertTreeModels("", "ADD, folder/test.txt");

    //---------------
    // Back to unStaged
    //---------------
    change(false, "folder");
    assertTreeModels("UNTRACKED, folder/test.txt", "");
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
    String localTestRepository = "target/test-resources/treeMode_testFailedCommit";
    
    // Create repositories
    createRepository(localTestRepository);
    
    createNewFile(localTestRepository, "test.txt", "content");
    add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    
    
    // Don't give a message to force an exception.
    try {
      gitAccess.commit(null);
      fail("Exception expected.");
    } catch (NoMessageException e) {
      // Expected.
    }
    
    assertTreeModels("", "ADD, test.txt");
  }
}
