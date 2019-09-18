package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessRemoveTest {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resource/GitAccessRemoveTest";
	private GitAccess gitAccess;

	@Before
	public void init() {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
		gitAccess.commit("file test added");
	}

	@Test
	public void testRemoveModifyFile() {
		try {
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<FileStatus> files = gitAccess.getUnstagedFiles();
		gitAccess.addAll(files);
		gitAccess.resetAll(files);
		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveAddFile() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/add.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<FileStatus> files = gitAccess.getUnstagedFiles();
		gitAccess.addAll(files);
		gitAccess.resetAll(files);
		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.UNTRACKED, "add.txt"));
		assertEquals(expected, actual);
	}

	@Test
	public void testRemoveDeleteFile() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.delete();

		List<FileStatus> files = gitAccess.getUnstagedFiles();
		gitAccess.addAll(files);
		gitAccess.resetAll(files);
		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.MISSING, "test.txt"));
		assertEquals(expected, actual);
	}

	@After
	public void freeResources() {
		gitAccess.closeRepo();
		File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
		try {
			FileUtils.deleteDirectory(dirToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
