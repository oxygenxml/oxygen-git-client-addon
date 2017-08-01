package com.oxygenxml.sdksamples.workspace.git.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.OptionsManager;

public class GitAccessPullTest {
	private final static String LOCAL_TEST_REPOSITPRY = "src/test/resources/local";
	private final static String SECOND_LOCAL_TEST_REPOSITORY = "src/test/resources/local2";
	private final static String REMOTE_TEST_REPOSITPRY = "src/test/resources/remote";
	private Repository db1;
	private Repository db2;
	private Repository db3;
	private GitAccess gitAccess;

	@Before
	public void init() throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException {
		gitAccess = GitAccess.getInstance();
		gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
		db1 = gitAccess.getRepository();
		gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
		db2 = gitAccess.getRepository();
		gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
		db3 = gitAccess.getRepository();
		
		gitAccess.setRepository(LOCAL_TEST_REPOSITPRY);
		OptionsManager.getInstance().saveSelectedRepository(LOCAL_TEST_REPOSITPRY);
		File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		StoredConfig config = gitAccess.getRepository().getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
		URIish uri = new URIish(db3.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();
		
		gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
		gitAccess.commit("file test added");
		gitAccess.push("", "");
		
		gitAccess.setRepository(REMOTE_TEST_REPOSITPRY);
		OptionsManager.getInstance().saveSelectedRepository(REMOTE_TEST_REPOSITPRY);
		System.out.println("UF = " + gitAccess.getUnstagedFiles());
		System.out.println("SF = " + gitAccess.getStagedFile());
		//for (FileStatus fileStatus : gitAccess.getStagedFile()) {
		//	gitAccess.restoreLastCommit(fileStatus.getFileLocation());
	//	}
		
		gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
		OptionsManager.getInstance().saveSelectedRepository(SECOND_LOCAL_TEST_REPOSITORY);
		config = gitAccess.getRepository().getConfig();
		remoteConfig = new RemoteConfig(config, "origin");
		uri = new URIish(db1.getDirectory().toURI().toURL());
		remoteConfig.addURI(uri);
		remoteConfig.update(config);
		config.save();
		
	}
	
	@Test
	public void testPush() throws URISyntaxException, IOException, InvalidRemoteException, TransportException, GitAPIException{
		System.out.println("UF Seco = " + gitAccess.getUnstagedFiles());
		System.out.println("SF Seco = " + gitAccess.getStagedFile());
		PullResponse response = gitAccess.pull("", "");
		System.out.println(response.getStatus());
	}
}
