package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTree;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.PanelRefresh;
import com.oxygenxml.git.view.event.Command;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.StageController;

/**
* Base for the test classes related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
public class FlatViewTestBase extends GitTestBase {
  /**
   * Access to the Git API.
   */
  protected final GitAccess gitAccess = GitAccess.getInstance();
  /**
   * Main frame.
   */
  protected JFrame mainFrame = null;
  /**
   * Staging/Unstaging panel.
   */
  protected StagingPanel stagingPanel;
  /**
   * Refresh support.
   */
  protected PanelRefresh refreshSupport;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    
    // Create the unstaged resources panel
    refreshSupport = new PanelRefresh();
    stagingPanel = new StagingPanel(
        refreshSupport,
        new StageController(),
        null) {
      @Override
      public PushPullController createPushPullController() {
        return new PushPullController() {
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
    
    if (mainFrame != null) {
      // Can be null if the setUp() failed.
      mainFrame.setVisible(false);
    }
  }
  
  /**
   * Pull through the push/pull controller.
   * 
   * @throws Exception If it fails.
   */
  protected void pull() throws Exception {
    // Execute pull command and wait for it to finish.
    stagingPanel.getPushPullController().execute(Command.PULL).get();
  }
  
  /**
   * Dumps the un-staged and stage models and asserts their content.
   * 
   * @param unstagedExpected Expected for the un-staged model.
   * @param indexExpected Expected for the staged model.
   */
  protected void assertTableModels(String unstagedExpected, String indexExpected) {
    flushAWT();
    sleep(200);
    
    ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
    JTable filesTable = unstagedChangesPanel.getFilesTable();
    StagingResourcesTableModel uModel = (StagingResourcesTableModel) filesTable.getModel();
    // The newly created file is present in the model.
    
    ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
    JTable stFilesTable = stagedChangesPanel.getFilesTable();
    StagingResourcesTableModel stModel = (StagingResourcesTableModel) stFilesTable.getModel();
    // The newly created file is present in the model.
    
    String ex = "--UNSTAGED--\n" + unstagedExpected + "\n\n--INDEX--\n" + indexExpected;
    String ac = "--UNSTAGED--\n" + getFlatModelDump(uModel) + "\n\n--INDEX--\n" + getFlatModelDump(stModel);

    assertEquals(ex, ac);
  }
  
  /**
   * Dumps the model as a string.
   * 
   * @param model Model to dump.
   * 
   * @return The model.
   */
  private String getFlatModelDump(StagingResourcesTableModel model) {
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
   * Dumps the un-staged and stage models and asserts their content.
   * 
   * @param unstagedExpected Expected for the un-staged model.
   * @param indexExpected Expected for the staged model.
   */
  protected void assertTreeModels(String unstagedExpected, String indexExpected) {
    flushAWT();
    sleep(200);
    
    ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
    JTree filesTable = unstagedChangesPanel.getTreeView();
    StagingResourcesTreeModel uModel = (StagingResourcesTreeModel) filesTable.getModel();
    // The newly created file is present in the model.
    
    ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
    JTree stFilesTable = stagedChangesPanel.getTreeView();
    StagingResourcesTreeModel stModel = (StagingResourcesTreeModel) stFilesTable.getModel();
    // The newly created file is present in the model.
    
    String ex = "--UNSTAGED--\n" + unstagedExpected + "\n\n--INDEX--\n" + indexExpected;
    String ac = "--UNSTAGED--\n" + getTreeModelDump(uModel) + "\n\n--INDEX--\n" + getTreeModelDump(stModel);

    assertEquals(ex, ac);
  }
  
  /**
   * Dumps the model as a string.
   * 
   * @param model Model to dump.
   * 
   * @return The model.
   */
  private String getTreeModelDump(StagingResourcesTreeModel model) {
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
   * Updates the information presented in the view with the current state of the working copy.   
   */
  protected final void refreshViews() {
    refreshSupport.call();
    sleep(500);
    flushAWT();
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
  protected final File commitNewFile(String parentDir, String fileName, String content) throws Exception {
    File file = createNewFile(parentDir, fileName, content);
    gitAccess.add(new FileStatus(GitChangeType.ADD, fileName));
    gitAccess.commit("First version.");
    
    return file;
  }
  
  protected final File createNewFile(String parentDir, String fileName, String content) throws IOException, Exception {
    new File(parentDir).mkdirs();
    File file = new File(parentDir + "/" + fileName);
    file.createNewFile();
    setFileContent(file, content);
    return file;
  }
}
