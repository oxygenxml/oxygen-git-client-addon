/*
* Copyright (c) 2020 Syncro Soft SRL - All Rights Reserved.
*
* This file contains proprietary and confidential source code.
* Unauthorized copying of this file, via any medium, is strictly prohibited.
*/

package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTable;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryCommitTableModel;

/**
 * Tests the quick search function
 * 
 * @author mircea_badoi
 */
public class HistoryPanelQuickSearchTest extends HistoryPanelTestBase {
  
  /**
   * Tests search with a word that could not be found in the history list
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testSearchNoMatch() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
    
      historyPanel.showRepositoryHistory();
      flushAWT();
      
      JTable historyTable = historyPanel.getHistoryTable();
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      model.filterChanged("nimic");
      sleep(300);
      
      String dump = dumpHistory(model.getAllCommits(), true);
      assertEquals("", dump);
  }
  
  /**
   * Tests when we search with the author name and the message of the commit
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testSearchAuthorAndMessage() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
  
      historyPanel.showRepositoryHistory();
      waitForScheduler();
      flushAWT();
      sleep(300);
  
      JTable historyTable = historyPanel.getHistoryTable();
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      model.filterChanged("alex rename");
      sleep(700);
      
      String dump = dumpHistory(model.getAllCommits(), true);
      String expected = "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n";
      assertEquals(expected, dump);
      
  }
  
  /**
   * Tests multiple cases like user do not write in the search with the upper case(the message is in upper case), 
   * search for a message,  but not in the right order...
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testSearchMultipleCases() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
  
      historyPanel.showRepositoryHistory();
      flushAWT();
  
      JTable historyTable = historyPanel.getHistoryTable();
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      //we search the message, but the message is not with uppercase
      model.filterChanged("FIRST COMMIT");
      sleep(300);
      
      String dump = dumpHistory(model.getAllCommits(), true);
      String expected = "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , null ]\n";
      assertEquals(expected, dump);
      
      sleep(100);
      
      //we go back to the original list
      model.filterChanged("");
      sleep(300);
      
      dump = dumpHistory(model.getAllCommits(), true);
      String expectedAll = "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [1] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , null ]\n" + 
          "";
      assertEquals(expectedAll, dump);
      
      sleep(100);
      
      //we search for message but not in the right order 
      model.filterChanged("commit First");
      sleep(300);
      dump = dumpHistory(model.getAllCommits(), true);
      assertEquals(expected, dump);
      
  }
  
  
  /**
   * Tests if the commit details are correct.
   * <br><br>
   * EXM-48899
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testCommitDetails() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
  
      historyPanel.showRepositoryHistory();
      waitForScheduler();
      flushAWT();
      sleep(300);
  
      JTable historyTable = historyPanel.getHistoryTable();
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      model.filterChanged("alex rename");
      historyTable.setRowSelectionInterval(0, 0);
      sleep(300);
      
      JTable affectedFiles = historyPanel.getAffectedFilesTable();
      CommitCharacteristics commitDetails = ((HistoryCommitTableModel)historyTable.getModel()).getAllCommits().get(0);
      List<FileStatus> changes = null;
      if (GitAccess.UNCOMMITED_CHANGES != commitDetails) {
        try {
          changes = RevCommitUtil.getChangedFiles(commitDetails.getCommitId());
        } catch (GitAPIException | RevisionSyntaxException | IOException e) {
          e.printStackTrace();
        }
      } else {
        changes = GitAccess.getInstance().getUnstagedFiles();
      }
      assertEquals(changes.size(), affectedFiles.getRowCount());
      for(int i = 0; i < changes.size(); i++) {
        assertEquals(changes.get(i), affectedFiles.getValueAt(i, 1));
      }
  }
  
  
}
