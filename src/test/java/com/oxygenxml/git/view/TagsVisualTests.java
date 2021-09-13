package com.oxygenxml.git.view;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.ToolbarPanel;
import com.oxygenxml.git.view.tags.GitTag;
import com.oxygenxml.git.view.tags.TagsDialog;
import com.oxygenxml.git.view.tags.TagsTableModel;

import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Class for testing Visual Git Tags
 * 
 * @author gabriel_nedianu
 *
 */
public class TagsVisualTests extends GitTestBase {

  private final static String LOCAL_REPO = "target/test-resources/GitTags/localRepository";
  private final static String REMOTE_REPO = "target/test-resources/GitTags/remoteRepository";
  private final static String LOCAL_BRANCH = "LocalBranch";

  /**
   * The git acces
   */
  private GitAccess gitAccess;
  
  /**
   * Staging panel
   */
  private StagingPanel stagingPanel;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();

    //Creates the remote repository.
    createRepository(REMOTE_REPO);
    Repository remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(LOCAL_REPO);
    Repository localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }
  

  /**
   * <p><b>Description:</b> Tests the "Show Tags" button basic characteristics and the "Show details" functionality.</p>
   * <p><b>Bug ID:</b> EXM-46109</p>
   *
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */ 
  public void testTagsDialog() throws Exception {
    //Make the first commit for the local repository
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    //Make the 2nd commit on local
    setFileContent(file, "local content 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Second local commit.");
    
    //Make the 3rd commit on local
    setFileContent(file, "local content 3");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Third local commit.");
    
    //Make the 4th commit on local
    setFileContent(file, "local content 4");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Fourth local commit.");
    
    //Push the changes
    push("", "");
    
    //Make 3 tags ( 2nd tag will be pushed )
    
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    String commitIdForTag1 = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag1", "MesajTag1", commitIdForTag1);
    
    String commitIdForTag2 = commitsCharacteristics.get(2).getCommitId();
    gitAccess.tagCommit("Tag2", "MesajTag2", commitIdForTag2);
    gitAccess.pushTag("Tag2");
    
    String commitIdForTag3 = commitsCharacteristics.get(3).getCommitId();
    gitAccess.tagCommit("Tag3", "", commitIdForTag3);
    
    System.out.println("Tag1 commitId: " + commitIdForTag1);
    System.out.println("Tag2 commitId: " + commitIdForTag2);
    System.out.println("Tag3 commitId: " + commitIdForTag3);
    
    JFrame frame = new JFrame();
   
    try {
      // Init UI
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, null);
      ToolbarPanel toolbarPanel = stagingPanel.getToolbarPanel();
      frame.getContentPane().add(stagingPanel);
      frame.pack();
      frame.setVisible(true);
      flushAWT();
      toolbarPanel.refresh();
      refreshSupport.call();
      flushAWT();
      
      ToolbarButton showTagsButton = toolbarPanel.getShowTagsButton();
      // Test the "Show Tags" button tooltip text
      assertEquals(Tags.TOOLBAR_PANEL_TAGS_TOOLTIP, showTagsButton.getToolTipText());
      
      // Click the showTags Button and verify if the dialog is correct generated
      showTagsButton.doClick();
      flushAWT();
      
      JDialog tagsDialog = findDialog(Tags.TAGS_DIALOG);
      assertNotNull(tagsDialog);
      TagsDialog showTagsJDialog = null;
      
      if (tagsDialog instanceof TagsDialog) {
        showTagsJDialog = (TagsDialog) tagsDialog;
      }
      
      assertNotNull(showTagsJDialog);
      JTable tagsTable = showTagsJDialog.getTagsTable();
      //Should have 3 tags
      assertEquals(3, tagsTable.getModel().getRowCount());
      
      //Get the model of the tags table and verify the tags
      TagsTableModel tableModel = (TagsTableModel) tagsTable.getModel();
      
      //Verify 1st tag
      GitTag tag1 = tableModel.getItemAt(0);
      assertEquals(commitIdForTag1, tag1.getCommitID());
      assertEquals("Tag1", tag1.getName());
      assertEquals("MesajTag1", tag1.getMessage());
      assertFalse(tag1.isPushed());
      
      //2nd tag in the table should be pushed
      GitTag tag2 = tableModel.getItemAt(1);
      assertEquals(commitIdForTag2, tag2.getCommitID());
      assertEquals("Tag2", tag2.getName());
      assertEquals("MesajTag2", tag2.getMessage());
      assertTrue(tag2.isPushed());
      
      //Verify 3rd tag
      GitTag tag3 = tableModel.getItemAt(2);
      assertEquals(commitIdForTag3, tag3.getCommitID());
      assertEquals("Tag3", tag3.getName());
      assertEquals("", tag3.getMessage());
      assertFalse(tag3.isPushed());

      //Select first row and verify if the push and delete buttons are enabled
      tagsTable.setRowSelectionInterval(0, 0);
      assertTrue(showTagsJDialog.getPushButton().isEnabled());
      assertTrue(showTagsJDialog.getDeleteButton().isEnabled());
      
      //Select 2nd row and verify if the buttons are not enabled
      tagsTable.setRowSelectionInterval(1, 1);
      assertFalse(showTagsJDialog.getPushButton().isEnabled());
      assertFalse(showTagsJDialog.getDeleteButton().isEnabled());
      
      //Select first row and push the tag and verify if the buttons are not enabled and the tag was pushed
      tagsTable.setRowSelectionInterval(0, 0);
      showTagsJDialog.getPushButton().doClick();
      assertFalse(showTagsJDialog.getPushButton().isEnabled());
      assertFalse(showTagsJDialog.getDeleteButton().isEnabled());
      assertTrue(tag1.isPushed());
      
      //Select last row and delete the tag and verify if the tag doesn't exist
      tagsTable.setRowSelectionInterval(2, 2);
      showTagsJDialog.getDeleteButton().doClick();
      assertFalse(gitAccess.existsTag("Tag3"));
      
      //Verify how many rows has the table left
      assertEquals(2, tagsTable.getRowCount());
      
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  /**
   * Used to free up test resources.
   */
  @After
  public void freeResources() {
    gitAccess.closeRepo();
    File dirToDelete = new File(REMOTE_REPO);
    File dirToDelete2 = new File(LOCAL_REPO);
    try {
      FileUtils.deleteDirectory(dirToDelete);
      FileUtils.deleteDirectory(dirToDelete2);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}