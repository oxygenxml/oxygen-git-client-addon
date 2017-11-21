package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.RepositoryState;
import org.junit.Test;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

public class GitAccessConflictTest extends GitAccessPullTest {

	@Test
	public void testResolveUsingTheirs() throws RepositoryNotFoundException, FileNotFoundException,
			InvalidRemoteException, TransportException, IOException, GitAPIException {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		gitAccess.updateWithRemoteFile("test.txt");
		String expected = "hellllo";
		String actual = getFileContent();
		assertEquals(expected, actual);
	}

	@Test
	public void testRestartMerge() throws RepositoryNotFoundException, FileNotFoundException, InvalidRemoteException, TransportException, IOException, GitAPIException, NoRepositorySelected {
		pushOneFileToRemote();

		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
		file.createNewFile();

		PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
		out.println("teeeeeest");
		out.close();

		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("conflict");
		gitAccess.pull("", "");
		for (FileStatus fileStatus : gitAccess.getUnstagedFiles()) {
			if(fileStatus.getChangeType() == GitChangeType.CONFLICT){
				gitAccess.add(fileStatus);
			}
		}
		RepositoryState actual = gitAccess.getRepository().getRepositoryState();
		RepositoryState expected = RepositoryState.MERGING_RESOLVED;
		assertEquals(expected, actual);
		
		gitAccess.restartMerge();
		actual = gitAccess.getRepository().getRepositoryState();
		expected = RepositoryState.MERGING;
		assertEquals(expected, actual);
	}

	private String getFileContent() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
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
}
