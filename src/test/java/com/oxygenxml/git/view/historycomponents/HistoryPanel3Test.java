package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.script.RepoGenerationScript;

/**
 * UI level tests for history.
 */
public class HistoryPanel3Test extends HistoryPanelTestBase {

  /**
   * <p><b>Description:</b> create new branch starting from a commit from the history table.</p>
   * <p><b>Bug ID:</b> EXM-45710-</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testCreateBranchFromCommit() throws Exception {
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
              "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n";
      expected = replaceDate(expected);
      assertEquals(expected, dump);

      historyPanel.showRepositoryHistory();
      flushAWT();

      JTable historyTable = historyPanel.historyTable;
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      dump = dumpHistory(model.getAllCommits());
      assertEquals(expected, dump);

      // Checkout commit as new branch
      CommitCharacteristics commitCharacteristics = model.getAllCommits().get(2);
      dump = dumpHistory(Arrays.asList(commitCharacteristics));
      expected = replaceDate("[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n");
      assertEquals(expected, dump);
      GitAccess.getInstance().checkoutCommitAndCreateBranch("new_branch", commitCharacteristics.getCommitId());
      waitForScheduler();
      sleep(400);
      assertEquals("new_branch", GitAccess.getInstance().getBranchInfo().getBranchName());
      
      // Check the history of the new branch
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
      dump = dumpHistory(commitsCharacteristics);
      expected =  
              "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
              "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n";
      expected = replaceDate(expected);
      assertEquals(expected, dump);
    } finally {
      GitAccess.getInstance().closeRepo();
      FileUtils.deleteDirectory(wcTree);
    }

  }
}
