package com.oxygenxml.sdksamples.workspace.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessPushTest {
	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	private final static String REMOTE_TEST_REPOSITPRY = "src/test/resources/remote";
	private final static String SECOND_TEST_REPOSITPRY = "src/test/resources/local2";
	private Repository db1;
	private Repository db2;
	private Repository db3;
	private GitAccess gitAccess;

	@Before
	public void init() throws RepositoryNotFoundException, IOException, NoRepositorySelected {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		db2 = gitAccess.getRepository();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		db1 = gitAccess.getRepository();
		gitAccess.createNewRepository(SECOND_TEST_REPOSITPRY);
		db3 = gitAccess.getRepository();

		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
			file = new File(SECOND_TEST_REPOSITPRY + "/test2.txt");
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test(expected = MissingObjectException.class)
	public void testRemoteRepositoryHasNoCommitst()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db2.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");

		// throws missingObjectException
		db2.resolve(gitAccess.getLastLocalCommit().getName() + "^{commit}");
	}

	@Test
	public void testPushOK()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db2.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");

		gitAccess.push("", "");

		assertEquals(db1.resolve(gitAccess.getLastLocalCommit().getName() + "^{commit}"),
				db2.resolve(gitAccess.getLastLocalCommit().getName() + "^{commit}"));

	}
	
	@Test
	public void testPushRejected()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException, NoRepositorySelected {
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db2.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");
		gitAccess.push("", "");
		
		gitAccess.setRepository(SECOND_TEST_REPOSITPRY);
		config = gitAccess.getRepository().getConfig();
		remoteConfig = new RemoteConfig(config, "origin");
		uri = new URIish(db2.getDirectory().toURI().toURL());
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
	public void testPushesAhead() throws RepositoryNotFoundException, IOException, URISyntaxException, NoRepositorySelected, InvalidRemoteException, TransportException, GitAPIException{
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db2.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");

		gitAccess.push("", "");
		int actual = gitAccess.getPushesAhead();
		int expected = 1;
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testNoPushesAhead() throws RepositoryNotFoundException, IOException, URISyntaxException, NoRepositorySelected, InvalidRemoteException, TransportException, GitAPIException{
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		final StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db2.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		int actual = gitAccess.getPushesAhead();
		int expected = 0;
		
		assertEquals(expected, actual);
	}


	@After
	public void freeResources() {
		gitAccess.close();
		db1.close();
		db2.close();
		db3.close();
		File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
		try {
			FileUtils.deleteDirectory(dirToDelete);
			dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dirToDelete);
			dirToDelete = new File(SECOND_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
