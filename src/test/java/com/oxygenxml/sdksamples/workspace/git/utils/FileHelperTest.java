package com.oxygenxml.sdksamples.workspace.git.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.oxygenxml.git.utils.FileHelper;

public class FileHelperTest {

	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	
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
