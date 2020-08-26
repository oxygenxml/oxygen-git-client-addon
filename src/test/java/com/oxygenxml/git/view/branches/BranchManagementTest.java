package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

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
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/local";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/remote";
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
    File file = new File(REMOTE_TEST_REPOSITORY + "remote1.txt");
    file.createNewFile();
    setFileContent(file, "remote content");
    
    //Make the first commit for the remote repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "remote1.txt"));
    gitAccess.commit("First remote commit.");
    gitAccess.createBranch(REMOTE_BRANCH_NAME1);
    gitAccess.createBranch(REMOTE_BRANCH_NAME2);

    
    //Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();
    file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    //Make the first commit for the local repository and create a branch for it.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    gitAccess.createBranch(LOCAL_BRANCH_NAME2);

    
    bindLocalToRemote(localRepository , remoteRepository);
  }
  
  /**
   * <p><b>Description:</b> branches in current repo.</p>
   *
   * @author Bogdan Draghici
   *
   * @throws Exception
   */
  @Test
  public void testBranchesTreeContent() throws Exception {
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    assertEquals(2, root.getChildCount());
    
    GitTreeNode localTag = (GitTreeNode)root.getFirstChild();
    assertEquals("Local",(String)localTag.getUserObject());
    
    GitTreeNode remoteTag = (GitTreeNode)root.getLastChild();
    assertEquals("Remote",(String)remoteTag.getUserObject());
    
    //===================== Local branches comparison ==============================
    //asserting with lists
    Enumeration<?> breadthFirstLocalEnumeration = localTag.breadthFirstEnumeration();
    List<String> expectedLocalBranches = new ArrayList<>();
    expectedLocalBranches.add("Local");
    expectedLocalBranches.add("LocalBranch");
    expectedLocalBranches.add("LocalBranch2");
    expectedLocalBranches.add("master");
    List<String> actualLocalBranches = new ArrayList<>();
    while(breadthFirstLocalEnumeration.hasMoreElements()) {
      GitTreeNode node = (GitTreeNode) breadthFirstLocalEnumeration.nextElement();
      actualLocalBranches.add((String)node.getUserObject());
      assertTrue(node.isLeaf() || "Local".contentEquals((String)node.getUserObject()));
    }
    assertTrue(expectedLocalBranches.containsAll(actualLocalBranches));
    assertEquals(expectedLocalBranches.size(), actualLocalBranches.size());
    
    ListIterator<String> expectedListIterator = expectedLocalBranches.listIterator();
    ListIterator<String> actualListIterator = actualLocalBranches.listIterator();
    while(actualListIterator.hasNext()) {
      String actual = actualListIterator.next();
      String expected = expectedListIterator.next();
      assertEquals(expected, actual);
    }
    
    //===================== Remote branches comparison ==============================
    //asserting with array and enumeration
    assertEquals(remoteTag.getChildCount(), 1);
    GitTreeNode originTag = (GitTreeNode) remoteTag.getFirstChild();
    assertEquals("origin", (String)originTag.getUserObject());
    Enumeration<?> breadththFirstOriginEnumeration = originTag.breadthFirstEnumeration();
    String[]remoteBranches = {"origin", "master", "RemoteBranch", "RemoteBranch2"};
    int remoteStringIterator = 0;
    while(breadththFirstOriginEnumeration.hasMoreElements()) {
      GitTreeNode node = (GitTreeNode) breadththFirstOriginEnumeration.nextElement();
      assertTrue(node.isLeaf() || "origin".contentEquals((String)node.getUserObject()));
      assertEquals(remoteBranches[remoteStringIterator], (String)node.getUserObject());
      assertTrue(remoteStringIterator < remoteBranches.length);
      ++remoteStringIterator;
    }
    assertEquals(remoteBranches.length, remoteStringIterator);
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
  
  @Test
  public void testBranchesTreeStructure() throws Exception {
    gitAccess.fetch();

    BranchManagementPanel branchManagementPanel = new BranchManagementPanel();
    JTree tree = branchManagementPanel.getTree();
    GitTreeNode root = (GitTreeNode)tree.getModel().getRoot();
    
    StringBuilder actualTree = new StringBuilder();
    serializeTree(actualTree, root);
    System.out.println(actualTree.toString());
    
    StringBuilder expectedTree = new StringBuilder();
    expectedTree.append("\n");
    expectedTree.append("  Local\n");
    expectedTree.append("    LocalBranch\n");
    expectedTree.append("    LocalBranch2\n");
    expectedTree.append("    master\n");
    expectedTree.append("  Remote\n");
    expectedTree.append("    origin\n");
    expectedTree.append("      master\n");
    expectedTree.append("      RemoteBranch\n");
    expectedTree.append("      RemoteBranch2\n");
    
    assertEquals(expectedTree.toString(), actualTree.toString());
  }
}
