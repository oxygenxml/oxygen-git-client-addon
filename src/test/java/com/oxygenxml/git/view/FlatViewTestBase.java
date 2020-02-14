package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTree;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PushPullController;

/**
* Base for the test classes related to the actions performed
* on the staged/unstaged resources seen in the flat view.
*/
@Ignore
public class FlatViewTestBase extends GitTestBase { // NOSONAR
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(TreeViewTest.class);
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

  
  @Override
  public void setUp() throws Exception {
    super.setUp();


    stagingPanel = new StagingPanel(
        refreshSupport,
        new GitController(),
        null) {
      @Override
      public PushPullController createPushPullController() {
        return new PushPullController() {
          @Override
          protected void showPullSuccessfulWithConflicts(PullResponse response) {
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

  @Before
  public void tearDown() throws Exception {
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
    stagingPanel.getPushPullController().pull().get();
  }
  
  /**
   * Dumps the un-staged and stage models and asserts their content.
   * 
   * @param unstagedExpected Expected for the un-staged model.
   * @param indexExpected Expected for the staged model.
   */
  protected void assertTableModels(String unstagedExpected, String indexExpected) {
    flushAWT();
    
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
    sleep(300);
    
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
   *    
   * @throws InterruptedException 
   */
  protected final void refreshViews() throws InterruptedException {
    refreshSupport.call();
    
    waitForScheduler();
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
  
  protected void waitForScheluerBetter() {
    // TODO Alex This sequence is needed because of com.oxygenxml.git.view.CommitAndStatusPanel.commitButtonAndMessageUpdateTaskTimer
    // We can stop using this timer and switch to the scheduler.
    sleep(300);
    flushAWT();
    waitForScheduler();
  }
  
  /**
   * Adds a file in the git index.
   * 
   * @param fs File to add to the index.
   */
  protected void add(FileStatus fs) {
    gitAccess.add(fs);

    waitForSwitchUntracked2Index(fs);
  }

  /**
   * Waits for the UI to react after a state change from Untracked to Index.
   * 
   * @param fs Modified file.
   */
  protected void waitForSwitchUntracked2Index(FileStatus fs) {
    int i = 0;
    while(i< 10 && !uiReadyAfterSwitchUntracked2Index(fs)) {
      i++;
      flushAWT();
      Thread.yield();
      sleep(300);
    }
  }

  /**
   * Waits for the UI to react after a switch from Untracked to Index.
   * 
   * @param fs Modified file.
   * 
   * @return <code>true</code> if the UI reflects the state change.
   */
  private boolean uiReadyAfterSwitchUntracked2Index(FileStatus fs) {
    boolean uiready = true;
    ChangesPanel unstagedChangesPanel = stagingPanel.getUnstagedChangesPanel();
    
    for (FileStatus fileStatus : unstagedChangesPanel.getFilesStatuses()) {
      if (fs.getFileLocation().equals(fileStatus.getFileLocation())) {
        logger.warn("Still in the untracked area: " + fs);
        
        uiready = false;
        break;
      }
    }
    
    if (uiready) {
      uiready = false;
      ChangesPanel stagedChangesPanel = stagingPanel.getStagedChangesPanel();
      for (FileStatus fileStatus : stagedChangesPanel.getFilesStatuses()) {
        if (fs.getFileLocation().equals(fileStatus.getFileLocation())) {

          uiready = true;
          break;
        }
      }
      
      if (!uiready) {
        logger.warn("Not found in the index area: " + fs);
      }
    }
    
    return uiready;
  }
}
