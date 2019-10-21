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

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitCommandState;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class GitAccessConflictTest {
  
  protected final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/local";
  protected final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessConflictTest/local2";
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/remote";
  private Repository localRepo1;
  private Repository localRepo2;
  private Repository remoteRepo;
  protected GitAccess gitAccess;
  private String[] shownWarningMess = new String[1];
  
  @Before
  public void init() throws Exception {

    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITPRY);
    localRepo1 = gitAccess.getRepository();
    gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
    localRepo2 = gitAccess.getRepository();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    File file = new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
    file.createNewFile(); 
    StoredConfig config = gitAccess.getRepository().getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec1 = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec1);
    remoteConfig.update(config);
    config.save();
    
    String branchName = "master";
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();

    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    config = gitAccess.getRepository().getConfig();
    remoteConfig = new RemoteConfig(config, "origin");
    uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    remoteConfig.update(config);
    config.save();
    
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();

  
    
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
  
  @After
  public void freeResources() {
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    WindowCache.getInstance().cleanup();

    gitAccess.closeRepo();
    localRepo1.close();
    localRepo2.close();
    remoteRepo.close();
    File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    try {
      FileUtils.deleteDirectory(dirToDelete);
      dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
      dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITORY);
      FileUtils.deleteDirectory(dirToDelete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

	@Test
	public void testResolveUsingTheirs() throws Exception {
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    OptionsManager.getInstance().saveSelectedRepository(FIRST_LOCAL_TEST_REPOSITPRY);

    PrintWriter out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
    out.println("hellllo");
    out.close();
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    gitAccess.push("", "");
  

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File testFileSecondRepo = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		testFileSecondRepo.createNewFile();

		out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		FileStatus fileStatus = new FileStatus(GitChangeType.ADD, "test.txt");
    gitAccess.add(fileStatus);
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		
		StageController stageCtrl = new StageController();
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_THEIRS_STARTED);
		
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

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    OptionsManager.getInstance().saveSelectedRepository(FIRST_LOCAL_TEST_REPOSITPRY);

    PrintWriter out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
    out.println("hellllo");
    out.close();
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    gitAccess.push("", "");
  

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		out = new PrintWriter(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
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
      protected void showRebaseInProgressDialog() {
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
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommandState cmd) {
        return cmd == GitCommandState.RESOLVE_USING_MINE_STARTED;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_MINE_STARTED);
    
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
      protected void showRebaseInProgressDialog() {
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
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommandState cmd) {
        return cmd == GitCommandState.RESOLVE_USING_MINE_STARTED;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_MINE_STARTED);
    
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
      protected void showRebaseInProgressDialog() {
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
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommandState cmd) {
        return cmd == GitCommandState.RESOLVE_USING_THEIRS_STARTED;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_THEIRS_STARTED);
    
    
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
      protected void showRebaseInProgressDialog() {
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
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommandState cmd) {
        return cmd == GitCommandState.RESOLVE_USING_THEIRS_STARTED;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_THEIRS_STARTED);
    
    
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
      protected void showRebaseInProgressDialog() {
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
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(GitCommandState cmd) {
        return cmd == GitCommandState.RESOLVE_USING_THEIRS_STARTED;
      }
    };
    stageCtrl.doGitCommand(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")),
        GitCommandState.RESOLVE_USING_THEIRS_STARTED);
    
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
