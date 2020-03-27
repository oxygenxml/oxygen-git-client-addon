package com.oxygenxml.git.view;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JTable;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class FlatView4Test extends FlatViewTestBase {
  
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
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
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

}
