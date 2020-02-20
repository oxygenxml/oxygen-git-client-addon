package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.ChangesPanel.ResourcesViewMode;
import com.oxygenxml.git.view.ChangesPanel.SelectedResourcesProvider;

/**
* Test cases related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class TreeView2Test extends FlatViewTestBase {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(TreeView2Test.class);
  
  @Override
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
    
    TreePath treePath = TreeFormatter.getTreePath(filesTree.getModel(), fileToSelect);
    filesTree.getSelectionModel().setSelectionPath(treePath);
    flushAWT();
    
    assertTrue(ssButton.isEnabled());
    ssButton.doClick();
    
    waitForScheduler();
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
    
    waitForScheduler();
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
    
    File file = createNewFile(localTestRepository, "test.txt", "remote");
    
    // Create repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Add it to the index.
    add(new FileStatus(GitChangeType.ADD, "test.txt"));
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
        stagingPanel.getStageController());
    
    discardAction.actionPerformed(null);
    
    waitForScheduler();
    assertTreeModels(
        "", 
        "");    
  }
}
