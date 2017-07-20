package com.oxygenxml.sdksamples.workspace.git.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

public class GitAccessUnstageFilesTest {

	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources";
	private GitAccess gitAccess;

	@Before
	public void init() {
		gitAccess = new GitAccess();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(ChangeType.ADD, file.getName()));
		gitAccess.commit("file test added");
	}

	@Test
	public void testGetUnstagedFilesForModifyFiles() {
		try {
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(ChangeType.MODIFY, "test.txt"));
		assertEquals(actual, expected);
	}

	@Test
	public void testGetUnstagedFilesForAddedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/add.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(ChangeType.ADD, "add.txt"));
		assertEquals(actual, expected);
	}

	@Test
	public void testGetUnstagedFilesForDeletedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.delete();

		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(ChangeType.DELETE, "test.txt"));
		assertEquals(actual, expected);
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
