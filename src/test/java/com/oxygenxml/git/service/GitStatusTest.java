package com.oxygenxml.git.service;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Asserts the status of the local repository. 
 */
public class GitStatusTest extends GitTestBase {
  
  protected final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitStatusTest/local";
  protected final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/GitStatusTest/local2";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitStatusTest/remote";
  protected GitAccess gitAccess;

  protected void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();
    createRepository(LOCAL_TEST_REPOSITORY);
    createRepository(SECOND_LOCAL_TEST_REPOSITORY);
    createRepository(REMOTE_TEST_REPOSITORY);
    Repository remoteRepo = gitAccess.getRepository();
    
    /**
     * Newly initialized repositories have a peculiarity in that no branch has yet been created. 
     * Though there is a HEAD (the pointer to the current branch) that references a branch, (named master by default) this very branch does not exist.
     * Usually, nothing to be worried about as with the first commit, the missing branch will be created. 
     */
    commitOneFile(REMOTE_TEST_REPOSITORY, "remote.txt", "remote");
    

    gitAccess.setRepository(LOCAL_TEST_REPOSITORY);
    StoredConfig config = gitAccess.getRepository().getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec1 = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec1);
    remoteConfig.update(config);
    
    String branchName = "master";
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    
    config.save();

    gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
    config = gitAccess.getRepository().getConfig();
    remoteConfig = new RemoteConfig(config, "origin");
    uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    remoteConfig.update(config);
    
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    
    config.save();
  }
  
  public void testPullsBehind() throws Exception {
    gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);

    PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test_second_local.txt");
    out.println("hellllo");
    out.close();
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test_second_local.txt"));
    gitAccess.commit("file test added1");
    gitAccess.push("", "");
    
    
    pushOneFileToRemote(SECOND_LOCAL_TEST_REPOSITORY, "test.txt", "hello");
    gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();
    
    int actual = gitAccess.getPullsBehind();
    int expected = 1;
    
    assertEquals(expected, actual);
  }

  @Test
  public void testNoPullsBehind() throws RepositoryNotFoundException, IOException{
    gitAccess.setRepository(SECOND_LOCAL_TEST_REPOSITORY);
    
    int actual = gitAccess.getPullsBehind();
    int expected = 0;
    
    assertEquals(expected, actual);
  }

}
