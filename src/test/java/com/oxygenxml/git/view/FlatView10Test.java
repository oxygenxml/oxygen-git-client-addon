package com.oxygenxml.git.view;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ConflictButtonsPanel;
import com.oxygenxml.git.view.staging.FlatViewTestBase;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

public class FlatView10Test extends FlatViewTestBase {

  /**
   * <p><b>Description:</b> show/hide/click "Abort merge" button.</p>
   * <p><b>Bug ID:</b> EXM-46222</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testAbortMergeButton() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowHideAbortMergeButton-local-1";
    String localTestRepository_2 = "target/test-resources/testShowHideAbortMergeButton-local-2";
    String remoteTestRepository = "target/test-resources/testShowHideAbortMergeButton-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    GitAccess.getInstance().add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    GitAccess.getInstance().commit("First commit.");
    push("", "");
    
    //----------------- REPO 2
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    flushAWT();
    Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).until(() -> 
      !secondRepoFile.exists());
    pull("", "", PullType.MERGE_FF, false);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    GitAccess.getInstance().commit("Second commit.");
    push("", "");
    
    //--------------  REPO 1
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    GitAccess.getInstance().commit("Third commit.");
    
    // Now pull to generate conflict
    ConflictButtonsPanel abortMergeButtonPanel[] = { stagingPanel.getConflictButtonsPanel() };
    
    assertFalse(abortMergeButtonPanel[0].isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.MERGE_FF, false);
    refreshSupport.call();
    waitForScheduler();
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    RepositoryState repositoryState = GitAccess.getInstance().getRepository().getRepositoryState();
    assertEquals(RepositoryState.MERGING, repositoryState);
    assertTrue(abortMergeButtonPanel[0].isShowing());

    // --------------- REPO 2
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_2);
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
        !abortMergeButtonPanel[0].isShowing()); 
    
    // --------------- REPO 1
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_1);
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
    abortMergeButtonPanel[0].isShowing()); 
    JButton abortMergeBtn = findFirstButton(abortMergeButtonPanel[0], Tags.ABORT_MERGE);
    assertNotNull(abortMergeBtn);
    
    // Resolve using mine
    GitControllerBase gitCtrl = stagingPanel.getGitController();
    
    PluginWorkspace spy = Mockito.spy(PluginWorkspaceProvider.getPluginWorkspace());
    Mockito.when(spy.showConfirmDialog(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(0);
    PluginWorkspaceProvider.setPluginWorkspace(spy);
    
    
    FileStatus testFileStatus = new FileStatus(GitChangeType.CONFLICT, "test.txt");
    gitCtrl.asyncResolveUsingMine(Arrays.asList(testFileStatus));
    refreshSupport.call();
    waitForScheduler();
    flushAWT();

    abortMergeButtonPanel[0] = stagingPanel.getConflictButtonsPanel();
    assertFalse(abortMergeButtonPanel[0].isShowing());
    
    // Restart merge
    GitAccess.getInstance().restartMerge();
    flushAWT();
    sleep(300);
    
    abortMergeButtonPanel[0] = stagingPanel.getConflictButtonsPanel();
    assertTrue(abortMergeButtonPanel[0].isShowing());
    abortMergeBtn = findFirstButton(abortMergeButtonPanel[0], Tags.ABORT_MERGE);
    assertNotNull(abortMergeBtn);
    
    // Resolve using theirs
    gitCtrl.asyncResolveUsingTheirs(Arrays.asList(testFileStatus));
    waitForScheduler();
    flushAWT();

    abortMergeButtonPanel[0] = stagingPanel.getConflictButtonsPanel();
    assertFalse(abortMergeButtonPanel[0].isShowing());
    
    // Restart merge
    GitAccess.getInstance().restartMerge();
    flushAWT();
    sleep(300);
    
    abortMergeButtonPanel[0] = stagingPanel.getConflictButtonsPanel();
    assertTrue(abortMergeButtonPanel[0].isShowing());
    abortMergeBtn = findFirstButton(abortMergeButtonPanel[0], Tags.ABORT_MERGE);
    assertNotNull(abortMergeBtn);
    repositoryState = GitAccess.getInstance().getRepository().getRepositoryState();
    assertEquals(RepositoryState.MERGING, repositoryState);
    assertEquals(1, GitAccess.getInstance().getPullsBehind());
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
    // Abort merge
    abortMergeBtn.doClick();
    flushAWT();
    abortMergeButtonPanel[0] = stagingPanel.getConflictButtonsPanel();
    assertFalse(abortMergeButtonPanel[0].isShowing());
    
    repositoryState = GitAccess.getInstance().getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    assertEquals(1, GitAccess.getInstance().getPullsBehind());
    assertEquals(1, GitAccess.getInstance().getPushesAhead());
    
  }
  
}
