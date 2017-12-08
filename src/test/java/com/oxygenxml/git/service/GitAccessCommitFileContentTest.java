package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Arrays;

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

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessCommitFileContentTest {
	protected final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	protected final static String SECOND_LOCAL_TEST_REPOSITORY = "src/test/resources/local2";
	private final static String REMOTE_TEST_REPOSITPRY = "src/test/resources/remote";
	private Repository db1;
	private Repository db2;
	private Repository db3;
	protected GitAccess gitAccess;

	@Before
	public void init() throws URISyntaxException, IOException, InvalidRemoteException, TransportException,
			GitAPIException, NoRepositorySelected {
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
	public void testMineCommitFile() throws RepositoryNotFoundException, FileNotFoundException, InvalidRemoteException,
			TransportException, IOException, GitAPIException, NoRepositorySelected, InterruptedException {
		pushOneFileToRemote("hellllo");

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
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
		
		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		gitAccess.pull("", "");
		gitAccess.close();

		gitAccess = GitAccess.getInstance();
		pushOneFileToRemote("test 2");
		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
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

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
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
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println(message);
		out.close();
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		gitAccess.commit(message);
		gitAccess.push("", "");
	}

	@After
	public void freeResources() throws InterruptedException {

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
}
