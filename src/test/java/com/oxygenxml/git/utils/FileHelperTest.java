package com.oxygenxml.git.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

public class FileHelperTest {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/FileHelperTest/local";
	
	@Test
	public void testIsNotGitRepositoery() throws IOException{
		File file = new File(LOCAL_TEST_REPOSITPRY);
		
		file.mkdirs();
		
		boolean actual = FileHelper.isGitRepository(file.getAbsolutePath());
		boolean expected = false;
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testIsGitRepositoery() throws IOException{
		File repositoryDirectory = new File(LOCAL_TEST_REPOSITPRY);
		repositoryDirectory.mkdirs();
		File gitDirectory = new File(LOCAL_TEST_REPOSITPRY + "/.git");
		gitDirectory.mkdirs();
		
		boolean actual = FileHelper.isGitRepository(repositoryDirectory.getAbsolutePath());
		boolean expected = true;
		
		assertEquals(expected, actual);
	}
	
	/**
	 * Test the {@link FileHelper#getCommonDir(java.util.Set)} method.
	 * 
	 * @throws IOException
	 */
	@Test
  public void testGetCommonDirectory() throws IOException {
	  Set<File> files = new HashSet<>();
	  File firstFile = new File("/home/user1/tmp/coverage/test");
    files.add(firstFile);
	  files.add(new File("/home/user1/tmp/covert/operator"));
	  files.add(new File("/home/user1/tmp/coven/members"));
	  assertEquals(new File("/home/user1/tmp/").getCanonicalPath(), FileHelper.getCommonDir(files).getCanonicalPath());
	  
	  files.remove(firstFile);
	  files.add(new File("/notHome/user1/tmp/coverage/test"));
	  assertEquals(new File("/").getCanonicalPath(), FileHelper.getCommonDir(files).getCanonicalPath());
	}
	
	@After
	public void freeResources() {
		
		try {
			File dir = new File(LOCAL_TEST_REPOSITPRY);
			FileUtils.deleteDirectory(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
