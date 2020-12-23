package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Push test cases.
 */
public class GitAccessPush2Test extends GitTestBase {
  
  private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessPush2Test/local";
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessPush2Test/remote";
  private Repository localRepo;
  private Repository remoteRepo;
  private GitAccess gitAccess;
  
  /**
   * Set up before each test.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    deleteTestResources();
    
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
    localRepo = gitAccess.getRepository();

    bindLocalToRemote(localRepo, remoteRepo);
  }

  /**
   * <p><b>Description:</b> push to a remote branch that has a different name than the local one.</p>
   * <p><b>Bug ID:</b> EXM-47020</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testPushToRemoteWithNameDifferentThanLocal() throws Exception {
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITPRY);
    
    // Push to init remote.
    File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
    file.createNewFile();
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    gitAccess.push("", "");
    
    gitAccess.checkoutRemoteBranchWithNewName("localBranch", "master");
    String detectedBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals("localBranch", detectedBranchName);
    assertEquals("origin/master", gitAccess.getUpstreamBranchShortNameFromConfig(detectedBranchName));
    
    setFileContent(new File("test.txt"), "NEW TEXT CONTENT");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    assertEquals(1, gitAccess.getPushesAhead());
    gitAccess.push("", "");
    assertEquals(0, gitAccess.getPushesAhead());

    // The commit has made it to the remote repo
    assertEquals(
        localRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"),
        remoteRepo.resolve(gitAccess.getLastLocalCommitInRepo().getName() + "^{commit}"));
  }
  
  /**
   * Tear each test down.
   */
  @Override
  public void tearDown() throws Exception {
    gitAccess.closeRepo();
    localRepo.close();
    remoteRepo.close();
    
    deleteTestResources();
    
    super.tearDown();
  }

  /**
   * Delete test resources.
   */
  private void deleteTestResources() {
    File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
    try {
      FileUtils.deleteDirectory(dirToDelete);
      dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
      FileUtils.deleteDirectory(dirToDelete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
