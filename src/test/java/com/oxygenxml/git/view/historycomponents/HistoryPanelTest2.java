package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.MenuElement;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.GitController;

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
  
    try {
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics, true);
  
      String expected =  
          "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
  
      assertEquals(expected, dump);
  
      historyPanel.showRepositoryHistory();
  
      JTable historyTable = historyPanel.historyTable;
  
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
      
      StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) historyPanel.affectedFilesTable.getModel();
      FileStatus fileStatus = affectedFilesModel.getFilesStatuses().get(0);
      
      CommitCharacteristics cc = model.getAllCommits().get(0);
      
      List<Action> actions = getCompareWithPreviousAction(fileStatus, cc);
      
      Action action = actions.get(0);
      
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());

      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      assertEquals("git://" + model.getAllCommits().get(0).getCommitId() + "/file_renamed.txt", left.toString());
      assertEquals("git://" + model.getAllCommits().get(1).getCommitId() + "/file.txt", right.toString());
  
    } finally {
      GitAccess.getInstance().closeRepo();
  
    }
  
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
  
    try {
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics, true);
  
      String expected =  
          "[ Move , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Initial , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
  
      assertEquals(expected, dump);
  
      historyPanel.showRepositoryHistory();
  
      JTable historyTable = historyPanel.historyTable;
  
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
      
      StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) historyPanel.affectedFilesTable.getModel();
      FileStatus fileStatus = affectedFilesModel.getFilesStatuses().get(0);
      
      CommitCharacteristics cc = model.getAllCommits().get(0);
      
      List<Action> actions = getCompareWithPreviousAction(fileStatus, cc);
  
      assertFalse("Unable to find the 'Compare with previous version' action.", actions.isEmpty());
      
      Action action = actions.get(0);
      
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());
  
      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      assertEquals("git://" + model.getAllCommits().get(0).getCommitId() + "/child/file.txt", left.toString());
      assertEquals("git://" + model.getAllCommits().get(1).getCommitId() + "/file.txt", right.toString());
  
    } finally {
      GitAccess.getInstance().closeRepo();
  
    }
  
  }
}
