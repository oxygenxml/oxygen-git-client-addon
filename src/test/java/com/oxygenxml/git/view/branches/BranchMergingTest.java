package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitController;
/**
 * Test cases for the actions that can be done on a branch.
 * 
 * @author gabriel_nedianu
 *
 */
public class BranchMergingTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessMerging/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
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
  
  /**
   * <p><b>Description:</b>Tests the branch merging.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testBranchMerging() throws Exception{
    File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    //Make the first commit for the local repository on the main branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    
    //------------- Checkout the other branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT))) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    flushAWT();
    
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Change file on the secondary branch
    setFileContent(file, "local content for merging");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit");
  
    // Move back to the main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getBranchInfo().getBranchName());
    
    // Merge LocalBranch into main
    List<AbstractAction> actionsForSecondaryBranch = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction action : actionsForSecondaryBranch) {
      if (action != null) {
        String actionName = action.getValue(AbstractAction.NAME).toString();
        if("Merge_Branch1_Into_Branch2".equals(actionName)) {
          action.actionPerformed(null);
          break;
        }
      }
    }
    flushAWT();
    
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    
  }
  
  /**
   * <p><b>Description:</b>Tests the branch merging. Conflict happens.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testBranchMergingWithConflict() throws Exception {
    JDialog conflictMergeDialog = null;
    try {

    File file1 = new File(LOCAL_TEST_REPOSITORY, "local1.txt");
    File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
    file1.createNewFile();
    file2.createNewFile();

    setFileContent(file1, "local file 1 content");
    setFileContent(file2, "local file 2 content");

    // Make the first commit for the local repository and create a new branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("First local commit on main.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);

    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    // ------------- Checkout branch: LOCAL_BRANCH_NAME1  -------------
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    
    // Commit on this branch
    setFileContent(file1, "local file 1 on new branch");
    setFileContent(file2, "local file 2 on new branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Commit on secondary branch");

    // ------------- Move to the main branch and commit something there ---------------
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    setFileContent(file1, "local file 1 modifications");
    setFileContent(file2, "local file 2 modifications");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("2nd commit on main branch");

    // Merge secondary branch into main
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
    String secondaryBranchPath = (String) secondaryBranchNode.getUserObject();
    assertTrue(secondaryBranchPath.contains(Constants.R_HEADS));
    
    List<AbstractAction> actionsForSecondaryBranch = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
    for (AbstractAction action : actionsForSecondaryBranch) {
      if (action != null) {
        String actionName = action.getValue(AbstractAction.NAME).toString();
        if("Merge_Branch1_Into_Branch2".equals(actionName)) {
          SwingUtilities.invokeLater(() -> action.actionPerformed(null));
          break;
        }
      }
    }
    flushAWT();

    conflictMergeDialog = findDialog(translator.getTranslation(Tags.MERGE_CONFLICTS_TITLE));
    assertNotNull(conflictMergeDialog);

    assertTrue(TestUtil.read(file1.toURI().toURL()).
        contains("<<<<<<< HEAD\n" 
            + "local file 1 modifications\n" 
            + "=======\n" 
            + "local file 1 on new branch\n" 
            + ">>>>>>>"));

    assertTrue(TestUtil.read(file2.toURI().toURL()).
        contains("<<<<<<< HEAD\n" 
            + "local file 2 modifications\n" 
            + "=======\n" 
            + "local file 2 on new branch\n" 
            + ">>>>>>>"));
    } finally {
      if (conflictMergeDialog != null) {
        conflictMergeDialog.setVisible(false);
        conflictMergeDialog.dispose();
      }
    }
  }
  
  /**
   *<p><b>Description:</b>Tests the failing merging.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  @Test
  public void testFailingBranchMerging() throws Exception {
    JDialog mergeFailDialog = null;

    try {

      File file1 = new File(LOCAL_TEST_REPOSITORY, "local1.txt");
      File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
      file1.createNewFile();
      file2.createNewFile();

      setFileContent(file1, "local file 1 content");
      setFileContent(file2, "local file 2 content");

      // Make the first commit for the local repository, create a new branch and move on it
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
      gitAccess.commit("First local commit on main.");
      
      // Create new branch and commit some changes
      gitAccess.createBranch(LOCAL_BRANCH_NAME1);
      gitAccess.setBranch(LOCAL_BRANCH_NAME1);

      setFileContent(file1, "branch file1 modification ");
      setFileContent(file2, "branch file2 modification ");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
      gitAccess.commit("Commit on secondary branch");

      // Move on main branch, make some uncommitted modifications
      gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
      
      setFileContent(file1, "file1 something xx...xx...");
      setFileContent(file2, "file2 something xx...xx...");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
      
      // Try to merge the secondary branch into the default one - CHECKOUT CONFLICT
      GitControllerBase mock = new GitController(GitAccess.getInstance());
      BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
      branchManagementPanel.refreshBranches();
      flushAWT();
      
      BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
      GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
      GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
      
      List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
      for (AbstractAction action : actionsForNode) {
        if (action != null) {
          String actionName = action.getValue(AbstractAction.NAME).toString();
          if("Merge_Branch1_Into_Branch2".equals(actionName)) {
            SwingUtilities.invokeLater(() -> action.actionPerformed(null));
            break;
          }
        }
      }

      mergeFailDialog = findDialog(translator.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE));
      assertNotNull(mergeFailDialog);
      
      mergeFailDialog.setVisible(false);
      mergeFailDialog.dispose();

      //Commit the changes on the main branch then make other uncommitted changes and try to merge again
      // MERGE FAILED
      gitAccess.commit("Commit on main branch");

      setFileContent(file1, "file1 something modif");
      setFileContent(file2, "file2 something modif");
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
      gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));

      actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode);
      for (AbstractAction action : actionsForNode) {
        if (action != null) {
          String actionName = action.getValue(AbstractAction.NAME).toString();
          if("Merge_Branch1_Into_Branch2".equals(actionName)) {
            SwingUtilities.invokeLater(() -> action.actionPerformed(null));
            break;
          }
        }
      }

      mergeFailDialog = findDialog(translator.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE));
      assertNotNull(mergeFailDialog);

    } finally {
      if (mergeFailDialog != null) {
        mergeFailDialog.setVisible(false);
        mergeFailDialog.dispose();
      }
    }
  }
  
}
