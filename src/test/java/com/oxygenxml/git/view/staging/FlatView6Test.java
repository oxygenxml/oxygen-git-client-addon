package com.oxygenxml.git.view.staging;

import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class FlatView6Test extends FlatViewTestBase {
  
  @Override
  @Before
  public void setUp() throws Exception {
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
      flushAWT();
      
      assertTrue(ssButton.isEnabled());
      ssButton.doClick();
    } else {
      ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
      JTable stFilesTable = stagedChangesPanel.getFilesTable();
      
      JButton usButton = stagedChangesPanel.getChangeSelectedButton();
      stFilesTable.getSelectionModel().setSelectionInterval(index, index);
      flushAWT();
      
      assertTrue(usButton.isEnabled());
      usButton.doClick();
    }
    
    waitForScheduler();
    
    flushAWT();
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
    
    waitForScheluerBetter();
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
  @Test
  public void testStageUnstage_NewFile() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_NewFile_local";
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
   * Stage and UnStage a newly created file.
   *  
   * @throws Exception If it fails.
   */
  @Test
  public void testStageUnstage_ModifiedFile() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_local";
    String remoteTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_remote";
    
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Add it to the index.
    add(new FileStatus(GitChangeType.UNTRACKED, "test.txt"));
    flushAWT();
    sleep(400);
    assertTableModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    assertTableModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    assertTableModels("MODIFIED, test.txt", "");
    
    //------------
    // Add to INDEX (Stage)
    //------------
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
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
   * <p>
   * <b>Description:</b> Tests the Stage button enabling.
   * </p>
   * 
   * <p>
   * <b>Bug ID:</b> EXM-48559
   * </p>
   *
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test  
  public void testStageButtonEnabled() throws Exception {
    String localTestRepository = "target/test-resources/testStageButtonVisibility_local";
    String remoteTestRepository = "target/test-resources/testStageButtonVisibility_remote";
    
    new File(localTestRepository).mkdirs();
    File file = createNewFile(localTestRepository, "test.txt", "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
    JButton usButton = unstagedChangesPanel.getChangeSelectedButton();
    assertFalse(usButton.isEnabled());
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        unstagedChangesPanel.getFilesTable().selectAll();
      }
    });
    sleep(500);
    assertTrue(usButton.isEnabled());
    List<FileStatus> allFilesUnstagedPanel = unstagedChangesPanel.getFilesStatuses();
    allFilesUnstagedPanel.add(new FileStatus(GitChangeType.CONFLICT, "another_file.txt"));
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        unstagedChangesPanel.getFilesTable().selectAll();
      }
    });
    sleep(500);
    assertFalse(usButton.isEnabled());
    
    allFilesUnstagedPanel.clear();
    allFilesUnstagedPanel.add(new FileStatus(GitChangeType.CONFLICT, "first.txt"));
    allFilesUnstagedPanel.add(new FileStatus(GitChangeType.CONFLICT, "second.txt"));
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        unstagedChangesPanel.getFilesTable().selectAll();
      }
    });
    sleep(500);
    assertFalse(usButton.isEnabled());

  }
  

  /**
   * Stage All and UnStage All on newly created files.
   *  
   * @throws Exception If it fails.
   */
  @Test
  public void testStageUnstage_NewMultipleFiles() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_NewMultipleFiles_local";
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
  @Test
  public void testStageUnstage_NewFile_2() throws Exception {
    String localTestRepository = "target/test-resources/testStageUnstage_NewFile_2_local";
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

}
