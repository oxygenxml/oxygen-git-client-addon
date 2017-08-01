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
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessPushTest {
	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	private final static String REMOTE_TEST_REPOSITPRY = "src/test/resources/remote";
	private Repository db1;
	private Repository db2;
	private GitAccess gitAccess;

	@Before
	public void init() throws RepositoryNotFoundException, IOException {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		db2 = gitAccess.getRepository();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		db1 = gitAccess.getRepository();
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test(expected = MissingObjectException.class)
	public void testRemoteRepositoryHasNoCommitst()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException {

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
	public void testPush()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException {

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

	@After
	public void freeResources() {
		gitAccess.close();
		db2.close();
		File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
		try {
			FileUtils.deleteDirectory(dirToDelete);
			dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
