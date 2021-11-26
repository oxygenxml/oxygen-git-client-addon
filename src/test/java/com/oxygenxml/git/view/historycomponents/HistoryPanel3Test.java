package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryCommitTableModel;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.history.HistoryStrategy;

/**
 * UI level tests for history.
 */
public class HistoryPanel3Test extends HistoryPanelTestBase {
  
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
      public void refresh() {
        super.refresh();
        noOfRefreshes++;
      }
    };
  }

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

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
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

      JTable historyTable = historyPanel.getHistoryTable();
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
      commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, null);
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
  
  /**
   * <p><b>Description:</b> test automatic refresh on various actions.</p>
   * <p><b>Bug ID:</b> EXM-44998</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testAutomaticRefreshOnVariousActions() throws Exception {
    Repository localRepo = createRepository("target/gen/HistoryPanelTest/testAffectedFiles_local");
    Repository remoteRepo = createRepository("target/gen/HistoryPanelTest/testAffectedFiles_remote");
    bindLocalToRemote(GitAccess.getInstance().getRepository(), remoteRepo);
    File repoDir = localRepo.getDirectory();
    try {
      
      GitAccess.getInstance().setRepositorySynchronously(repoDir.getParent());

      int initialNoOfRefreshes = 0;
      assertEquals(initialNoOfRefreshes, noOfRefreshes);
      
      File file = new File(repoDir, "textFile.txt");
      setFileContent(file, "BLA");
      assertEquals(initialNoOfRefreshes + 1, noOfRefreshes);
      
      GitAccess.getInstance().add(new FileStatus(GitChangeType.ADD, "textFile.txt"));
      GitAccess.getInstance().commit("Another commit");
      assertEquals(initialNoOfRefreshes + 2, noOfRefreshes);
      
      refreshSupport.setHistoryPanel(historyPanel);
      refreshSupport.call();
      sleep(1000);
      assertEquals(initialNoOfRefreshes + 3, noOfRefreshes);
      
      GitAccess.getInstance().createBranch("new_branch");
      assertEquals(initialNoOfRefreshes + 4, noOfRefreshes);
      
      GitAccess.getInstance().deleteBranch("new_branch");
      assertEquals(initialNoOfRefreshes + 5, noOfRefreshes);
      
      final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();
      RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
      URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
      remoteConfig.addURI(uri);
      remoteConfig.update(config);
      config.save();
      
      PUSH_PULL_CONTROLLER.push();
      sleep(700);
      assertEquals(initialNoOfRefreshes + 6, noOfRefreshes);
      
    } finally {
      GitAccess.getInstance().closeRepo();
      sleep(1000);
      FileUtils.deleteDirectory(repoDir);
    }
  }
  
}
