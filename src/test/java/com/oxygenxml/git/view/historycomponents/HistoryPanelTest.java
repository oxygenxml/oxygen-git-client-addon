package com.oxygenxml.git.view.historycomponents;

import java.awt.Color;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.FileHistoryPresenter;
import com.oxygenxml.git.view.history.HistoryCommitTableModel;
import com.oxygenxml.git.view.staging.StagingResourcesTableCellRenderer;
import com.oxygenxml.git.view.util.UIUtil;

/**
 * UI level tests for history.
 *  
 * @author alex_jitianu
 */
public class HistoryPanelTest extends HistoryPanelTestBase {

  /**
   * Tests the affected files presented when a revision is selected inside the history panel.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testAffectedFiles() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");

    File wcTree = new File("target/gen/HistoryPanelTest/testAffectedFiles");
    RepoGenerationScript.generateRepository(script, wcTree);

    try {

      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dumpHistory(commitsCharacteristics);

      String expected =  
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
              "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
              "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
              "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
              "";

      expected = replaceDate(expected);

      assertEquals(
          expected, dump);

      historyPanel.showRepositoryHistory();
      waitForScheduler();
      flushAWT();
      sleep(300);

      JTable historyTable = historyPanel.getHistoryTable();

      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

      dump = dumpHistory(model.getAllCommits());

      assertEquals(
          expected, dump);

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, historyPanel.getAffectedFilesTable(), 0, "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=CHANGED, fileLocation=root.txt)\n");


      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, historyPanel.getAffectedFilesTable(), 1, "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=ADD, fileLocation=root.txt)\n");

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, historyPanel.getAffectedFilesTable(), 2, "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, 
          "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
          "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file4.txt)\n" + 
          "(changeType=RENAME, fileLocation=f2/file3_renamed.txt)\n" + 
          "");
      
      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, historyPanel.getAffectedFilesTable(), 3, "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, 
          "(changeType=ADD, fileLocation=f1/file1.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file2.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file3.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file4.txt)\n" + 
              "(changeType=ADD, fileLocation=newProject.xpr)\n" + 
          "");
      
    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }

  }
  
  
  /**
   * Tests the affected files foreground when a file or folder is searched.
   * <br><br>
   * EXM: EXM-48807
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testAffectedFilesForeground() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");

    File wcTree = new File("target/gen/HistoryPanelTest/testAffectedFiles");
    RepoGenerationScript.generateRepository(script, wcTree);

    try {

      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dumpHistory(commitsCharacteristics);

      String expected =  
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
              "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
              "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
              "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
              "";

      expected = replaceDate(expected);

      assertEquals(
          expected, dump);

      historyPanel.showRepositoryHistory();
      waitForScheduler();
      flushAWT();
      sleep(300);

      JTable historyTable = historyPanel.getHistoryTable();

      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

      dump = dumpHistory(model.getAllCommits());

      assertEquals(
          expected, dump);

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, historyPanel.getAffectedFilesTable(), 2, "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, 
          "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
          "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
          "(changeType=REMOVED, fileLocation=f2/file4.txt)\n" + 
          "(changeType=RENAME, fileLocation=f2/file3_renamed.txt)\n" + 
          "");
      
      JTable affectedFiles = historyPanel.getAffectedFilesTable();
      
      TableCellRenderer render = affectedFiles.getDefaultRenderer(FileStatus.class);
      
      FileHistoryPresenter presentedPath = new FileHistoryPresenter("f2/file4.txt");
      ((StagingResourcesTableCellRenderer)render).setPresentedFilePath(presentedPath);
      
      Color foregroundColor = render.getTableCellRendererComponent(
          affectedFiles, affectedFiles.getValueAt(0, 1), false, true, 0, 1).getForeground();
      assertEquals(UIUtil.NOT_SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      foregroundColor = render.getTableCellRendererComponent(
          affectedFiles, affectedFiles.getValueAt(1, 1), false, true, 1, 1).getForeground();
      assertEquals(UIUtil.NOT_SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      foregroundColor = render.getTableCellRendererComponent(
          affectedFiles, affectedFiles.getValueAt(2, 1), false, true, 2, 1).getForeground();
      assertEquals(UIUtil.SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      foregroundColor = render.getTableCellRendererComponent(
          affectedFiles, affectedFiles.getValueAt(3, 1), false, true, 3, 1).getForeground();
      assertEquals(UIUtil.NOT_SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      
      presentedPath.setFilePath("f2"); 
      for(int i = 0; i < affectedFiles.getColumnCount(); i++) {
        foregroundColor = render.getTableCellRendererComponent(
            affectedFiles, affectedFiles.getValueAt(i, 1), false, true, 1, 1).getForeground();
        assertEquals(UIUtil.SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      }
      
      presentedPath.setFilePath(null); 
      for(int i = 0; i < affectedFiles.getColumnCount(); i++) {
        foregroundColor = render.getTableCellRendererComponent(
            affectedFiles, affectedFiles.getValueAt(i, 1), false, true, 1, 1).getForeground();
        assertEquals(UIUtil.SEARCHED_FILES_COLOR_LIGHT_THEME, foregroundColor);
      }
      

    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }

  }
  

  /**
   * Changing branches fires notification.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testChangeBranchEvent() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/git_branch_events.txt");

    File wcTree = new File("target/gen/GitHistoryTest_testChangeBranchEvent");
    generateRepositoryAndLoad(script, wcTree);

    List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

    String dump = dumpHistory(commitsCharacteristics);

    String expected =  
        "[ New branch , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
            "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
            "";
    expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));

    expected = replaceDate(expected);

    assertEquals(
        expected, dump);

    historyPanel.showRepositoryHistory();
    waitForScheduler();
    flushAWT();
    sleep(300);

    JTable historyTable = historyPanel.getHistoryTable();

    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits());

    assertEquals(
        expected, dump);

    //=======================
    // Change branch.
    //=======================
    GitAccess.getInstance().setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    // History panel uses the scheduler to perform the change.
    ScheduledFuture<?> schedule = GitOperationScheduler.getInstance().schedule(() -> {});
    schedule.get();
    waitForScheduler();

    model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits());

    expected = "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n";
    expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
    assertEquals(expected, dump);
  }
}
