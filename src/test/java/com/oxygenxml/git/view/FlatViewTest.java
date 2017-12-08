package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;

import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.StageController;

/**
 * Tests the flat table view.
 * 
 * @author alex_jitianu
 */
public class FlatViewTest extends GitTestBase {
  /**
   * Access to the Git API.
   */
  private final GitAccess gitAccess = GitAccess.getInstance();
  
  private JFrame mainFrame = null;

  private StagingPanel stagingPanel;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    
    // Create the unstaged resources panel
    PanelRefresh refreshSupport = new PanelRefresh(Translator.getInstance());
    stagingPanel = new StagingPanel(
        Translator.getInstance(),
        refreshSupport,
        new StageController(gitAccess)) {
      @Override
      public PushPullController createPushPullController(GitAccess gitAccess) {
        return new PushPullController(gitAccess, Translator.getInstance()) {
          @Override
          protected void showPullConflicts(PullResponse response) {
            // Nothing to do.
          }
        };
      }
    };
    // TODO: Maybe move this init on the StagingPanel constructor. 
    // Careful not to create a cycle.
    refreshSupport.setPanel(stagingPanel);
    
    mainFrame = new JFrame("Git test");
    mainFrame.getContentPane().add(stagingPanel, BorderLayout.CENTER);
    mainFrame.setSize(new Dimension(400, 600));
    mainFrame.setVisible(true);
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    
    mainFrame.setVisible(false);
  }

  /**
   * Pull through the push/pull controller.
   * 
   * @throws Exception If it fails.
   */
  private void pull() throws Exception {
    Thread execute = stagingPanel.getPushPullController().execute(Command.PULL);
    execute.join();
  }
  
  /**
   * Dumps the model as a string.
   * 
   * @param model Model to dump.
   * 
   * @return The model.
   */
  private String getTreeModelDump(StagingResourcesTableModel model) {
    StringBuilder sb = new StringBuilder();
    for (FileStatus fileStatus : model.getFilesStatuses()) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(fileStatus.getChangeType()).append(", ").append(fileStatus.getFileLocation());
    }
    return sb.toString();
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
  
  /**
   * Dumps the un-staged and stage models and asserts their content.
   * 
   * @param unstagedExpected Expected for the un-staged model.
   * @param indexExpected Expected for the staged model.
   */
  private void assertModels(String unstagedExpected, String indexExpected) {
    ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
    JTable filesTable = unstagedChangesPanel.getFilesTable();
    StagingResourcesTableModel uModel = (StagingResourcesTableModel) filesTable.getModel();
    
    // The newly created file is present in the model.
    assertEquals("Unstaged area", unstagedExpected, getTreeModelDump(uModel));
    
    ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
    JTable stFilesTable = stagedChangesPanel.getFilesTable();
    StagingResourcesTableModel stModel = (StagingResourcesTableModel) stFilesTable.getModel();
    // The newly created file is present in the model.
    assertEquals("Index area", indexExpected, getTreeModelDump(stModel));
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
    assertModels(
        "UNTRACKED, test.txt",
        "");

    //---------------
    // Stage.
    //---------------
    change(true, 0);
    // The file has moved to the INDEX.
    assertModels("", "ADD, test.txt");

    //---------------
    // Back to unStaged
    //---------------
    change(false, 0);
    assertModels("UNTRACKED, test.txt", "");
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
    assertModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertModels("MODIFIED, test.txt", "");
    
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertModels("", "CHANGED, test.txt");
    
    // Change the file.
    setFileContent(file, "modified content");
    
    assertModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    // Discard.
    DiscardAction discardAction = new DiscardAction(
        Arrays.asList(new FileStatus(GitChangeType.MODIFIED, "test.txt")),
        stagingPanel.getStageController());
    discardAction.actionPerformed(null);
    assertModels(
        "", 
        "");    
  }

  /**
   * Resolve a conflict using mine copy.
   * 
   * Resolve using mine
   * Resolve using theirs
   * Restart merge
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

    assertModels("CONFLICT, test.txt", "");
    
    stagingPanel.getStageController().doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    
    assertModels("", "");

    // Check the commit.
    CommitPanel commitPanel = stagingPanel.getCommitPanel();
    assertEquals("Conclude_Merge_Message", commitPanel.getCommitMessage().getText());
    
    commitPanel.getCommitButton().doClick();
    flushAWT();
    
    assertEquals("", "");
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
    assertModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertModels("MODIFIED, test.txt", "");
    //------------
    // Add to INDEX (Stage)
    //------------
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertModels("", "CHANGED, test.txt");
    
    //-----------------
    // Change the file again. It will appear in the index as well.
    //------------------
    setFileContent(file, "modified content");
    
    assertModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    //------------------
    // Unstage the file from the INDEX.
    //------------------
    change(false, 0);
    
    assertModels(
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
    assertModels(
        "UNTRACKED, test.txt\n" + 
        "UNTRACKED, test2.txt", 
        "");
    
    changeAll(true);
    
    // The newly created file is present in the model.
    assertModels(
        "", 
        "ADD, test.txt\n" + 
        "ADD, test2.txt");
    
    //--------------
    // Back to unStaged
    //---------------
    changeAll(false);
    
    // The newly created file is present in the model.
    assertModels(
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
    assertModels("UNTRACKED, test.txt","");

    change(true, 0);
    
    // The file has moved to the INDEX.
    assertModels("", "ADD, test.txt");
  
    //----------------
    // Change the file again.
    //----------------
    setFileContent(file, "new content");
    assertModels("MODIFIED, test.txt", "ADD, test.txt");
    
    //--------------
    // Back to unstaged
    //---------------
    change(false, 0);
    
    assertModels("UNTRACKED, test.txt","");
  }
  
 
}
