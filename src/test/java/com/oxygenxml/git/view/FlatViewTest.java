package com.oxygenxml.git.view;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JTable;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.GitCommand;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class FlatViewTest extends FlatViewTestBase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }
  /**
   * Invokes the change button on the view.
   * 
   * @param stage <code>true</code> to mode files from UnStage to the IDNEX.
   * <code>false</code> to move files out of the INDEX.
   * @param index Index in the table of the file to move.
   */
  private void change(boolean stage, int index) {
    if (stage) {
      ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
      JTable filesTable = unstagedChangesPanel.getFilesTable();
      
      JButton ssButton = unstagedChangesPanel.getChangeSelectedButton();
      filesTable.getSelectionModel().setSelectionInterval(index, index);
      
      assertTrue(ssButton.isEnabled());
      ssButton.doClick();
    } else {
      ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
      JTable stFilesTable = stagedChangesPanel.getFilesTable();
      
      JButton usButton = stagedChangesPanel.getChangeSelectedButton();
      stFilesTable.getSelectionModel().setSelectionInterval(index, index);
      assertTrue(usButton.isEnabled());
      usButton.doClick();
    }
  }
  
  /**
   * Stage/Unstage all files from the model.
   * 
   * @param stage <code>true</code> to stage. <code>false</code> to un-stage.
   */
  private void changeAll(boolean stage) {
    if (stage) {
      ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
      
      JButton ssButton = unstagedChangesPanel.getChangeAllButton();
      assertTrue(ssButton.isEnabled());
      ssButton.doClick();
    } else {
      ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
      
      JButton usButton = stagedChangesPanel.getChangeAllButton();
      assertTrue(usButton.isEnabled());
      usButton.doClick();
    }
  }
  
  protected final void switchToView(ResourcesViewMode viewMode) {
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(viewMode);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(viewMode);
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
    assertTableModels(
        "UNTRACKED, test.txt",
        "");

    //---------------
    // Stage.
    //---------------
    change(true, 0);
    // The file has moved to the INDEX.
    assertTableModels("", "ADD, test.txt");

    //---------------
    // Back to unStaged
    //---------------
    change(false, 0);
    assertTableModels("UNTRACKED, test.txt", "");
  }
  
  /**
   * Stage a file that has been renamed only by changing the letter case.
   * 
   * EXM-41394
   *  
   * @throws Exception If it fails.
   */
  public void testStageFileWithModifiedLetterCase() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_NewFile_localX";
    String remoteTestRepository = "target/test-resources/testStageUnstage_NewFile_remoteX";
    
    // Create a new test file.
    new File(localTestRepository).mkdirs();
    File originalFile = new File(localTestRepository + "/test.txt");
    originalFile.createNewFile();
    setFileContent(originalFile, "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    assertTableModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTableModels("", "");
    
    //---------------------------
    originalFile.renameTo(new File(localTestRepository + "/Test.txt"));
    refreshSupport.call();
    flushAWT();
    sleep(1000);

    //---------------
    // Stage.
    //---------------
    changeAll(true);
    flushAWT();
    
    // The file has moved to the INDEX.
    assertTableModels(
        // Unstaged
        "",
        // Staged
        "ADD, Test.txt\n" + 
        "REMOVED, test.txt");

    String listedFiles = Arrays.toString(new File(localTestRepository).listFiles());
    assertFalse(listedFiles.contains("test.txt"));
    assertTrue(listedFiles.contains("Test.txt"));
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
    assertTableModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTableModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertTableModels("MODIFIED, test.txt", "");
    
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertTableModels("", "CHANGED, test.txt");
    
    // Change the file.
    setFileContent(file, "modified content");
    
    // The file is present in  both areas.
    assertTableModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    // Discard.
    DiscardAction discardAction = new DiscardAction(
        Arrays.asList(new FileStatus(GitChangeType.MODIFIED, "test.txt")),
        stagingPanel.getStageController());
    discardAction.actionPerformed(null);
    assertTableModels(
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
    assertTableModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTableModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertTableModels("MODIFIED, test.txt", "");
    //------------
    // Add to INDEX (Stage)
    //------------
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertTableModels("", "CHANGED, test.txt");
    
    //-----------------
    // Change the file again. It will appear in the index as well.
    //------------------
    setFileContent(file, "modified content");
    
    assertTableModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    //------------------
    // Unstage the file from the INDEX.
    //------------------
    change(false, 0);
    
    assertTableModels(
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
    assertTableModels(
        "UNTRACKED, test.txt\n" + 
        "UNTRACKED, test2.txt", 
        "");
    
    changeAll(true);
    
    // The newly created file is present in the model.
    assertTableModels(
        "", 
        "ADD, test.txt\n" + 
        "ADD, test2.txt");
    
    //--------------
    // Back to unStaged
    //---------------
    changeAll(false);
    
    // The newly created file is present in the model.
    assertTableModels(
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
    assertTableModels("UNTRACKED, test.txt","");

    change(true, 0);
    
    // The file has moved to the INDEX.
    assertTableModels("", "ADD, test.txt");
  
    //----------------
    // Change the file again.
    //----------------
    setFileContent(file, "new content");
    assertTableModels("MODIFIED, test.txt", "ADD, test.txt");
    
    //--------------
    // Back to unstaged
    //---------------
    change(false, 0);
    
    assertTableModels("UNTRACKED, test.txt","");
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
    
    gitAccess.setRepository(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepository(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();

    assertTableModels("CONFLICT, test.txt", "");
    
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    
    assertTableModels("", "");

    // Check the commit.
    CommitPanel commitPanel = stagingPanel.getCommitPanel();
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
    
    gitAccess.setRepository(localTestRepository2);
    // Commit a new version of the file.
    setFileContent(file2, "modified from 2nd local repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    gitAccess.push("", "");
    
    // Change back the repo.
    gitAccess.setRepository(localTestRepository);
    
    // Change the file. Create a conflict.
    setFileContent(file, "modified from 1st repo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("modified from 2nd local repo");
    
    // Get the remote. The conflict appears.
    pull();
    flushAWT();
    assertTableModels("CONFLICT, test.txt", "");
    
    // Resolve using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    assertTableModels("", "CHANGED, test.txt");
    
    // Restart merge
    gitAccess.restartMerge();
    flushAWT();
    assertTableModels("CONFLICT, test.txt", "");
    
    // Resolve again using theirs
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    assertTableModels("", "CHANGED, test.txt");
    
    // Commit
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
  public void testDontEnableSubmoduleButtonForEveryPushOrPull() throws Exception {
    // ================= No submodules ====================
    stagingPanel.getPushPullController().execute(Command.PULL);
    sleep(500);
    
    assertFalse(stagingPanel.getToolbarPanel().getSubmoduleSelectButton().isEnabled());
    
    // ================= Set submodule ====================
    stagingPanel.setToolbarPanelFromTests(new ToolbarPanel(stagingPanel.getPushPullController(), refreshSupport) {
      @Override
      boolean gitRepoHasSubmodules() {
        return true;
      }
    });
    stagingPanel.getPushPullController().execute(Command.PULL);
    sleep(500);
    
    assertTrue(stagingPanel.getToolbarPanel().getSubmoduleSelectButton().isEnabled());
  }

  /**
   * <p><b>Description:</b> lock fail test.</p>
   * <p><b>Bug ID:</b> EXM-42867</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  public void testPullCannotLockRef() throws Exception {
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    try {
      final boolean[] showErrorMessageCalled = new boolean[] {false};
      
      StandalonePluginWorkspace pluginWorkspaceMock = Mockito.mock(StandalonePluginWorkspace.class);
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          showErrorMessageCalled[0] = true;
          return null;
        }
      }).when(pluginWorkspaceMock).showErrorMessage(Mockito.anyString());
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspaceMock);
      
      String localTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_local_pullCannotLock";
      String remoteTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_remote_pullCannotLock";

      // Create repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo = createRepository(localTestRepository);
      bindLocalToRemote(localRepo , remoteRepo);

      // Create a new file and push it.
      String fileName = "test.txt";
      File file = commitNewFile(localTestRepository, fileName, "content");
      PushResponse push = gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Create lock files
      String repoDir = gitAccess.getRepository().getDirectory().getAbsolutePath();
      Ref ref = gitAccess.getRemoteBrachListForCurrentRepo().get(0);
      File lockFile = new File(repoDir, ref.getName() + ".lock");
      lockFile.createNewFile();
      setFileContent(lockFile, gitAccess.getLastLocalCommit().getName());

      // Commit a new version of the file.
      setFileContent(file, "modified");
      gitAccess.add(new FileStatus(GitChangeType.ADD, fileName));
      gitAccess.commit("modified");
      gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Pull should throw "Lock failed" error
      PullResponse pullResponse = gitAccess.pull("", "");
      assertEquals(PullStatus.LOCK_FAILED, pullResponse.getStatus());
      assertTrue(showErrorMessageCalled[0]);
      stagingPanel.getPushPullController().execute(Command.PULL);
      sleep(300);
      assertEquals("Lock_failed", stagingPanel.getCommitPanel().getStatusLabel().getText());
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    }
  }
  
  /**
   * Creates a new file and commits it the repository.
   * 
   * @param parentDir parent directory.
   * @param fileName File name.
   * @param content Content for the new file.
   * 
   * @return The newly created file.
   * 
   * @throws Exception If it fails.
   */
  private File commitNewFile(String parentDir, String fileName, String content) throws Exception {
    File file = createNewFile(parentDir, fileName, content);
    gitAccess.add(new FileStatus(GitChangeType.ADD, fileName));
    gitAccess.commit("First version.");
    
    return file;
  }
  private File createNewFile(String parentDir, String fileName, String content) throws IOException, Exception {
    new File(parentDir).mkdirs();
    File file = new File(parentDir + "/" + fileName);
    file.createNewFile();
    setFileContent(file, content);
    return file;
  }

  /**
   * <p><b>Description:</b> Tests switching between view modes.</p>
   * <p><b>Bug ID:</b> EXM-43553</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
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

}
