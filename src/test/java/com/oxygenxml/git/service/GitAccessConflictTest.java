package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
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

import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.refresh.PanelRefresh;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

public class GitAccessConflictTest {
  PanelRefresh refreshSupport = new PanelRefresh(null) {
    @Override
    protected int getScheduleDelay() {
      // Execute refresh events immediately from tests.
      return 1;
    }
  };
  
  protected final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/local";
  protected final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessConflictTest/local2";
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/remote";
  private Repository localRepo1;
  private Repository localRepo2;
  private Repository remoteRepo;
  protected GitAccess gitAccess;
  private String[] shownWarningMess = new String[1];
  private String[] errMsg = new String[1];
  
  /**
   * Push changes.
   * 
   * @param username User name.
   * @param password Password.
   * 
   * @return push response.
   * 
   * @throws GitAPIException 
   */
  protected final PushResponse push(String username, String password) throws GitAPIException {
    return GitAccess.getInstance().push(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()));
  }
  
  /**
   * Pull.
   * 
   * @param username          Username.
   * @param password          Password.
   * @param pullType          Pull type.
   * @param updateSubmodules  <code>true</code> to update submodules.
   * 
   * @return Pull response.
   * 
   * @throws GitAPIException
   */
  protected PullResponse pull(String username, String password, PullType pullType, boolean updateSubmodules) throws GitAPIException {
    return GitAccess.getInstance().pull(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()),
        pullType,
        updateSubmodules);
  }
  
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


    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage mockedWsOptionsStorage = Mockito.mock(WSOptionsStorage.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(mockedWsOptionsStorage).setOption(Mockito.anyString(), Mockito.any());
    Mockito.doAnswer(new Answer<WSOptionsStorage>() {
      @Override
      public WSOptionsStorage answer(InvocationOnMock invocation) throws Throwable {
        return mockedWsOptionsStorage;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getOptionsStorage();
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    });
    Mockito.doAnswer(new Answer<XMLUtilAccess>() {
      @Override
      public XMLUtilAccess answer(InvocationOnMock invocation) throws Throwable {
        return xmlUtilAccess;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getXMLUtilAccess();

    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(pluginWSMock).showWarningMessage(Mockito.anyString());
    
    ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);
    Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(projectCtrlMock).refreshFolders(Mockito.any());
  
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        shownWarningMess[0] = message;
        return null;
      }
    }).when(pluginWSMock).showWarningMessage(Mockito.anyString());
    shownWarningMess[0] = "";
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        errMsg[0] = message;
        return null;
      }
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString());
    errMsg[0] = "";
  }
  
  @After
  public void freeResources() {
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    System.gc();
    
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

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File testFileSecondRepo = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		testFileSecondRepo.createNewFile();

		writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "teeeeeest");

		FileStatus fileStatus = new FileStatus(GitChangeType.ADD, "test.txt");
    gitAccess.add(fileStatus);
		gitAccess.commit("conflict");
		pull("", "", PullType.MERGE_FF, false);
		
		GitController gitCtrl = new GitController(gitAccess);
    gitCtrl.asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(700);
		
		String expected = "hellllo";
		String actual = getFileContent(testFileSecondRepo);
		assertEquals(expected, actual);
		
		// Pulling now will say that the merge was not concluded and we should commit
    assertEquals(RepositoryState.MERGING_RESOLVED, gitAccess.getRepository().getRepositoryState());

    GitController ppc = new GitController(gitAccess);
    ppc.pull();
    sleep(1200);

    assertEquals("Conclude_Merge_Message", shownWarningMess[0]);
	}

	@Test
	public void testRestartMerge() throws Exception {

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    OptionsManager.getInstance().saveSelectedRepository(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "teeeeeest");;

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		pull("", "", PullType.MERGE_FF, false);
		for (FileStatus fileStatus : gitAccess.getUnstagedFiles()) {
			if(fileStatus.getChangeType() == GitChangeType.CONFLICT){
				gitAccess.add(fileStatus);
			}
		}
		RepositoryState actual = gitAccess.getRepository().getRepositoryState();
		RepositoryState expected = RepositoryState.MERGING_RESOLVED;
		assertEquals(expected, actual);
		
		gitAccess.restartMerge();
		sleep(1000);
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
    push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    GitController pc = new GitController(gitAccess) {
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
    TestUtil.collectPushPullEvents(pc, b);
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGit().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    GitControllerBase gitCtrl = new GitControllerBase(gitAccess) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_MINE;
      }
    };
    gitCtrl.asyncResolveUsingMine(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(700);
    
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
    sleep(700);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.SAFE, repositoryState);
    
    assertEquals(0, gitAccess.getPullsBehind());
    assertEquals(0, gitAccess.getPushesAhead());
    
    PushResponse pushResp = push("", "");
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
    push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    GitController pc = new GitController(gitAccess) {
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
    TestUtil.collectPushPullEvents(pc, b);
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGit().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    GitControllerBase gitCtrl = new GitControllerBase(gitAccess) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_MINE;
      }
    };
    gitCtrl.asyncResolveUsingMine(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(700);
    
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
    sleep(700);
    
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
    push("", "");
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    GitController pc = new GitController(gitAccess) {
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
    TestUtil.collectPushPullEvents(pc, b);
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGit().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    GitControllerBase gitCtrl = new GitControllerBase(gitAccess) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_THEIRS;
      }
    };
    gitCtrl.asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(700);
    
    
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
    sleep(700);
    
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
    push("", "");
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    GitController pc = new GitController(gitAccess) {
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
    TestUtil.collectPushPullEvents(pc, b);
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGit().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    GitControllerBase gitCtrl = new GitControllerBase(gitAccess) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_THEIRS;
      }
    };
    gitCtrl.asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(700);
    
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
    sleep(700);
    
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
    push("", "");
    
    
    //----------------
    // LOCAL 2
    //----------------
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    PullResponse pull = pull("", "", PullType.MERGE_FF, false);
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    File local2File = new File(SECOND_LOCAL_TEST_REPOSITORY, "test.txt");
    assertEquals("original", getFileContent(local2File));
    
    writeToFile(local2File, "changed in local 2");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Al doilea");
    push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(local1File, "changed in local 1");
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    GitController pc = new GitController(gitAccess) {
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
    TestUtil.collectPushPullEvents(pc, b);
    
    // Get conflict
    assertEquals("changed in local 1", getFileContent(local1File));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Another");
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    
    // Show the "Interrupted rebase" dialog
    pc.pull(PullType.REBASE).get();
    assertTrue(wasRebaseInterrupted[0]);
    
    Status status = gitAccess.getGit().status().call();
    assertEquals("[test.txt]", status.getConflicting().toString());
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
    GitControllerBase gitCtrl = new GitControllerBase(gitAccess) {
      @Override
      protected boolean isUserOKWithResolvingRebaseConflictUsingMineOrTheirs(ConflictResolution cmd) {
        return cmd == ConflictResolution.RESOLVE_USING_THEIRS;
      }
    };
    gitCtrl.asyncResolveUsingTheirs(
        Arrays.asList(new FileStatus(GitChangeType.CONFLICT, "test.txt")));
    sleep(1000);
    
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
    sleep(1000);
    
    repositoryState = gitAccess.getRepository().getRepositoryState();
    assertEquals(RepositoryState.REBASING_MERGE, repositoryState);
    
    gitStatus = gitAccess.getStatus();
    assertTrue(gitStatus.getStagedFiles().isEmpty());
    assertEquals(
        "[(changeType=CONFLICT, fileLocation=test.txt)]",
        gitStatus.getUnstagedFiles().toString());
    
    assertTrue(getFileContent(local1File).startsWith("<<<<<<< Upstream, based on branch '" + GitAccess.DEFAULT_BRANCH_NAME + "' of file:"));
  }
  
  /**
   * Sleep well!
   * 
   * @param delay Delay.
   * 
   * @throws InterruptedException
   */
  private void sleep(int delay) throws InterruptedException {
    Thread.sleep(delay);
  }

  /**
   * Writes content to file.
   * 
   * @param file Target file.
   * @param content Content to write in the file.
   * 
   * @throws FileNotFoundException File not found.
   */
  private void writeToFile(File file, String content) throws FileNotFoundException {
    PrintWriter out = new PrintWriter(file);
    try {
    out.print(content);
    } finally {
      out.close();
    }
  }

  /**
   * Reads file content.
   * 
   * @param file File to read.
   * 
   * @return The file content.
   * 
   * @throws IOException Unable to read the file.
   */
	private String getFileContent(File file) throws IOException {
		return GitTestBase.read(file.toURI().toURL());
	}
}
