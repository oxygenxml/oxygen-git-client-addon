package com.oxygenxml.sdksamples.workspace.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

public class GitAccessCommitTest {

	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources";
	private GitAccess gitAccess;

	@Before
	public void init() {
		gitAccess = new GitAccess();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
	}

	@Test
	public void testSingleFileCommit() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(ChangeType.ADD, file.getName()));
		gitAccess.commit("single file added");

		Repository repository = gitAccess.getRepository();

		RevWalk walk = null;
		TreeWalk treeWalk = null;
		List<String> actualFileNamesPath = new ArrayList<String>();
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
		List<String> expectedFileNamesPath = new ArrayList<String>();
		expectedFileNamesPath.add("test.txt");

		assertEquals(actualMessage, expectedMessage);
		assertEquals(actualFileNamesPath, expectedFileNamesPath);
	}

	@Test
	public void testMultipleFileCommit() {
		int n = 3;
		List<FileStatus> files = new ArrayList<FileStatus>();
		for (int i = 0; i < n; i++) {
			File file = new File(LOCAL_TEST_REPOSITPRY + "/test" + i + ".txt");
			files.add(new FileStatus(ChangeType.ADD, file.getName()));
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
		List<String> actualFileNamesPath = new ArrayList<String>();
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
		List<String> expectedFileNamesPath = new ArrayList<String>();
		expectedFileNamesPath.add("test0.txt");
		expectedFileNamesPath.add("test1.txt");
		expectedFileNamesPath.add("test2.txt");

		assertEquals(actualMessage, expectedMessage);
		assertEquals(actualFileNamesPath, expectedFileNamesPath);
	}

	@After
	public void freeResources() {
		gitAccess.close();
		File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
		try {
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
