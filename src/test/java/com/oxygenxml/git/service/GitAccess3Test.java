package com.oxygenxml.git.service;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;

/**
 * Git test cases.
 */
public class GitAccess3Test extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private final static String LOCAL_BRANCH_NAME2 = "LocalBranch_2";
  private GitAccess gitAccess;
  private Repository remoteRepository;
  private Repository localRepository;
  
  String[] errMsg = new String[1];
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();
    
    createRepository(REMOTE_TEST_REPOSITORY);
    remoteRepository = gitAccess.getRepository();
    
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();
    
    bindLocalToRemote(localRepository , remoteRepository);
  }
  
  @Override
  public void tearDown() throws Exception {
    MessagePresenterProvider.setBuilder(null);
    
    super.tearDown();
  }
  
  /**
   * <p><b>Description:</b> test "obsolete" branches, i.e. local branches
   * that had an upstream, but do not have it anymore.</p>
   * <p><b>Bug ID:</b> EXM-53634</p>
   * 
   * @author sorin_carbunaru
   * 
   * @throws Exception
   */
  public void testObsoleteLocalBranches() throws Exception{
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    push("", "");
    refreshSupport.call();
    
    // ========================= Local branch 1 ===================================
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Change file on the secondary branch
    setFileContent(file, "local content on the first branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit");
    push("", "");
    refreshSupport.call();
    Thread.sleep(500);
    
    // ========================== Local branch 2 ===================================
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    gitAccess.setBranch(LOCAL_BRANCH_NAME2);
    assertEquals(LOCAL_BRANCH_NAME2, gitAccess.getRepository().getBranch());
    
    // ======================== no obsolete branch =================================
    List<Ref> obsoleteBranches = BranchesUtil.getLocalBranchesThatNoLongerHaveRemotes();
    assertTrue(obsoleteBranches.isEmpty());
    
    // ========================= Local branch 1 will become "obsolete" ==============================
    gitAccess.getGit()
      .push()
      .setRefSpecs(new RefSpec(":refs/heads/" + LOCAL_BRANCH_NAME1))
      .setRemote("origin")
      .call();
    
    obsoleteBranches = BranchesUtil.getLocalBranchesThatNoLongerHaveRemotes();
    assertEquals(1, obsoleteBranches.size());
    
  }
  
}
