package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JTable;

import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryCommitTableModel;

/**
 * UI level tests for history.
 *  
 * @author alex_jitianu
 */
public class HistoryPanelTest2 extends HistoryPanelTestBase {

  /**
   * Tests the affected files presented when a revision is selected inside the history panel.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testAffectedFiles_ShowRenames() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics, true);
  
      String expected =  
          "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
  
      assertEquals(expected, dump);
  
      historyPanel.showRepositoryHistory();
  
      JTable historyTable = historyPanel.getAffectedFilesTable();
  
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
  
      dump = dumpHistory(model.getAllCommits(), true);
  
      assertEquals(expected, dump);
  
      //-----------
      // Select an entry in the revision table.
      //-----------
      expected = "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]";
      expected = replaceDate(expected);
      
      selectAndAssertRevision(historyTable, 0, expected);
  
      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=RENAME, fileLocation=file_renamed.txt)\n");
      
      //---------------
      // Invoke the Diff action to see if the built URLs are O.K.
      //---------------
      
      StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) historyPanel.getAffectedFilesTable().getModel();
      FileStatus fileStatus = affectedFilesModel.getFilesStatuses().get(0);
      
      CommitCharacteristics cc = model.getAllCommits().get(0);
      
      Action action = getCompareWithPreviousAction(fileStatus, cc);
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());

      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      assertEquals("git://" + model.getAllCommits().get(0).getCommitId() + "/file_renamed.txt", left.toString());
      assertEquals("git://" + model.getAllCommits().get(1).getCommitId() + "/file.txt", right.toString());
  
  }

  /**
   * Tests the affected files presented when a revision is selected inside the history panel.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testAffectedFiles_ShowCopyRenames() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_follow_move.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowCopyRenames"));
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics, true);
  
      String expected =  
          "[ Move , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Initial , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
  
      assertEquals(expected, dump);
  
      historyPanel.showRepositoryHistory();
  
      JTable historyTable = historyPanel.getAffectedFilesTable();
  
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
  
      dump = dumpHistory(model.getAllCommits(), true);
  
      assertEquals(expected, dump);
  
      //-----------
      // Select an entry in the revision table.
      //-----------
      expected = "[ Move , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]";
      expected = replaceDate(expected);
      
      selectAndAssertRevision(historyTable, 0, expected);
  
      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=RENAME, fileLocation=child/file.txt)\n" + 
          "");
      
      //---------------
      // Invoke the Diff action to see if the built URLs are O.K.
      //---------------
      
      StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) historyPanel.getAffectedFilesTable().getModel();
      FileStatus fileStatus = affectedFilesModel.getFilesStatuses().get(0);
      
      CommitCharacteristics cc = model.getAllCommits().get(0);
      
      Action action = getCompareWithPreviousAction(fileStatus, cc);
      
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());
  
      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      assertEquals("git://" + model.getAllCommits().get(0).getCommitId() + "/child/file.txt", left.toString());
      assertEquals("git://" + model.getAllCommits().get(1).getCommitId() + "/file.txt", right.toString());
  }
  
  /**
   * Contextual actions were not presented For revisions preceding a rename.
   * EXM-44300
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testActionsOnRenamedFile() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/EXM-44300/script.txt"), 
        new File("target/gen/HistoryPanelTest/testActionsOnRenamedFile"));

    List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

    String dump = dumpHistory(commitsCharacteristics, true);

    String expected =  
        "[ Fourth , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Third (Rename) , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
        "[ Second , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
        "[ First , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
        "";

    assertEquals(expected, dump);

    historyPanel.showRepositoryHistory();

    JTable historyTable = historyPanel.getAffectedFilesTable();

    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits(), true);

    assertEquals(expected, dump);

    //---------------
    // Assert the available actions.
    //---------------
    CommitCharacteristics cc = commitsCharacteristics.get(2);
    assertEquals("Second", cc.getCommitMessage());

    List<Action> actions = getAllActions(new FileStatus(GitChangeType.MODIFIED, "file_renamed.txt"), cc);
    
    List<Object> collect = actions.stream().map(t -> t.getValue(Action.NAME)).collect(Collectors.toList());
    
    assertEquals("[Open_file, Compare_file_with_previous_version, Compare_file_with_working_tree_version]", collect.toString());

  }

  /**
   * Tests the actions presented for multiple selection in the history panel.
   * 
   * EXM-44448
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testMultipleSelectionHistoryActions() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/file_content_script.txt"), 
        new File("target/gen/HistoryPanelTest/testMultipleSelectionHistoryActions"));

    List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

    String dump = dumpHistory(commitsCharacteristics, true);

    String expected =  
        "[ Third. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
            "[ Second. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
            "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + 
            "";

    assertEquals(expected, dump);

    historyPanel.showRepositoryHistory();

    JTable historyTable = historyPanel.getAffectedFilesTable();

    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits(), true);

    assertEquals(expected, dump);
    //---------------
    // Invoke the Diff action to see if the built URLs are O.K.
    //---------------
    CommitCharacteristics cc1 = model.getAllCommits().get(0);
    CommitCharacteristics cc3 = model.getAllCommits().get(2);

    FileStatus fileStatus = new FileStatus(GitChangeType.CHANGED, "file1.txt");
    Action action = getCompareWithEachOther(fileStatus , cc1, cc3);

    action.actionPerformed(null);

    assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());

    URL left = urls2compare.get(0);
    URL right = urls2compare.get(1);

    assertEquals("git://" + cc1.getCommitId() + "/file1.txt", left.toString());
    assertEquals("git://" + cc3.getCommitId() + "/file1.txt", right.toString());

    /////////////////
    // Test the open multiple files
    //////////////////
    CommitCharacteristics cc2 = model.getAllCommits().get(1);
    Action open = getOpenFileAction(fileStatus , cc1, cc2, cc3);
    open.actionPerformed(null);

    assertEquals(
        "[" + "git://" + cc1.getCommitId() + "/file1.txt" + ", " +
            "git://" + cc2.getCommitId() + "/file1.txt" + ", " + 
            "git://" + cc3.getCommitId() + "/file1.txt" + "]", toOpen.toString());
  }
}
