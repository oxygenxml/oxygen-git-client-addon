package com.oxygenxml.git.view.staging;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class TreeView2Test extends FlatViewTestBase {
  
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.TREE_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.TREE_VIEW);
  }
  
  /**
   * Discard changes. THose files must not appear in either stage/un-stage area.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testDiscard() throws Exception {
    /**
     * Local repository location.
     */
    String localTestRepository = "target/test-resources/testDiscard_NewFile_local";
    
    /**
     * Remote repository location.
     */
    String remoteTestRepository = "target/test-resources/testDiscard_NewFile_remote";
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    File file = createNewFile(localTestRepository, "test.txt", "remote");
    // Add it to the index.
    add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    assertTreeModels("", "ADD, test.txt");
    
    gitAccess.commit("First version.");
    
    assertTreeModels("", "");
    
    // Change the file.
    setFileContent(file, "index content");
    
    assertTreeModels("MODIFIED, test.txt", "");
    
    add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    
    assertTreeModels("", "CHANGED, test.txt");
    
    // Change the file.
    setFileContent(file, "modified content");
    
    // The file is present in  both areas.
    assertTreeModels(
        "MODIFIED, test.txt", 
        "CHANGED, test.txt");
    
    // Discard.
    DiscardAction discardAction = new DiscardAction(
        new SelectedResourcesProvider() {
          @Override
          public List<FileStatus> getOnlySelectedLeaves() {
            return null;
          }
          @Override
          public List<FileStatus> getAllSelectedResources() {
            return Arrays.asList(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
          }
        },
        stagingPanel.getGitController());
    
    discardAction.actionPerformed(null);
    
    waitForScheduler();
    assertTreeModels(
        "", 
        "");    
  }
}
