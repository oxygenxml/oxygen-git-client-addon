package com.oxygenxml.sdksamples.workspace.git.service;

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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.PushResponse;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.OptionsManager;

public class GitAccessPullTest {
	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	private final static String SECOND_LOCAL_TEST_REPOSITORY = "src/test/resources/local2";
	private final static String REMOTE_TEST_REPOSITPRY = "src/test/resources/remote";
	private Repository db1;
	private Repository db2;
	private Repository db3;
	private GitAccess gitAccess;

	@Before
	public void init()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		db1 = gitAccess.getRepository();
		gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
		db2 = gitAccess.getRepository();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		db3 = gitAccess.getRepository();

		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.createNewFile();	
		StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db3.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		config = gitAccess.getRepository().getConfig();
		remoteConfig = new RemoteConfig(config, "origin");
		uri = new URIish(db3.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
		remoteConfig.addFetchRefSpec(spec);
		remoteConfig.update(config);
		config.save();

	}

	@Test
	public void testPullOK()
			throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		PullResponse response = gitAccess.pull("", "");
		PullStatus actual = response.getStatus();
		PullStatus expected = PullStatus.OK;
		assertEquals(expected, actual);
	}

	@Test
	public void testPullUncomitedFiles() throws RepositoryNotFoundException, FileNotFoundException,
			InvalidRemoteException, TransportException, IOException, GitAPIException {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test2.txt");
		file.createNewFile();

		PullResponse response = gitAccess.pull("", "");
		PullStatus actual = response.getStatus();
		PullStatus expected = PullStatus.UNCOMITED_FILES;
		assertEquals(expected, actual);
	}
	
	@Test
	public void testPullConflicts() throws RepositoryNotFoundException, FileNotFoundException,
			InvalidRemoteException, TransportException, IOException, GitAPIException {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();
		
		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();
		
		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		PushResponse pushResponse = gitAccess.push("", "");
		Status pushActual = pushResponse.getStatus();
		Status pushExpected = Status.REJECTED_NONFASTFORWARD;
		assertEquals(pushExpected, pushActual);
		
		PullResponse pullResponse = gitAccess.pull("", "");
		PullStatus pullActual = pullResponse.getStatus();
		PullStatus pullExpected = PullStatus.CONFLICTS;
		assertEquals(pullExpected, pullActual);
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
			dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITORY);
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void pushOneFileToRemote() throws IOException, RepositoryNotFoundException, FileNotFoundException,
			InvalidRemoteException, TransportException, GitAPIException {
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("hellllo");
		out.close();
		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");
		gitAccess.push("", "");
	}

}
