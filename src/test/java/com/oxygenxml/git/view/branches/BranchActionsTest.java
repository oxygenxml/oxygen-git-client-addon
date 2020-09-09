package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;

public class BranchActionsTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private final static String REMOTE_BRANCH_NAME1 = "RemoteBranch";
  private final static String LOCAL_BRANCH_NAME2 = "LocalBranch2";
  private final static String REMOTE_BRANCH_NAME2 = "RemoteBranch2";
  
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
    
    //Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();
    
    bindLocalToRemote(localRepository , remoteRepository);
  }
  /**
   * Tests the action of checkout of a local branch.
   * 
   * @throws Exception
   */
  @Test
  public void testCheckoutLocalBranchAction() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    gitAccess.fetch();
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals("master", initialBranchName);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(
        branchManagementPanel::refreshBranches);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
   
    //------------- Checkout the first branch in the list: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT_BRANCH))) {
        abstractAction.actionPerformed(null);
      }
    }
    sleep(500);
    gitAccess.fetch();
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    //------------- Checkout the next branch in the list: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode nextLeaf = (GitTreeNode)firstLeaf.getNextLeaf();
    String nextLeafPath = (String) nextLeaf.getUserObject();
    assertTrue(nextLeafPath.contains(Constants.R_HEADS));
    
    branchPath = nextLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME2, branchPath[branchPath.length - 1]);
    
    AbstractAction checkoutAction = branchTreeMenuActionsProvider.getCheckoutAction(nextLeaf);
    if (checkoutAction != null) {
      checkoutAction.actionPerformed(null);
      System.out.println("action done");
    }
    sleep(500);
    gitAccess.fetch();
    assertEquals(LOCAL_BRANCH_NAME2, gitAccess.getRepository().getBranch());
  }
  
  /**
   * Tests the action of creating a new branch from a local branch.
   * 
   * @throws Exception
   */
  @Test
  public void testCreateLocalBranchAction() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.fetch();
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals("master", initialBranchName);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(
        branchManagementPanel::refreshBranches);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    String[] split = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, split[split.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH))) {
        //TODO solve the dialogs that appear and ask for an input when executing the action.
        abstractAction.actionPerformed(null);
      }
    }
    sleep(500);
    gitAccess.fetch();
    String currentBranchName = gitAccess.getBranchInfo().getBranchName();

    assertEquals(LOCAL_BRANCH_NAME1, currentBranchName);
  }
  
  /**
   * Tests the action of deleting a local branch.
   * 
   * @throws Exception
   */
  @Test
  public void testDeleteLocalBranchAction() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.fetch();
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals("master", initialBranchName);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(
        branchManagementPanel::refreshBranches);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] split = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, split[split.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.DELETE_BRANCH))) {
      //TODO solve the dialogs that appear and ask for an input when executing the action.
        abstractAction.actionPerformed(null);
      }
    }
    sleep(500);
    gitAccess.fetch();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    BranchManagementTest.serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch2\n" +
        "    refs/heads/master\n",
        actualTree.toString());
  }
  
  /**
   * Tests the action of checkout a remote branch.
   * @throws Exception
   */
  @Test
  public void testCheckoutRemoteBranchAction() throws Exception{
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    
    File file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    //Create new remote branches
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals("master", initialBranchName);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(
        branchManagementPanel::refreshBranches);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    
    //------------- Checkout the last branch in the list: REMOTE_BRANCH_NAME2 -------------
    GitTreeNode lastLeaf = (GitTreeNode)root.getLastLeaf();
    String lastLeafPath = (String)lastLeaf.getUserObject();
    assertTrue(lastLeafPath.contains(Constants.R_REMOTES));
    
    String[] branchPath = lastLeafPath.split("/");
    assertEquals(REMOTE_BRANCH_NAME2, branchPath[branchPath.length - 1]);
    
    AbstractAction checkoutAction = branchTreeMenuActionsProvider.getCheckoutAction(lastLeaf);
    if(checkoutAction != null) {
      //TODO solve the dialogs that appear and ask for an input when executing the action.
      checkoutAction.actionPerformed(null);
    }
    sleep(500);
    gitAccess.fetch();
    assertEquals(REMOTE_BRANCH_NAME2, gitAccess.getRepository().getBranch());
    
    //------------- Checkout the previous branch in the list: REMOTE_BRANCH_NAME1 -------------
    GitTreeNode previousLeaf = (GitTreeNode)root.getLastLeaf();
    String previousLeafPath = (String)previousLeaf.getUserObject();
    assertTrue(lastLeafPath.contains(Constants.R_REMOTES));
    
    branchPath = previousLeafPath.split("/");
    assertEquals(REMOTE_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(previousLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT_BRANCH))) {
        //TODO solve the dialogs that appear and ask for an input when executing the action.
        abstractAction.actionPerformed(null);
      }
    }
    sleep(500);
    gitAccess.fetch();
    assertEquals(REMOTE_BRANCH_NAME1, gitAccess.getRepository().getBranch());
  }
}
