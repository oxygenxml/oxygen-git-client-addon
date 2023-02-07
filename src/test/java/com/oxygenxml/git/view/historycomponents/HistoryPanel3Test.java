package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.google.common.base.Supplier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.utils.FileUtil;
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
      public void scheduleRefreshHistory() {
        noOfRefreshes++;
        super.scheduleRefreshHistory();
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
      final Supplier<String> branchName = new Supplier<String>() { 
        @Override
        public String get() {
          try {
            return GitAccess.getInstance().getRepository().getBranch();
          } catch (IOException | NoRepositorySelected e) {
            return null;
          }
        }
      };
      GitAccess.getInstance().checkoutCommitAndCreateBranch("new_branch", commitCharacteristics.getCommitId());
      waitForScheduler();
      Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).until(() -> 
        "new_branch".equals(branchName.get())
      );
   
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
      FileUtil.deleteRecursivelly(wcTree);
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
       
      final int initialNoOfRefreshes[] = { 0 };
      assertEquals(initialNoOfRefreshes[0], noOfRefreshes);
      
      File file = new File(repoDir, "textFile.txt");
      setFileContent(file, "BLA");
     
      // assertEquals(initialNoOfRefreshes + 1, noOfRefreshes);
      
      GitAccess.getInstance().add(new FileStatus(GitChangeType.ADD, "textFile.txt"));
      GitAccess.getInstance().commit("Another commit");
      initialNoOfRefreshes[0]++;
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() ->initialNoOfRefreshes[0]
          == noOfRefreshes);
      
      refreshSupport.setHistoryPanel(historyPanel);
      refreshSupport.call();
      
      initialNoOfRefreshes[0]++;
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() ->initialNoOfRefreshes[0]
          == noOfRefreshes);
      
      GitAccess.getInstance().createBranch("new_branch");
      initialNoOfRefreshes[0]++;
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> initialNoOfRefreshes[0] 
          == noOfRefreshes);
      
      GitAccess.getInstance().deleteBranch("new_branch");
      initialNoOfRefreshes[0]++;
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> initialNoOfRefreshes[0] 
          == noOfRefreshes);
      
      final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();
      RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
      URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
      remoteConfig.addURI(uri);
      remoteConfig.update(config);
      config.save();
      
      PUSH_PULL_CONTROLLER.push();
      initialNoOfRefreshes[0]++;
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> initialNoOfRefreshes[0] 
          == noOfRefreshes);
      
    } finally {
      GitAccess.getInstance().closeRepo();
      sleep(100);
      FileUtil.deleteRecursivelly(repoDir);
    }
  }
  
}
