package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;

import junit.framework.TestCase;

public class GitAccessCommitTest extends TestCase {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessCommitTest";
	private GitAccess gitAccess = GitAccess.getInstance();

	protected void setUp() throws Exception {
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
	}
	
	@Override
	protected void tearDown() throws Exception {
	  gitAccess.cleanUp();
	  File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
    FileUtil.deleteRecursivelly(dirToDelete);
	}

	@Test
	public void testSingleFileCommit() throws Exception {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
		gitAccess.commit("single file added");

		Repository repository = gitAccess.getRepository();

		RevWalk walk = null;
		TreeWalk treeWalk = null;
		List<String> actualFileNamesPath = new ArrayList<>();
		String actualMessage = null;
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			walk = new RevWalk(repository);
			RevCommit commit = walk.parseCommit(head.getObjectId());
			actualMessage = commit.getFullMessage();

			treeWalk = new TreeWalk(repository);
			treeWalk.reset(commit.getTree());
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				actualFileNamesPath.add(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			walk.close();
			treeWalk.close();
		}

		String expectedMessage = "single file added";
		List<String> expectedFileNamesPath = new ArrayList<>();
		expectedFileNamesPath.add("test.txt");

		assertEquals(expectedMessage, actualMessage);
		assertEquals(expectedFileNamesPath.toString(), actualFileNamesPath.toString());
	}

	@Test
	public void testMultipleFileCommit() throws Exception {
		int n = 3;
		List<FileStatus> files = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			File file = new File(LOCAL_TEST_REPOSITPRY + "/test" + i + ".txt");
			files.add(new FileStatus(GitChangeType.ADD, file.getName()));
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		gitAccess.addAll(files);
		gitAccess.commit("multiple files added");

		Repository repository = gitAccess.getRepository();

		RevWalk walk = null;
		TreeWalk treeWalk = null;
		List<String> actualFileNamesPath = new ArrayList<>();
		String actualMessage = null;
		try {
			Ref head = repository.exactRef(Constants.HEAD);
			walk = new RevWalk(repository);
			RevCommit commit = walk.parseCommit(head.getObjectId());
			actualMessage = commit.getFullMessage();

			treeWalk = new TreeWalk(repository);
			treeWalk.reset(commit.getTree());
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				actualFileNamesPath.add(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			walk.close();
			treeWalk.close();
		}

		String expectedMessage = "multiple files added";
		List<String> expectedFileNamesPath = new ArrayList<>();
		expectedFileNamesPath.add("test0.txt");
		expectedFileNamesPath.add("test1.txt");
		expectedFileNamesPath.add("test2.txt");

		assertEquals(expectedMessage, actualMessage);
		assertEquals(expectedFileNamesPath.toString(), actualFileNamesPath.toString());
	}
}
