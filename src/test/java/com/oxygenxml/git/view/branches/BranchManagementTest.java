package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.Enumeration;

import javax.swing.JTree;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.GitTreeNode;

public class BranchManagementTest extends GitTestBase{
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
  
  
  private void serializeTree(StringBuilder stringTree, GitTreeNode currentNode) {
    int level = currentNode.getLevel();
    while(level != 0) {
      stringTree.append("  ");
      --level;
    }
    stringTree.append( currentNode.getUserObject());
    stringTree.append("\n");
    if (!currentNode.isLeaf()) {
      Enumeration<GitTreeNode> children = currentNode.children();
      while (children.hasMoreElements()) {
        serializeTree(stringTree, children.nextElement());
      }
    }
  }
  
  /**
   * <p><b>Description:</b> Tests the structure of a tree with both local and remote branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeStructure() throws Exception {
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    
    file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/master\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the structure of a tree with only local branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeStructureLocalBranchesOnly() throws Exception {
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/master\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the structure of a tree with only remote branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeStructureRemoteBranchesOnly() throws Exception{
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the structure of a tree with only master remote branch.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeStructureRemoteMasterOnly() throws Exception {
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    File file = new File(REMOTE_TEST_REPOSITORY + "remote.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote.txt"));
    gitAccess.commit("First remote commit.");
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the filter for branches on a tree with only local branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeFilterLocalBranchesOnly() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    branchManagementPanel.filterTree("heads");
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Local");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("as");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/master\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("h2");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch2\n",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the filter for branches on a tree with only remote branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeFilterRemoteBranchesOnly() throws Exception{
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    branchManagementPanel.filterTree("remotes");
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("mote");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
            "  refs/remotes/\n" + 
            "    refs/remotes/origin/\n" + 
            "      refs/remotes/origin/RemoteBranch\n" + 
            "      refs/remotes/origin/RemoteBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ter");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
            "  refs/remotes/\n" + 
            "    refs/remotes/origin/\n" + 
            "      refs/remotes/origin/master\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ch2");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
            "  refs/remotes/\n" + 
            "    refs/remotes/origin/\n" + 
            "      refs/remotes/origin/RemoteBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("te");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
            "  refs/remotes/\n" + 
            "    refs/remotes/origin/\n" + 
            "      refs/remotes/origin/master\n" + 
            "      refs/remotes/origin/RemoteBranch\n" + 
            "      refs/remotes/origin/RemoteBranch2\n",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the filter for branches on a tree with both local and remote branches.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeFilterAllBranches() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    
    file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    branchManagementPanel.filterTree("ewu82m");
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("as");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/master\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Branch");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("2");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("al");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Rem");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("te");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/master\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("a");
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/master\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/master\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
}
