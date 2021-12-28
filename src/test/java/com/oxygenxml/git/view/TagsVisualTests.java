package com.oxygenxml.git.view;

import java.io.File;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.ToolbarPanel;
import com.oxygenxml.git.view.tags.CreateTagDialog;
import com.oxygenxml.git.view.tags.GitTag;
import com.oxygenxml.git.view.tags.TagDetailsDialog;
import com.oxygenxml.git.view.tags.TagsDialog;
import com.oxygenxml.git.view.tags.TagsTableModel;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
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
   * Used to create some commits
   * 
   * @param numberOfCommits
   * 
   * @throws Exception 
   */
  private void createCommits(int numberOfCommits) throws Exception {
    
    File file = new File(LOCAL_REPO, "local.txt");
    file.createNewFile();
    for (int i = 0; i < numberOfCommits; i++) {
      setFileContent(file, "local content" + i);
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
      gitAccess.commit("Local commit." + i);
    }
    //Push the changes
    push("", "");
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
    
    createCommits(4);
    
    //Make 3 tags ( 2nd tag will be pushed )
    
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String commitIdForTag1 = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag1", "MesajTag1", commitIdForTag1);
    sleep(1000);
    
    String commitIdForTag2 = commitsCharacteristics.get(2).getCommitId();
    gitAccess.tagCommit("Tag2", "MesajTag2", commitIdForTag2);
    gitAccess.pushTag("Tag2");
    sleep(1000);
    
    String commitIdForTag3 = commitsCharacteristics.get(3).getCommitId();
    gitAccess.tagCommit("Tag3", "", commitIdForTag3);
    
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
      assertEquals(Tags.SHOW_TAGS, showTagsButton.getToolTipText());
      
      // Click the showTags Button and verify if the dialog is correct generated
      showTagsButton.doClick();
      sleep(50);
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
      
      //Verify 1st tag that should be the last in the table
      GitTag tag1 = tableModel.getItemAt(2);
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
      
      //Verify 3rd tag added (the most recent so it s first in the tags table)
      GitTag tag3 = tableModel.getItemAt(0);
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
      assertTrue(tag3.isPushed());
      
      final TagsDialog tagsFinalJDialog = showTagsJDialog;
      //Select last row and delete the tag and verify if the tag doesn't exist
      tagsTable.setRowSelectionInterval(2, 2);
      SwingUtilities.invokeLater(() -> tagsFinalJDialog.getDeleteButton().doClick());

      flushAWT();
      
      JDialog deleteDialog = findDialog(Tags.DELETE_TAG_DIALOG_TITLE);
      assertNotNull(deleteDialog);
      findFirstButton(deleteDialog, Tags.YES).doClick();
      flushAWT();
      assertFalse(gitAccess.existsTag("Tag1"));
      
      //Verify how many rows has the table left
      assertEquals(2, tagsTable.getRowCount());
      
      //Verify the tagDetails Dialog
      new TagDetailsDialog(tag3).setVisible(true);
      flushAWT();
      
      JDialog tagDetailsDialog = findDialog(Tags.TAG_DETAILS_DIALOG_TITLE);
      assertNotNull(tagDetailsDialog);
      JTextArea tagMessageArea = findFirstTextArea(tagDetailsDialog);
      assertEquals("", tagMessageArea.getText()); //Tag doesn't have a message
      
    } finally {
      frame.setVisible(false);
      frame.dispose();
    }
  }
  
  /**
   * <p><b>Description:</b> Tests that the "Create Tags" dialog can't have wrong title values</p>
   * <p><b>Bug ID:</b> EXM-46109</p>
   *
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */ 
  public void testCreateTagsDialog() throws Exception {
    
    createCommits(5);
    
    //Make 1 tag then make the others with the CreateTag Dialog

    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
    String commitIdForTag1 = commitsCharacteristics.get(0).getCommitId();
    gitAccess.tagCommit("Tag1", "MesajTag1", commitIdForTag1);

    String commitIdForTag2 = commitsCharacteristics.get(3).getCommitId();

    //Create a tag with a custom name and message and not push, after this create one pushed then verify if they were corectly added to the Show Dialog
    CreateTagDialog createTagDialog ;
    SwingUtilities.invokeLater(() -> {
      new CreateTagDialog();
    });
    
    flushAWT();
    createTagDialog = (CreateTagDialog) findDialog(Tags.CREATE_TAG);
    createTagDialog.getTagTitleField().setText("TagCreated");
    createTagDialog.getTagMessageField().setText("Mesaj Tag De Test");
    createTagDialog.setVisible(false);
    flushAWT();
    sleep(500);
    createTagDialog.getOkButton().doClick();
    
    String tagTitle = createTagDialog.getTagTitle();
    String tagMessage = createTagDialog.getTagMessage();
    if (createTagDialog.getResult() == OKCancelDialog.RESULT_OK) {
      gitAccess.tagCommit(tagTitle, tagMessage, commitIdForTag2);
      if(createTagDialog.shouldPushNewTag()) {
        gitAccess.pushTag(tagTitle);
      }
    }
    createTagDialog.dispose();
    
    flushAWT();
    
    SwingUtilities.invokeLater(() -> {
      new CreateTagDialog();
    });
    flushAWT();
    
    String commitIdForTag3 = commitsCharacteristics.get(2).getCommitId();

    CreateTagDialog createTagDialog2 = (CreateTagDialog) findDialog(Tags.CREATE_TAG);
    createTagDialog2.getTagTitleField().setText("Tag2Created");
    createTagDialog2.getTagMessageField().setText("Mesaj Tag De Test2");
    createTagDialog2.getPushCheckbox().setSelected(true);
    createTagDialog2.setVisible(false);
    flushAWT();
    sleep(500);
    createTagDialog2.getOkButton().doClick();
    
    String tagTitle2 = createTagDialog2.getTagTitle();
    String tagMessage2 = createTagDialog2.getTagMessage();
    if (createTagDialog2.getResult() == OKCancelDialog.RESULT_OK) {
      gitAccess.tagCommit(tagTitle2, tagMessage2, commitIdForTag3);
      if(createTagDialog2.shouldPushNewTag()) {
        gitAccess.pushTag(tagTitle2);
      }
    }
    
    //Verify if the tags were created
    assertTrue(gitAccess.existsTag("Tag1"));
    assertTrue(gitAccess.existsTag("TagCreated"));
    assertTrue(gitAccess.existsTag("Tag2Created"));
  }
  
}