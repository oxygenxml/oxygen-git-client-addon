package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.event.GitCommandEvent;

import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class TreeViewTest extends FlatViewTestBase {
  
  @Override
  protected void setUp() throws Exception {
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
    
    TreePath treePath = TreeFormatter.getTreePath(filesTree.getModel(), fileToSelect);
    filesTree.getSelectionModel().setSelectionPath(treePath);
    flushAWT();
    
    assertTrue(ssButton.isEnabled());
    ssButton.doClick();
  }
  
  private static void expandAll(JTree tree) {
    TreeFormatter.expandAllNodes(tree, 0, tree.getRowCount());
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
  }
  
  /**
   * Stage and UnStage a newly created file.
   *  
   * @throws Exception If it fails.
   */
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
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    
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
   * Discard changes. THose files must not appear in either stage/un-stage area.
   * 
   * @throws Exception If it fails.
   */
  public void testDiscard() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testDiscard_NewFile_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testDiscard_NewFile_remote";
    
    
    new File(localTestRepository).mkdirs();
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    setFileContent(file, "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    assertTreeModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTreeModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertTreeModels("MODIFIED, test.txt", "");
    
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertTreeModels("", "CHANGED, test.txt");
    
    // Change the file.
    setFileContent(file, "modified content");
    
    // The file is present in  both areas.
    assertTreeModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    // Discard.
    DiscardAction discardAction = new DiscardAction(
        Arrays.asList(new FileStatus(GitChangeType.MODIFIED, "test.txt")),
        stagingPanel.getStageController());
    discardAction.actionPerformed(null);
    assertTreeModels(
        "", 
        "");    
  }

  /**
   * Stage and UnStage a newly created file.
   *  
   * @throws Exception If it fails.
   */
  public void testStageUnstage_ModifiedFile() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_remote";
    
    new File(localTestRepository).mkdirs();
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    setFileContent(file, "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    assertTreeModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTreeModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertTreeModels("MODIFIED, test.txt", "");
    //------------
    // Add to INDEX (Stage)
    //------------
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertTreeModels("", "CHANGED, test.txt");
    
    //-----------------
    // Change the file again. It will appear in the index as well.
    //------------------
    setFileContent(file, "modified content");
    
    assertTreeModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    //------------------
    // Unstage the file from the INDEX.
    //------------------
    change(false, "test.txt");
    
    assertTreeModels(
        "MODIFIED, test.txt", 
        "");
  }


  /**
   * Stage All and UnStage All on newly created files.
   *  
   * @throws Exception If it fails.
   */
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
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    File file2 = new File(localTestRepository + "/test2.txt");
    file2.createNewFile();
    
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
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    
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
    file.createNewFile();
    setFileContent(file, "content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = gitAccess.push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandEvent.RESOLVE_USING_MINE_STARTED);
    
    assertTreeModels("", "");

    // Check the commit.
    CommitAndStatusPanel commitPanel = stagingPanel.getCommitPanel();
    flushAWT();
    sleep(1000);
    assertEquals("Conclude_Merge_Message", commitPanel.getCommitMessage().getText());
    
    commitPanel.getCommitButton().doClick();
    flushAWT();
    
    assertEquals("", "");
  }
  
  /**
   * Resolve a conflict using "their" copy, restart merge, and resolve again.
   * 
   * @throws Exception If it fails.
   */
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
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    setFileContent(file, "content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First version.");
    PushResponse push = gitAccess.push("", "");
    assertEquals("status: OK message null", push.toString());
    
    gitAccess.setRepositorySynchronously(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    // Resolve using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandEvent.RESOLVE_USING_THEIRS_STARTED);
    flushAWT();
    assertTreeModels("", "CHANGED, test.txt");
    
    // Restart merge
    gitAccess.restartMerge();
    flushAWT();
    assertTreeModels("CONFLICT, test.txt", "");
    
    // Resolve again using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandEvent.RESOLVE_USING_THEIRS_STARTED);
    flushAWT();
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
  public void testStageUnstage_Folder() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_Folder_local";
    String remoteTestRepository = "target/test-resources/testStageUnstage_Folder_remote";
    
    // Create a new test file.
    new File(localTestRepository).mkdirs();
    new File(localTestRepository + "/folder").mkdirs();
    File file = new File(localTestRepository + "/folder/test.txt");
    file.createNewFile();
    
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
}
