package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;

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
    
    waitForScheduler();
  }
  
  protected final void switchToView(ResourcesViewMode viewMode) {
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(viewMode);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(viewMode);
  }
  
  /**
   * Stage a file that has been renamed only by changing the letter case.
   * 
   * EXM-41394
   *  
   * @throws Exception If it fails.
   */
  @Test
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
    add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    assertTableModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTableModels("", "");
    
    //---------------------------
    originalFile.renameTo(new File(localTestRepository + "/Test.txt"));
    refreshSupport.call();
    
    waitForScheduler();

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

}
