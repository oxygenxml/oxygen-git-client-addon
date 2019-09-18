package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessRestoreLastCommitTest {

	private final static String LOCAL_TEST_REPOSITPRY = "target/test-resource/GitAccessRestoreLastCommitTest";
	private GitAccess gitAccess;

	@Before
	public void init() {
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");

		try {
			file.createNewFile();
			PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
			out.println("modificare");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
		gitAccess.commit("file test added");
	}

	@Test
	public void testRestoreLastCommit() throws IOException {
		String actual = getFileContent();
		
		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("hellllo");
		out.close();

		gitAccess.restoreLastCommitFile(Arrays.asList("test.txt"));
		String expected = getFileContent();
		assertEquals(actual, expected);
	}

	private String getFileContent() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(LOCAL_TEST_REPOSITPRY + "/test.txt");
		BufferedReader br = new BufferedReader(fr);

		String sCurrentLine;

		String content = "";
		while ((sCurrentLine = br.readLine()) != null) {
			content += sCurrentLine;
		}
		br.close();
		fr.close();
		return content;
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
