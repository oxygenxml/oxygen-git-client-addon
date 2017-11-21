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

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GItAccessStagedFilesTest {


	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources";
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
	public void testGetStagedFilesForModify(){
		try {
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		List<FileStatus> actual = gitAccess.getStagedFile();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(GitChangeType.CHANGED, "test.txt"));
		assertEquals(actual, expected);
	}
	
	@Test
	public void testGetStagedFilesForAddedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/add.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		List<FileStatus> actual = gitAccess.getStagedFile();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(GitChangeType.ADD, "add.txt"));
		assertEquals(actual, expected);
	}
	
	@Test
	public void testGetStagedFilesForDeletedFiles() {
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		file.delete();
		
		gitAccess.addAll(gitAccess.getUnstagedFiles());
		List<FileStatus> actual = gitAccess.getStagedFile();
		List<FileStatus> expected = new ArrayList<FileStatus>();
		expected.add(new FileStatus(GitChangeType.REMOVED, "test.txt"));
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
