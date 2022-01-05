package com.oxygenxml.git.view;

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

import com.oxygenxml.git.service.ConflictResolution;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.staging.ConflictButtonsPanel;
import com.oxygenxml.git.view.staging.FlatViewTestBase;

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
    PluginWorkspace pluginWorkspaceMock = PluginWorkspaceProvider.getPluginWorkspace();
    final boolean[] showErrorMessageCalled = new boolean[] {false};

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
    sleep(700);

    // Create a new file and push it.
    String fileName = "test.txt";
    File file = commitNewFile(localTestRepository, fileName, "content");
    PushResponse push = push("", "");
    assertEquals("status: OK message null", push.toString());

    // Create lock files
    String repoDir = GitAccess.getInstance().getRepository().getDirectory().getAbsolutePath();
    Ref ref = GitAccess.getInstance().getRemoteBrachListForCurrentRepo().get(0);
    File lockFile = new File(repoDir, ref.getName() + ".lock");
    boolean createNewFile = lockFile.createNewFile();
    assertTrue("Unnable to create lock file " + lockFile.getAbsolutePath(), createNewFile);
    setFileContent(lockFile, GitAccess.getInstance().getLastLocalCommitInRepo().getName());

    // Commit a new version of the file.
    setFileContent(file, "modified");
    GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, fileName));
    GitAccess.getInstance().commit("modified");
    push("", "");
    assertEquals("status: OK message null", push.toString());

    // Pull should throw "Lock failed" error
    PullResponse pullResponse = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.LOCK_FAILED, pullResponse.getStatus());
    assertTrue(showErrorMessageCalled[0]);
    Future<?> execute = ((GitController) stagingPanel.getGitController()).pull();
    execute.get();
    flushAWT();
    assertEquals("Lock_failed", stagingPanel.getCommitPanel().getStatusLabel().getText());
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
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    pull("", "", PullType.REBASE, false);
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
    ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.REBASE, false);
    refreshSupport.call();
    waitForScheduler();
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    // --------------- REPO 2
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_2);
    sleep(300);
    assertFalse(rebasePanel.isShowing());
    
    // --------------- REPO 1
    GitAccess.getInstance().setRepositorySynchronously(localTestRepository_1);
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
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    pull("", "", PullType.REBASE, false);
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
    ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.REBASE, false);
    refreshSupport.call();
    waitForScheduler();
    sleep(1000);
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    JButton abortButton = findFirstButton(rebasePanel, "Abort_rebase");
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
    sleep(400);
    
    assertFalse(secondRepoFile.exists());
    pull("", "", PullType.REBASE, false);
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
    ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.REBASE, false);
    refreshSupport.call();
    waitForScheduler();
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());
    
    GitControllerBase sc = new GitControllerBase(GitAccess.getInstance()) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_MINE;
      }
    };
    sc.asyncResolveUsingMine(Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
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
    waitForScheduler();
    
    assertFalse(secondRepoFile.exists());
    pull("", "", PullType.REBASE, false);
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
    ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.REBASE, false);
    refreshSupport.call();
    waitForScheduler();
    
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());

    Semaphore s = new Semaphore(0);
    GitController ppc = new GitController() {
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
    waitForScheduler();
    
    assertFalse(secondRepoFile.exists());
    pull("", "", PullType.REBASE, false);
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
    ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
    assertFalse(rebasePanel.isShowing());
    flushAWT();
    PullResponse pullResponse = pull("", "", PullType.REBASE, false);
    refreshSupport.call();
    waitForScheduler();
    assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
    assertTrue(rebasePanel.isShowing());
    
    GitController ppc = (GitController) stagingPanel.getGitController();
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
      sleep(400);

      assertFalse(secondRepoFile.exists());
      pull("", "", PullType.REBASE, false);
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
      ConflictButtonsPanel rebasePanel = stagingPanel.getConflictButtonsPanel();
      assertFalse(rebasePanel.isShowing());
      flushAWT();
      PullResponse pullResponse = pull("", "", PullType.REBASE, false);
      refreshSupport.call();
      flushAWT();
      sleep(500);
      assertEquals(PullStatus.CONFLICTS, pullResponse.getStatus());
      assertTrue(rebasePanel.isShowing());

      // Pull again. Rebase in progress dialog is shown
      GitController ppc = (GitController) stagingPanel.getGitController();
      ppc.pull(PullType.REBASE);
      flushAWT();
      sleep(300);

      JDialog rebaseInProgressDlg = findDialog(Tags.REBASE_IN_PROGRESS);
      assertNotNull(rebaseInProgressDlg);

      // Get Cannot_continue_rebase_because_of_conflicts message
      final String[] errMessage = new String[1];
      StandalonePluginWorkspace mockedPluginWS = Mockito.mock(StandalonePluginWorkspace.class);
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          Object[] arguments = invocation.getArguments();
          if (arguments != null && arguments.length == 1) {
            errMessage[0] = arguments[0].toString();
          }
          return null;
        }
      }).when(mockedPluginWS).showErrorMessage(Mockito.anyString());
      Mockito.when(mockedPluginWS.getParentFrame()).thenReturn(null);
      PluginWorkspaceProvider.setPluginWorkspace(mockedPluginWS);
      
      JButton continueBtn = findFirstButton(
          rebaseInProgressDlg.getRootPane(),
          Translator.getInstance().getTranslation(Tags.CONTINUE_REBASE));
      continueBtn.doClick();


      waitForScheduler();
      
      assertEquals("Cannot_continue_rebase_because_of_conflicts", errMessage[0]);

      // Resolve conflict
      GitControllerBase sc = new GitControllerBase(GitAccess.getInstance()) {
        @Override
        protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
          return cmd == ConflictResolution.RESOLVE_USING_MINE;
        }
      };
      sc.asyncResolveUsingMine(
          Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
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
      waitForScheduler();
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
