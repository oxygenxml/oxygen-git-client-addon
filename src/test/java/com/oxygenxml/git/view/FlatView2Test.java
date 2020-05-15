package com.oxygenxml.git.view;

import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class FlatView2Test extends FlatViewTestBase {

  /**
   * <p><b>Description:</b> lock fail test.</p>
   * <p><b>Bug ID:</b> EXM-42867</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testAPullCannotLockRef() throws Exception {
    PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    try {
      final boolean[] showErrorMessageCalled = new boolean[] {false};
      
      StandalonePluginWorkspace pluginWorkspaceMock = Mockito.mock(StandalonePluginWorkspace.class);
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          showErrorMessageCalled[0] = true;
          return null;
        }
      }).when(pluginWorkspaceMock).showErrorMessage(Mockito.anyString());
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspaceMock);
      
      String localTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_local_pullCannotLock";
      String remoteTestRepository = "target/test-resources/testStageUnstage_ModifiedFile_remote_pullCannotLock";

      // Create repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo = createRepository(localTestRepository);
      bindLocalToRemote(localRepo , remoteRepo);

      // Create a new file and push it.
      String fileName = "test.txt";
      File file = commitNewFile(localTestRepository, fileName, "content");
      PushResponse push = gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Create lock files
      String repoDir = gitAccess.getRepository().getDirectory().getAbsolutePath();
      Ref ref = gitAccess.getRemoteBrachListForCurrentRepo().get(0);
      File lockFile = new File(repoDir, ref.getName() + ".lock");
      boolean createNewFile = lockFile.createNewFile();
      assertTrue("Unnable to create lock file " + lockFile.getAbsolutePath(), createNewFile);
      setFileContent(lockFile, gitAccess.getLastLocalCommit().getName());

      // Commit a new version of the file.
      setFileContent(file, "modified");
      gitAccess.add(new FileStatus(GitChangeType.MODIFIED, fileName));
      gitAccess.commit("modified");
      gitAccess.push("", "");
      assertEquals("status: OK message null", push.toString());

      // Pull should throw "Lock failed" error
      PullResponse pullResponse = gitAccess.pull("", "");
      assertEquals(PullStatus.LOCK_FAILED, pullResponse.getStatus());
      assertTrue(showErrorMessageCalled[0]);
      Future<?> execute = stagingPanel.getPushPullController().pull();
      execute.get();
      flushAWT();
      assertEquals("Lock_failed", stagingPanel.getCommitPanel().getStatusLabel().getText());
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    }
  }
  
  /**
   * <p><b>Description:</b> Show and hide the rebase panel when changing the repository.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowHideRebasePanelWhenChangingRepo() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowRebasePanel_thenAbort-local-1";
    String localTestRepository_2 = "target/test-resources/testShowRebasePanel_thenAbort-local-2";
    String remoteTestRepository = "target/test-resources/testShowRebasePanel_thenAbort-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First commit.");
    gitAccess.push("", "");
    
    //----------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    flushAWT();
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    gitAccess.pull("", "", PullType.REBASE);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit.");
    gitAccess.push("", "");
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Third commit.");
    
    // Now pull to generate conflict
    RebasePanel rebasePanel = stagingPanel.getRebasePanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
    refreshSupport.call();
    waitForScheduler();
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    // --------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    sleep(300);
    assertFalse(rebasePanel.isShowing());
    
    // --------------- REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    sleep(300);
    assertTrue(rebasePanel.isShowing());
  }
  
  /**
   * <p><b>Description:</b> Show and hide (by aborting the rebase) the rebase panel.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowRebasePanel_thenAbort() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowRebasePanel_thenAbort-local-1";
    String localTestRepository_2 = "target/test-resources/testShowRebasePanel_thenAbort-local-2";
    String remoteTestRepository = "target/test-resources/testShowRebasePanel_thenAbort-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First commit.");
    gitAccess.push("", "");
    
    //----------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    flushAWT();
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    gitAccess.pull("", "", PullType.REBASE);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit.");
    gitAccess.push("", "");
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Third commit.");
    
    // Now pull to generate conflict
    RebasePanel rebasePanel = stagingPanel.getRebasePanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
    refreshSupport.call();
    waitForScheduler();
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    JButton abortButton = null;
    Component[] components = rebasePanel.getComponents();
    for (Component component : components) {
      if (component instanceof JButton) {
        JButton button = (JButton) component;
        if (button.getText().equals("Abort_rebase")) {
          abortButton = button;
          break;
        }
      }
    }
    assertNotNull(abortButton);
    
    abortButton.doClick();
    flushAWT();
    sleep(500);
    
    assertFalse(rebasePanel.isShowing());
  }
  
  /**
   * <p><b>Description:</b> Show and hide (by continuing the rebase) the rebase panel.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowRebasePanel_thenContinue() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowRebasePanel_thenContinue-local-1";
    String localTestRepository_2 = "target/test-resources/testShowRebasePanel_thenContinue-local-2";
    String remoteTestRepository = "target/test-resources/testShowRebasePanel_thenContinue-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First commit.");
    gitAccess.push("", "");
    
    //----------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    flushAWT();
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    gitAccess.pull("", "", PullType.REBASE);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit.");
    gitAccess.push("", "");
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Third commit.");
    
    // Now pull to generate conflict
    RebasePanel rebasePanel = stagingPanel.getRebasePanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
    refreshSupport.call();
    waitForScheduler();
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());
    
    GitController sc = new GitController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_MINE;
      }
    };
    sc.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    waitForScheduler();
    flushAWT();

    JButton continueBtn = findFirstButton(
        rebasePanel,
        Tags.CONTINUE_REBASE);
    
    assertNotNull(continueBtn);
    
    continueBtn.doClick();
    waitForScheduler();
    flushAWT();
    
    assertFalse(rebasePanel.isShowing());
  }
  
  /**
   * <p><b>Description:</b> Show interrupted rebase dialog and abort.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowInterruptedRebaseDlg_thenAbort() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowInterruptedRebaseDlg_thenAbort-local-1";
    String localTestRepository_2 = "target/test-resources/testShowInterruptedRebaseDlg_thenAbort-local-2";
    String remoteTestRepository = "target/test-resources/testShowInterruptedRebaseDlg_thenAbort-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First commit.");
    gitAccess.push("", "");
    
    //----------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    waitForScheduler();
    
    assertFalse(secondRepoFile.exists());
    gitAccess.pull("", "", PullType.REBASE);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit.");
    gitAccess.push("", "");
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Third commit.");
    
    // Now pull to generate conflict
    RebasePanel rebasePanel = stagingPanel.getRebasePanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
    refreshSupport.call();
    waitForScheduler();
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    Semaphore s = new Semaphore(0);
    PushPullController ppc = new PushPullController() {
      @Override
      protected void showRebaseInProgressDialog() {
        s.release();
        super.showRebaseInProgressDialog();
      }
    };
    ppc.pull(PullType.REBASE);
    s.acquire();
    flushAWT();
    
    JDialog interruptedRebaseDlg = findDialog(Tags.REBASE_IN_PROGRESS);
    assertNotNull(interruptedRebaseDlg);

    JButton abortBtn = findFirstButton(
        interruptedRebaseDlg.getRootPane(),
        Translator.getInstance().getTranslation(Tags.ABORT_REBASE));
    abortBtn.doClick();
    
    waitForScheduler();
    flushAWT();
    
    interruptedRebaseDlg = findDialog(Tags.REBASE_IN_PROGRESS);
    assertNull(interruptedRebaseDlg);

    assertFalse(rebasePanel.isShowing());
    
    assertTableModels(
        // Unstaged
        "",
        // Staged
        "");
  }
  
  /**
   * <p><b>Description:</b> Show interrupted rebase dialog and press cancel.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowInterruptedRebaseDlg_thenCancel() throws Exception {
    String localTestRepository_1 = "target/test-resources/testShowInterruptedRebaseDlg_thenCancel-local-1";
    String localTestRepository_2 = "target/test-resources/testShowInterruptedRebaseDlg_thenCancel-local-2";
    String remoteTestRepository = "target/test-resources/testShowInterruptedRebaseDlg_thenCancel-remote";
    
    // Create and repositories
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo_1 = createRepository(localTestRepository_1);
    Repository localRepo_2 = createRepository(localTestRepository_2);
    bindLocalToRemote(localRepo_1, remoteRepo);
    bindLocalToRemote(localRepo_2, remoteRepo);
    
    new File(localTestRepository_1).mkdirs();
    new File(localTestRepository_2).mkdirs();
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
    firstRepoFile.createNewFile();
    setFileContent(firstRepoFile, "First version");
    
    gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
    gitAccess.commit("First commit.");
    gitAccess.push("", "");
    
    //----------------- REPO 2
    gitAccess.setRepositorySynchronously(localTestRepository_2);
    File secondRepoFile = new File(localTestRepository_2 + "/test.txt");
    
    refreshSupport.call();
    waitForScheduler();
    
    assertFalse(secondRepoFile.exists());
    gitAccess.pull("", "", PullType.REBASE);
    assertTrue(secondRepoFile.exists());
    
    // Modify file and commit and push
    setFileContent(secondRepoFile, "Second versions");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit.");
    gitAccess.push("", "");
    
    //--------------  REPO 1
    gitAccess.setRepositorySynchronously(localTestRepository_1);
    setFileContent(firstRepoFile, "Third version");
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Third commit.");
    
    // Now pull to generate conflict
    RebasePanel rebasePanel = stagingPanel.getRebasePanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
    refreshSupport.call();
    waitForScheduler();
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());
    
    PushPullController ppc = new PushPullController();
    ppc.pull(PullType.REBASE);
    flushAWT();
    sleep(300);
    
    JDialog interruptedRebaseDlg = findDialog(Tags.REBASE_IN_PROGRESS);
    assertNotNull(interruptedRebaseDlg);

    JButton abortBtn = findFirstButton(
        interruptedRebaseDlg.getRootPane(),
        Translator.getInstance().getTranslation(Tags.CANCEL));
    abortBtn.doClick();
    flushAWT();
    sleep(1000);
    
    interruptedRebaseDlg = findDialog(Tags.REBASE_IN_PROGRESS);
    assertNull(interruptedRebaseDlg);

    assertTrue(rebasePanel.isShowing());
    
    assertTableModels(
        // Unstaged
        "CONFLICT, test.txt",
        // Staged
        "");
  }

  /**
   * <p><b>Description:</b> Show interrupted rebase dialog and press continue.</p>
   * <p><b>Bug ID:</b> EXM-42025</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testShowInterruptedRebaseDlg_thenContinue() throws Exception {
    try {
      String localTestRepository_1 = "target/test-resources/testShowInterruptedRebaseDlg_thenContinue-local-1";
      String localTestRepository_2 = "target/test-resources/testShowInterruptedRebaseDlg_thenContinue-local-2";
      String remoteTestRepository = "target/test-resources/testShowInterruptedRebaseDlg_thenContinue-remote";

      // Create and repositories
      Repository remoteRepo = createRepository(remoteTestRepository);
      Repository localRepo_1 = createRepository(localTestRepository_1);
      Repository localRepo_2 = createRepository(localTestRepository_2);
      bindLocalToRemote(localRepo_1, remoteRepo);
      bindLocalToRemote(localRepo_2, remoteRepo);

      new File(localTestRepository_1).mkdirs();
      new File(localTestRepository_2).mkdirs();

      //--------------  REPO 1
      gitAccess.setRepositorySynchronously(localTestRepository_1);
      File firstRepoFile = new File(localTestRepository_1 + "/test.txt");
      firstRepoFile.createNewFile();
      setFileContent(firstRepoFile, "First version");

      gitAccess.add(new FileStatus(GitChangeType.UNKNOWN, "test.txt"));
      gitAccess.commit("First commit.");
      gitAccess.push("", "");

      //----------------- REPO 2
      gitAccess.setRepositorySynchronously(localTestRepository_2);
      File secondRepoFile = new File(localTestRepository_2 + "/test.txt");

      refreshSupport.call();
      flushAWT();
      sleep(400);

      assertFalse(secondRepoFile.exists());
      gitAccess.pull("", "", PullType.REBASE);
      assertTrue(secondRepoFile.exists());

      // Modify file and commit and push
      setFileContent(secondRepoFile, "Second versions");
      gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
      gitAccess.commit("Second commit.");
      gitAccess.push("", "");

      //--------------  REPO 1
      gitAccess.setRepositorySynchronously(localTestRepository_1);
      setFileContent(firstRepoFile, "Third version");
      gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
      gitAccess.commit("Third commit.");

      // Now pull to generate conflict
      RebasePanel rebasePanel = stagingPanel.getRebasePanel();
      assertFalse(rebasePanel.isShowing());
      flushAWT();
      PullResponse pullResponse = gitAccess.pull("", "", PullType.REBASE);
      refreshSupport.call();
      flushAWT();
      sleep(500);
      assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
      assertTrue(rebasePanel.isShowing());

      // Pull again. Rebase in progress dialog is shown
      PushPullController ppc = new PushPullController();
      ppc.pull(PullType.REBASE);
      flushAWT();
      sleep(300);

      JDialog rebaseInProgressDlg = findDialog(Tags.REBASE_IN_PROGRESS);
      assertNotNull(rebaseInProgressDlg);

      // Get Cannot_continue_rebase_because_of_conflicts message
      final String[] warnMessage = new String[1];
      StandalonePluginWorkspace mockedPluginWS = Mockito.mock(StandalonePluginWorkspace.class);
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          warnMessage[0] = invocation.getArguments()[0].toString();
          return null;
        }
      }).when(mockedPluginWS).showWarningMessage(Mockito.anyString());
      Mockito.when(mockedPluginWS.getParentFrame()).thenReturn(null);
      PluginWorkspaceProvider.setPluginWorkspace(mockedPluginWS);
      
      JButton continueBtn = findFirstButton(
          rebaseInProgressDlg.getRootPane(),
          Translator.getInstance().getTranslation(Tags.CONTINUE_REBASE));
      continueBtn.doClick();


      waitForScheduler();
      
      assertEquals("Cannot_continue_rebase_because_of_conflicts", warnMessage[0]);

      // Resolve conflict
      GitController sc = new GitController() {
        @Override
        protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
          return cmd == GitCommand.RESOLVE_USING_MINE;
        }
      };
      sc.doGitCommand(
          Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
          GitCommand.RESOLVE_USING_MINE);
      flushAWT();
      
      // Pull again.
      ppc.pull(PullType.REBASE);
      flushAWT();
      sleep(300);

      // Rebase in progress dialog shown
      rebaseInProgressDlg = findDialog(Tags.REBASE_IN_PROGRESS);
      assertNotNull(rebaseInProgressDlg);
      
      // Now CONTINUE
      continueBtn = findFirstButton(
          rebaseInProgressDlg.getRootPane(),
          Translator.getInstance().getTranslation(Tags.CONTINUE_REBASE));
      continueBtn.doClick();
      flushAWT();
      sleep(1000);

      rebaseInProgressDlg = findDialog(Tags.REBASE_IN_PROGRESS);
      assertNull(rebaseInProgressDlg);

      assertFalse(rebasePanel.isShowing());

      assertTableModels(
          // Unstaged
          "",
          // Staged
          "");
    } finally {
      PluginWorkspaceProvider.setPluginWorkspace(null);
    }
  }
  
}
