package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class GitAccessConflictTest extends GitAccessPullTest {
  
  private String[] shownWarningMess = new String[1];
  
  @Override
  @Before
  public void init() throws Exception {
    super.init();
    
    StandalonePluginWorkspace pluginWorkspaceMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspaceMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        shownWarningMess[0] = message;
        return null;
      }
    }).when(pluginWorkspaceMock).showWarningMessage(Mockito.anyString());
    shownWarningMess[0] = "";
  }

	@Test
	public void testResolveUsingTheirs() throws Exception {
		pushOneFileToRemote();

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File testFileSecondRepo = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		testFileSecondRepo.createNewFile();

		PrintWriter out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		FileStatus fileStatus = new FileStatus(GitChangeType.ADD, "test.txt");
    gitAccess.add(fileStatus);
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		
		StageController stageCtrl = new StageController();
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
		
		String expected = "hellllo";
		String actual = getFileContent(testFileSecondRepo);
		assertEquals(expected, actual);
		
		// Pulling now will say that the merge was not concluded and we should commit
    assertEquals(RepositoryState.MERGING_RESOLVED, gitAccess.getRepository().getRepositoryState());

    PushPullController ppc = new PushPullController();
    ppc.pull();
    Thread.sleep(1200);

    assertEquals("Conclude_Merge_Message", shownWarningMess[0]);
	}

	@Test
	public void testRestartMerge() throws Exception {
		pushOneFileToRemote();

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		for (FileStatus fileStatus : gitAccess.getUnstagedFiles()) {
			if(fileStatus.getChangeType() == GitChangeType.CONFLICT){
				gitAccess.add(fileStatus);
			}
		}
		RepositoryState actual = gitAccess.getRepository().getRepositoryState();
		RepositoryState expected = RepositoryState.MERGING_RESOLVED;
		assertEquals(expected, actual);
		
		gitAccess.restartMerge();
		actual = gitAccess.getRepository().getRepositoryState();
		expected = RepositoryState.MERGING;
		assertEquals(expected, actual);
	}
	
  /**
   * Pull (rebase) with conflict. Resolve using mine and continue rebase.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase_ResolveUsingMine_Continue() throws Exception {
    //----------------
    // LOCAL 1
    //----------------
    gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    // Create a file in the remote.
    File remoteParent = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    remoteParent.mkdirs();
    File local1File = new File(FIRST_LOCAL_TEST_REPOSITPRY, "test.txt");
    writeToFile(local1File, "original");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Primul");
    gitAccess.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showInterruptedRebaseDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGitForTests().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    StageController stageCtrl = new StageController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_MINE;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    
    // When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed 
    assertEquals(
        "When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed ",
        "changed in local 2",
        getFileContent(local1File));

    GitStatus gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    gitAccess.continueRebase();
    Thread.sleep(700);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    
    assertEquals(0, gitAccess.getPullsBehind());
    assertEquals(0, gitAccess.getPushesAhead());
    
    PushResponse pushResp = gitAccess.push("", "");
    assertEquals(RemoteRefUpdate.Status.UP_TO_DATE, pushResp.getStatus());
    assertEquals("Push_Up_To_Date", pushResp.getMessage());
  }
  
  /**
   * Pull (rebase) with conflict. Resolve using theirs and then abort.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase_ResolveUsingMine_Abort() throws Exception {
    //----------------
    // LOCAL 1
    //----------------
    gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    // Create a file in the remote.
    File remoteParent = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    remoteParent.mkdirs();
    File local1File = new File(FIRST_LOCAL_TEST_REPOSITPRY, "test.txt");
    writeToFile(local1File, "original");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Primul");
    gitAccess.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showInterruptedRebaseDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGitForTests().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    StageController stageCtrl = new StageController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_MINE;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_MINE);
    
    // When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed 
    assertEquals(
        "When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed ",
        "changed in local 2",
        getFileContent(local1File));

    GitStatus gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    // Abort. Go back to the state before trying to pull.
    gitAccess.abortRebase();
    Thread.sleep(700);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    
    assertEquals(
        "changed in local 1",
        getFileContent(local1File));
    
    // The counters are back to the state from before pull
    assertEquals(1, gitAccess.getPullsBehind());
    assertEquals(1, gitAccess.getPushesAhead());
  }
  
  /**
   * Pull (rebase).
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase_ResolveUsingTheirs_Continue() throws Exception {
    //----------------
    // LOCAL 1
    //----------------
    gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    // Create a file in the remote.
    File remoteParent = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    remoteParent.mkdirs();
    File local1File = new File(FIRST_LOCAL_TEST_REPOSITPRY, "test.txt");
    writeToFile(local1File, "original");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Primul");
    gitAccess.push("", "");
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showInterruptedRebaseDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGitForTests().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    StageController stageCtrl = new StageController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_THEIRS;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    
    
    // When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed 
    assertEquals(
        "When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed ",
        "changed in local 1",
        getFileContent(local1File));
    
    GitStatus gitStatus = gitAccess.getStatus();
    assertEquals(1, gitStatus.getStagedFiles().size());
    assertEquals(
        "(changeType=CHANGED, fileLocation=test.txt)",
        gitStatus.getStagedFiles().get(0).toString());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    gitAccess.continueRebase();
    Thread.sleep(700);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    
    gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    assertEquals(0, gitAccess.getPullsBehind());
    assertEquals(1, gitAccess.getPushesAhead());
  }
  
  /**
   * Pull (rebase).
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase_ResolveUsingTheirs_Abort() throws Exception {
    //----------------
    // LOCAL 1
    //----------------
    gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    // Create a file in the remote.
    File remoteParent = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    remoteParent.mkdirs();
    File local1File = new File(FIRST_LOCAL_TEST_REPOSITPRY, "test.txt");
    writeToFile(local1File, "original");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Primul");
    gitAccess.push("", "");
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showInterruptedRebaseDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGitForTests().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    StageController stageCtrl = new StageController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_THEIRS;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    
    
    // When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed 
    assertEquals(
        "When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed ",
        "changed in local 1",
        getFileContent(local1File));
    
    GitStatus gitStatus = gitAccess.getStatus();
    assertEquals(1, gitStatus.getStagedFiles().size());
    assertEquals(
        "(changeType=CHANGED, fileLocation=test.txt)",
        gitStatus.getStagedFiles().get(0).toString());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    gitAccess.abortRebase();
    Thread.sleep(700);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    
    gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    // Back before pull
    assertEquals(1, gitAccess.getPullsBehind());
    assertEquals(1, gitAccess.getPushesAhead());
  }
  
  /**
   * Pull (rebase) with conflict. Restart merge.
   * 
   * @throws Exception
   */
  @Test
  public void testPullWithConflicts_Rebase_RestartMerge() throws Exception {
    //----------------
    // LOCAL 1
    //----------------
    gitAccess = GitAccess.getInstance();
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    // Create a file in the remote.
    File remoteParent = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    remoteParent.mkdirs();
    File local1File = new File(FIRST_LOCAL_TEST_REPOSITPRY, "test.txt");
    writeToFile(local1File, "original");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Primul");
    gitAccess.push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showInterruptedRebaseDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGitForTests().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
    StageController stageCtrl = new StageController() {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommand cmd) {
        return cmd == GitCommand.RESOLVE_USING_THEIRS;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommand.RESOLVE_USING_THEIRS);
    
    // When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed 
    assertEquals(
        "When having a conflict while rebasing, 'Mine' and 'Theirs' become reversed ",
        "changed in local 1",
        getFileContent(local1File));
    
    GitStatus gitStatus = gitAccess.getStatus();
    assertEquals(1, gitStatus.getStagedFiles().size());
    assertEquals(
        "(changeType=CHANGED, fileLocation=test.txt)",
        gitStatus.getStagedFiles().get(0).toString());
    assertTrue(gitStatus.getUnstagedFiles().isEmpty());
    
    RepositoryState repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    assertEquals(
        "changed in local 1",
        getFileContent(local1File));
    
    // Restart merge
    gitAccess.restartMerge();
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertEquals(
        "[(changeType=CONFLICT, fileLocation=test.txt)]",
        gitStatus.getUnstagedFiles().toString());
    
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));
  }

  /**
   * 
   * @param file
   * @param content
   * @throws FileNotFoundException
   */
  private void writeToFile(File file, String content) throws FileNotFoundException {
    PrintWriter out = new PrintWriter(file);
    out.println(content);
    out.close();
  }

  /**
   * 
   * @param file
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
	private String getFileContent(File file) throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String sCurrentLine;
		String content = "";
		while ((sCurrentLine = br.readLine()) != null) {
			content += sCurrentLine;
		}
		br.close();
		fr.close();
		return content;
	}
}
