package com.oxygenxml.git.view.branches;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.util.UIUtil;
/**
 * Test cases for the structure of the branches tree.
 * 
 * @author Bogdan Draghici
 *
 */
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
  
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(UIUtil.DATE_FORMAT_PATTERN);
  
  @Override
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
  
  
  protected static void serializeTree(StringBuilder stringTree, GitTreeNode currentNode) {
    int level = currentNode.getLevel();
    while(level != 0) {
      stringTree.append("  ");
      --level;
    }
    stringTree.append( currentNode.getUserObject());
    stringTree.append("\n");
    if (!currentNode.isLeaf()) {
      Enumeration<TreeNode> children = currentNode.children();
      while (children.hasMoreElements()) {
        serializeTree(stringTree, (GitTreeNode) children.nextElement());
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "localRepository\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   * <p><b>Description:</b> Tests the structure of a tree with only main remote branch.</p>
   * <p><b>Bug ID:</b> EXM-41701</p>
   *
   * @author bogdan_draghici
   *
   * @throws Exception
   */
  public void testBranchesTreeStructureRemoteMainOnly() throws Exception {
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    File file = new File(REMOTE_TEST_REPOSITORY + "remote.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote.txt"));
    gitAccess.commit("First remote commit.");
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    
    assertEquals(
        "localRepository\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    branchManagementPanel.filterTree("hedz");
    flushAWT();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Local");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("in");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("h2");
    flushAWT();
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    branchManagementPanel.filterTree("rimotz");
    flushAWT();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("mote");
    flushAWT();
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
    
    branchManagementPanel.filterTree("ain");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
            "  refs/remotes/\n" + 
            "    refs/remotes/origin/\n" + 
            "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ch2");
    flushAWT();
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
    flushAWT();
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
  public void testBranchesTreeFilterAllBranches() throws Exception {
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    branchManagementPanel.filterTree("ewu82m");
    flushAWT();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ai");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Branch");
    flushAWT();
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
    flushAWT();
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
    flushAWT();
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
    flushAWT();
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
    
    branchManagementPanel.filterTree("ai");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("a");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  
  /**
   * <p><b>Description:</b> Tests the filter for branches on a tree with both local and remote branches with more remotes.</p>
   * <p><b>Bug ID:</b>  EXM-49458</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeCustomRemote() throws Exception {
    bindLocalToRemote(localRepository , remoteRepository, "fork", "main");
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    branchManagementPanel.filterTree("ewu82m");
    flushAWT();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ai");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" +
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("Branch");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/RemoteBranch\n" + 
        "      refs/remotes/fork/RemoteBranch2\n" +
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n",
        actualTree.toString());
    
    branchManagementPanel.filterTree("2");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/RemoteBranch2\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("al");
    flushAWT();
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
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/RemoteBranch\n" + 
        "      refs/remotes/fork/RemoteBranch2\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("ai");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "",
        actualTree.toString());
    
    branchManagementPanel.filterTree("a");
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch\n" + 
        "    refs/heads/LocalBranch2\n" + 
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/fork/\n" + 
        "      refs/remotes/fork/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/fork/RemoteBranch\n" + 
        "      refs/remotes/fork/RemoteBranch2\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  
  
  /**
   * <p><b>Description:</b> Tests the tool tips for branches on a tree with both local and remote branches.</p>
   * <p><b>Bug ID:</b> EXM-46438</p>
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testBranchesTreeToolTips() throws Exception {
    // Local repo
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    // Remote repo
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);
    
    // Local repo again
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    DefaultMutableTreeNode leaf = root.getFirstLeaf();
    JLabel rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 2, true);
    assertEquals(
        "<html><p>Local_branch LocalBranch<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 3, true);
    assertEquals(
        "<html><p>Local_branch LocalBranch2<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 4, true);
    assertEquals(
        "<html><p>Local_branch main<br>"
        // Also has upstream
        + "Upstream_branch origin/main<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 7, true);
    String remoteURL = gitAccess.getRemoteURLFromConfig();
    assertEquals(
        "<html><p>Remote_branch origin/main<br>"
        + "Clone_Repository_Dialog_Url_Label: "
        + "<a href=\"" + remoteURL + "\">"
        + remoteURL + "</a><br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 8, true);
    assertEquals(
        "<html><p>Remote_branch origin/RemoteBranch<br>"
            + "Clone_Repository_Dialog_Url_Label: "
            + "<a href=\"" + remoteURL + "\">"
            + remoteURL + "</a><br><br>"
            + "Last_Commit_Details:<br>"
            + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
            + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 9, true);
    assertEquals(
        "<html><p>Remote_branch origin/RemoteBranch2<br>"
            + "Clone_Repository_Dialog_Url_Label: "
            + "<a href=\"" + remoteURL + "\">"
            + remoteURL + "</a><br><br>"
            + "Last_Commit_Details:<br>"
            + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
            + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    assertNull(leaf.getNextLeaf());
    
  }
  
  /**
   * <p><b>Description:</b> Tests the tool tips for branches that contain slashes
   * in their names.</p>
   * <p><b>Bug ID:</b> EXM-48643</p>
   * 
   * @author sorin_carbunaru
   * 
   * @throws Exception
   */
  @Test
  public void testBranchesTreeToolTips_2() throws Exception {
    // Local repo
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch("the/breanci");
    
    // Local repo again
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    DefaultMutableTreeNode leaf = root.getFirstLeaf();
    JLabel rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 3, true);
    assertEquals("breanci", rendererLabel.getText());
    assertEquals(
        "<html><p>Local_branch the/breanci<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 4, true);
    assertEquals(
        "<html><p>Local_branch main<br>"
        // Also has upstream
        + "Upstream_branch origin/main<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    assertNull(leaf.getNextLeaf());
    
  }
  
  
  /**
   * <p><b>Description:</b> Tests the tool tips for branches that contain slashes
   * in their names with a custom remote.</p>
   * <p><b>Bug ID:</b> EXM-49458</p>
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testBranchesTreeToolTipsMultipleRemotes() throws Exception {
    final String localTestRepository  = "target/test-resources/GitAccessCheckoutNewBranch/localRepository";
    final String remoteTestRepository = "target/test-resources/GitAccessCheckoutNewBranch/remoteRepository";
    
    //Creates the remote repository.
    createRepository(remoteTestRepository);
    remoteRepository = gitAccess.getRepository();
    
    //Creates the local repository.
    createRepository(localTestRepository);
    localRepository = gitAccess.getRepository();
    
    bindLocalToRemote(localRepository , remoteRepository, "fork", "main");
    
    // Local repo
    File file = new File(localTestRepository + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch("the/breanci");
    
    // Local repo again
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    DefaultMutableTreeNode leaf = root.getFirstLeaf();
    JLabel rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 3, true);
    assertEquals("breanci", rendererLabel.getText());
    assertEquals(
        "<html><p>Local_branch the/breanci<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 4, true);
    assertEquals(
        "<html><p>Local_branch main<br>"
        // Also has upstream
        + "Upstream_branch fork/main<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    assertNull(leaf.getNextLeaf());
    
  }
  
  
  /**
   * <p><b>Description:</b> Tests the tool tips for deleted branches on a tree with both local and remote branches.</p>
   * <p><b>Bug ID:</b> EXM-48768</p>
   * 
   * @author Alex_Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testBranchesTreeToolTipsDeletedBranch() throws Exception {
    // Local repo
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    
    // Remote repo
    gitAccess.setRepositorySynchronously(REMOTE_TEST_REPOSITORY);
    file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);
    
    // Local repo again
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    gitAccess.fetch();
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitController.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    JTree tree = branchManagementPanel.getTree();
    gitAccess.deleteBranches(Arrays.asList(LOCAL_BRANCH_NAME2));
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    DefaultMutableTreeNode leaf = root.getFirstLeaf();
    JLabel rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 2, true);
    assertEquals(
        "<html><p>Local_branch LocalBranch<br><br>"
        + "Last_Commit_Details:<br>"
        + "- Author: AlexJitianu &lt;alex_jitianu@sync.ro&gt;<br> "
        + "- Date: {date}</p></html>".replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date())),
        rendererLabel.getToolTipText());
    
    leaf = leaf.getNextLeaf();
    rendererLabel = (JLabel) tree.getCellRenderer()
        .getTreeCellRendererComponent(tree, leaf, false, true, true, 3, true);
    assertNull(rendererLabel.getToolTipText());
    
  }
  
}
