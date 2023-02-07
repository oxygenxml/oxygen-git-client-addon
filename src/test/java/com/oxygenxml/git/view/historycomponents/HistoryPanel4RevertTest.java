package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;

import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryCommitTableModel;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.history.HistoryStrategy;

public class HistoryPanel4RevertTest extends HistoryPanelTestBase {
  private static final GitController PUSH_PULL_CONTROLLER = new GitController(GitAccess.getInstance());

  int noOfRefreshes;
  @Override
  protected void setUpHistoryPanel() {
    noOfRefreshes = 0;
    
    // Initialize history panel.
    historyPanel = new HistoryPanel(PUSH_PULL_CONTROLLER) {
      @Override
      protected int getUpdateDelay() {
        return 0;
      }

      @Override
      public boolean isShowing() {
        // Branch related refresh is done only if the view is displayed.
        return true;
      }

      @Override
      public void scheduleRefreshHistory() {
        super.scheduleRefreshHistory();
        noOfRefreshes++;
      }
    };
  }

  /**
   * <p><b>Description:</b> create new branch starting from a commit from the history table.</p>
   *
   * @author Tudosie Razvan
   *
   * @throws Exception
   */
  @Test
  public void testRevertCommit() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");

    File wcTree = new File("target/gen/HistoryPanelTest/testAffectedFiles");
    RepoGenerationScript.generateRepository(script, wcTree);

    try {

      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
      String dump = dumpHistory(commitsCharacteristics);
      String expected = "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
          + "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
          + "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n"
          + "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n";
      expected = replaceDate(expected);
      assertEquals(expected, dump);

      historyPanel.showRepositoryHistory();
      flushAWT();

      JTable historyTable = historyPanel.getHistoryTable();
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
      dump = dumpHistory(model.getAllCommits());
      assertEquals(expected, dump);

      // Checkout commit as new branch
      CommitCharacteristics commitCharacteristics = model.getAllCommits().get(2);
      dump = dumpHistory(Arrays.asList(commitCharacteristics));
      expected = replaceDate("[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n");
      assertEquals(expected, dump);
      
      GitAccess.getInstance().revertCommit(commitCharacteristics.getCommitId());
      waitForScheduler();

      // Check the history of the new branch
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
      dump = dumpHistory(commitsCharacteristics);
      expected = "[ Revert \"Changes.\"\n"
          + "\n"
          + "This reverts commit "+ commitCharacteristics.getCommitId()+".\n"
          + " , {date} , AlexJitianu <alex_jitianu@sync.ro> , 5 , AlexJitianu , [1] ]\n"
          + "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
          + "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
          + "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n"
          + "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n";
      expected = replaceDate(expected);
      assertEquals(expected, dump);
      assertEquals(1, noOfRefreshes);
    } finally {
      GitAccess.getInstance().closeRepo();
      FileUtil.deleteRecursivelly(wcTree);
    }
  }
}
