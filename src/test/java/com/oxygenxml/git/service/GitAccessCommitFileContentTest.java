package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
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

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

public class GitAccessCommitFileContentTest {
	protected final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessCommitFileContentTest/local";
	protected final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessCommitFileContentTest/local2";
	private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessCommitFileContentTest/remote";
	private Repository localRepo1;
	private Repository localRepo2;
	private Repository remoteRepo;
	protected GitAccess gitAccess;
	
	
	public GitAccessCommitFileContentTest() {
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
  }

	@Before
	public void init() throws URISyntaxException, IOException, InvalidRemoteException, TransportException,
			GitAPIException, NoRepositorySelected {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		localRepo1 = gitAccess.getRepository();
		gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
		localRepo2 = gitAccess.getRepository();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		remoteRepo = gitAccess.getRepository();

		gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.createNewFile();
		StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish remoteURI = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(remoteURI);
		remoteConfig.update(config);
		config.save();

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		config = gitAccess.getRepository().getConfig();
		remoteConfig = new RemoteConfig(config, "origin");
		remoteURI = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(remoteURI);
		RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
		remoteConfig.addFetchRefSpec(spec);
		remoteConfig.update(config);
		config.save();
	}

	@Test
	public void testMineCommitFile() throws RepositoryNotFoundException, FileNotFoundException, InvalidRemoteException,
			TransportException, IOException, GitAPIException, NoRepositorySelected, InterruptedException {
		pushOneFileToRemote("hellllo");

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		ObjectId commit = gitAccess.getCommit(Commit.MINE, "test.txt");
		ObjectLoader open = gitAccess.getRepository().open(commit);
		String actual = new String(open.getBytes());
		String expected = "teeeeeest\n";
		assertEquals(expected, actual);
	}

	@Test
	public void testBaseCommitFile() throws RepositoryNotFoundException, FileNotFoundException, InvalidRemoteException,
			TransportException, IOException, GitAPIException, NoRepositorySelected, InterruptedException {

		pushOneFileToRemote("test 1");
		
		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		gitAccess.pull("", "");
		gitAccess.closeRepo();

		gitAccess = GitAccess.getInstance();
		pushOneFileToRemote("test 2");
		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		PrintWriter out = new PrintWriter(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");

		ObjectId commit = gitAccess.getCommit(Commit.MINE, "test.txt");
		ObjectLoader open = gitAccess.getRepository().open(commit);
		String actual = new String(open.getBytes());
		String expected = "teeeeeest\n";
		assertEquals(expected, actual);

		commit = gitAccess.getCommit(Commit.THEIRS, "test.txt");
		open = gitAccess.getRepository().open(commit);
		actual = new String(open.getBytes());
		expected = "test 2\n";
		assertEquals(expected, actual);

		commit = gitAccess.getCommit(Commit.BASE, "test.txt");
		open = gitAccess.getRepository().open(commit);
		actual = new String(open.getBytes());
		expected = "test 1\n";
		assertEquals(expected, actual);
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		gitAccess.commit("pocpoc");
		gitAccess.push("", "");
		
	}

	@Test
	public void testTheirsCommitFile() throws RepositoryNotFoundException, FileNotFoundException, InvalidRemoteException,
			TransportException, IOException, GitAPIException, NoRepositorySelected, InterruptedException {
		pushOneFileToRemote("hellllo");

		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		ObjectId commit = gitAccess.getCommit(Commit.THEIRS, "test.txt");
		ObjectLoader open = gitAccess.getRepository().open(commit);
		String actual = new String(open.getBytes());
		String expected = "hellllo\n";
		
		assertEquals(expected, actual);
	}

	protected void pushOneFileToRemote(String message) throws IOException, RepositoryNotFoundException,
			FileNotFoundException, InvalidRemoteException, TransportException, GitAPIException, InterruptedException {
		gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITPRY);
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println(message);
		out.close();
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		gitAccess.commit(message);
		gitAccess.push("", "");
	}

	@After
	public void freeResources() throws Exception {
	  // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
	  System.gc();

		gitAccess.closeRepo();
		localRepo1.close();
		localRepo2.close();
		remoteRepo.close();
		File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
		FileUtils.deleteDirectory(dirToDelete);
		dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
		FileUtils.deleteDirectory(dirToDelete);
		dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITORY);
		FileUtils.deleteDirectory(dirToDelete);
	}
}
