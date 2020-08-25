package com.oxygenxml.git.service;

import java.io.File;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;


public class GitCheckoutBranchTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/local";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/remote";
  private final static String LOCAL_BRANCH_NAME = "LocalBranch";
  private final static String REMOTE_BRANCH_NAME = "RemoteBranch";
  

  private GitAccess gitAccess;
  private Repository remoteRepository;
  private Repository localRepository;
  @Before
  public void setUp() throws Exception {
    
    super.setUp();
    gitAccess = GitAccess.getInstance();
    
    //Creates the remote repository.
    createRepository(REMOTE_TEST_REPOSITORY);
    remoteRepository = gitAccess.getRepository();
    File file = new File(REMOTE_TEST_REPOSITORY + "remote.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME);
    
    //Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();
    file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME);
  }
  
  @Test
  public void testCheckoutBranch() throws Exception {
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepository , remoteRepository);
    
    gitAccess.fetch();
    
    //Checkout the remote branch
    gitAccess.checkoutRemoteBranch(REMOTE_BRANCH_NAME);
    
    Repository currentRepository = gitAccess.getRepository();
    
    assertEquals(localRepository, currentRepository);
    assertEquals(REMOTE_BRANCH_NAME, currentRepository.getBranch());
    
    
  }
}
