package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessPushTest extends GitTestBase {
	private final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local";
	private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/remote";
	private final static String SECOND_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPushTest/local2";
	private Repository firstLocalRepo;
	private Repository remoteRepo;
	private Repository secondLocalRepo;
	private GitAccess gitAccess;

	@Before
	public void setUp() throws Exception {
	  super.setUp();
	  
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		remoteRepo = gitAccess.getRepository();
		gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITPRY);
		firstLocalRepo = gitAccess.getRepository();
		gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITPRY);
		secondLocalRepo = gitAccess.getRepository();

		File file = new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
			file = new File(SECOND_LOCAL_TEST_REPOSITPRY + "/test2.txt");
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testRemoteRepositoryHasNoCommitst()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");

		// throws missingObjectException
		try {
		  remoteRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}");
		  
		  fail("A MissingObjectException is failed.");
		} catch (MissingObjectException ex) {
		  // Expected exception.
		}
	}

	@Test
	public void testPushOK()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");

		gitAccess.push("", "");

		assertEquals(firstLocalRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"),
				remoteRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"));

	}
	
	@Test
	public void testAPushRejected()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
		StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");
		gitAccess.push("", "");
		
		gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITPRY);
		config = gitAccess.getRepository().getConfig();
		remoteConfig = new RemoteConfig(config, "origin");
		uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file poc");
		PushResponse response = gitAccess.push("", "");
		Status actual = response.getStatus();
		Status expected = Status.REJECTED_NONFASTFORWARD;
		assertEquals(expected, actual);
		
	}
	
	@Test
	public void testNoPushesAhead() throws RepositoryNotFoundException, IOException, URISyntaxException, NoRepositorySelected, InvalidRemoteException, TransportException, GitAPIException, RepoNotInitializedException{
		StoredConfig config = firstLocalRepo.getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    remoteConfig.update(config);
    config.save();
    
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITPRY);
    File local1File = new File(REMOTE_TEST_REPOSITPRY, "test1.txt");
    local1File.createNewFile();
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test1.txt"));
    gitAccess.commit("Primul");
    
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    gitAccess.pull("", "");
    
    String branchName = "master";
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();
    
		assertEquals(0, gitAccess.getPushesAhead());
	}


	@After
	public void tearDown() throws Exception {
	  super.tearDown();
	  
		gitAccess.closeRepo();
		firstLocalRepo.close();
		remoteRepo.close();
		secondLocalRepo.close();
		File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITPRY);
		try {
			FileUtils.deleteDirectory(dirToDelete);
			dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dirToDelete);
			dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
