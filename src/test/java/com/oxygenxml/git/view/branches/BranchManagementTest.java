package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.GitTreeNode;
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
  
  @Override
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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
  @Test
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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
    
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
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
   * <p><b>Description:</b> Tests the tool tips for branches on a tree with both local and remote branches.</p>
   * <p><b>Bug ID:</b> EXM-46438</p>
   * 
   * @author Alex Smarandache
   * 
   * @throws Exception
   */
  @Test
  public void testBranchesTreeToolTipBranches() throws Exception {
    final String AUTHOR_DETAILS = "Author: AlexJitianu alex_jitianu@sync.ro";
    Map<String, Date> lastCommitDetailsForAllBranchesMap = new HashMap<>();
    
    // Local repo
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);
    try (RevWalk walk = new RevWalk(gitAccess.getRepository())) {
      List<Ref> localBranches = gitAccess.getLocalBranchList();
      for (Ref localBranch : localBranches) {
        Ref tracking = gitAccess.getRepository().exactRef(localBranch.getName());
        RevCommit trackingCommit = walk.parseCommit(tracking.getObjectId());
        lastCommitDetailsForAllBranchesMap.put(localBranch.getName(), trackingCommit.getAuthorIdent().getWhen());
      }
    }
    
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
    try (RevWalk walk = new RevWalk(gitAccess.getRepository())) {
      List<Ref> remoteBranches = gitAccess.getRemoteBrachListForCurrentRepo();
      for (Ref remoteBranch : remoteBranches) {
        Ref tracking = gitAccess.getRepository().exactRef(remoteBranch.getName());
        RevCommit trackingCommit = walk.parseCommit(tracking.getObjectId());
        lastCommitDetailsForAllBranchesMap.put(remoteBranch.getName(), trackingCommit.getAuthorIdent().getWhen());
      }
    }
     
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(Mockito.mock(GitControllerBase.class));
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode leaf = (GitTreeNode) root.getFirstLeaf();
    
    JLabel toolTipLabel = (JLabel) tree.getCellRenderer().getTreeCellRendererComponent(tree, root, false, true, root.isLeaf(), 0, true);
    assertNull(toolTipLabel.getToolTipText());
    
    //  Tests the tool tips for all branches
    for (int i = 0; i < root.getLeafCount(); i++) {
      toolTipLabel = (JLabel) tree.getCellRenderer().getTreeCellRendererComponent(tree, leaf, false, true, leaf.isLeaf(), 0, true);
      assertTrue(toolTipLabel.getToolTipText().contains(AUTHOR_DETAILS));
      assertTrue(toolTipLabel.getToolTipText().contains(lastCommitDetailsForAllBranchesMap.get(leaf.toString()).toString()));
      leaf = (GitTreeNode) leaf.getNextLeaf();
    }
  }
}
