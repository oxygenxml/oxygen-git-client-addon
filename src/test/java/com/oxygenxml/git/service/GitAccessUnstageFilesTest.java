package com.oxygenxml.git.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;

import junit.framework.TestCase;

public class GitAccessUnstageFilesTest extends TestCase {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessUnstageFilesTest";
	private static final File testDIr = new File(LOCAL_TEST_REPOSITPRY);
	private GitAccess gitAccess = GitAccess.getInstance();

	public void setUp() throws Exception {
	  testDIr.mkdirs();
		File file = new File(testDIr, "test.txt");
		file.createNewFile();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
		gitAccess.commit("file test added");
	}
	
	 public void tearDown() throws Exception {
	    gitAccess.cleanUp();
	    FileUtil.deleteRecursivelly(testDIr);
	  }

	public void testGetUnstagedFilesForModifyFiles() {
		try {
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
		assertEquals(actual.toString(), expected.toString());
	}

	public void testGetUnstagedFilesForAddedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/add.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.UNTRACKED, "add.txt"));
		assertEquals(actual, expected);
	}

	public void testGetUnstagedFilesForDeletedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.delete();

		List<FileStatus> actual = gitAccess.getUnstagedFiles();
		List<FileStatus> expected = new ArrayList<>();
		expected.add(new FileStatus(GitChangeType.MISSING, "test.txt"));
		assertEquals(actual, expected);
	}

}
