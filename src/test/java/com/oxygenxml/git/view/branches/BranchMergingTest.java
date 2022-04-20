package com.oxygenxml.git.view.branches;

import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.dialog.DialogPresenter;
import com.oxygenxml.git.view.dialog.SquashMergeDialog;
import com.oxygenxml.git.view.dialog.internal.IDialogPresenter;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
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
  
  String[] errMsg = new String[1];
  
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
  
  /**
   * <p><b>Description:</b>Tests the branch merging.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
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
    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    refreshSupport.call();
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    refreshSupport.call();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.CHECKOUT);
    refreshSupport.call();
    flushAWT();
    
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Change file on the secondary branch
    setFileContent(file, "local content for merging");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit 10");
    refreshSupport.call();
    
    // Move back to the main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getBranchInfo().getBranchName());
    refreshSupport.call();
    // Merge LocalBranch into main
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
    flushAWT();
    
    //Confirm merge dialog
    JDialog mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
    JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
    mergeOkButton.doClick();
    
    sleep(200);
    
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    
  }
  
  /**
   * <p><b>Description:</b>Tests the branch squash and merge.</p>
   * <p><b>Bug ID:</b> EXM-49976</p>
   * 
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void testBranchSquashMerging() throws Exception{
    final File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    
    final String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    //Make the first commit for the local repository on the main branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    
    //------------- Checkout the other branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitControllerBase mock = new GitController();
    final BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    refreshSupport.call();
    final GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    final GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    final String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    refreshSupport.call();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.CHECKOUT);
    refreshSupport.call();
    flushAWT();
    
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Change file on the secondary branch
    setFileContent(file, "local content for merging");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit");
    refreshSupport.call();
    
    final File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
    file2.createNewFile();
    setFileContent(file2, "squash content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Branch commit 2");
    refreshSupport.call();
    
    // Move back to the main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getBranchInfo().getBranchName());
    refreshSupport.call();
    // Merge LocalBranch into main
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.SQUASH_MERGE_ACTION_NAME + "...");
    flushAWT();
    
    final SquashMergeDialog squashMergeBranchesDialog = (SquashMergeDialog)findDialog(
        translator.getTranslation(Tags.SQUASH_MERGE));
    assertNotNull(squashMergeBranchesDialog);
    JButton mergeOkButton = findFirstButton(squashMergeBranchesDialog, Tags.SQUASH_AND_COMMIT);
    mergeOkButton.doClick();
    
    sleep(200);
    
    final List<CommitCharacteristics> commits = GitAccess.getInstance().getCommitsCharacteristics(
        HistoryStrategy.CURRENT_LOCAL_BRANCH, null, null);
    assertEquals(2, commits.size());
    for(CommitCharacteristics commit : commits) {
      assertTrue(commit.getPlotCommit().getParentCount() < 2); // test if every commit has maximum one parent.
    }
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    assertEquals("squash content", TestUtil.read(file2.toURI().toURL()));

  }
  
  
  /**
   * <p><b>Description:</b>Tests squash and merge action case when we don't have new changes.</p>
   * <p><b>Bug ID:</b> EXM-50356</p>
   * 
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void testBranchSquashMergingWithoutChanges() throws Exception {
    final File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    
    final String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    //Make the first commit for the local repository on the main branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    
    //------------- Checkout the other branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitControllerBase mock = new GitController();
    final BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    refreshSupport.call();
    final GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    final GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    final String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    refreshSupport.call();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.CHECKOUT);
    refreshSupport.call();
    flushAWT();
    
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Change file on the secondary branch
    setFileContent(file, "local content for merging");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Branch commit");
    refreshSupport.call();
    
    final File file2 = new File(LOCAL_TEST_REPOSITORY, "local2.txt");
    file2.createNewFile();
    setFileContent(file2, "squash content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Branch commit 2");
    refreshSupport.call();
    
    // Move back to the main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getBranchInfo().getBranchName());
    refreshSupport.call();
    // Merge LocalBranch into main
    
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), 
        Tags.SQUASH_MERGE_ACTION_NAME + "...");
    flushAWT();
    
    SquashMergeDialog squashMergeBranchesDialog = (SquashMergeDialog)findDialog(
        translator.getTranslation(Tags.SQUASH_MERGE));
    assertNotNull(squashMergeBranchesDialog);
    JButton mergeOkButton = findFirstButton(squashMergeBranchesDialog, Tags.SQUASH_AND_COMMIT);
    mergeOkButton.doClick();
    
    sleep(200);
    
    List<CommitCharacteristics> commits = GitAccess.getInstance().getCommitsCharacteristics(
        HistoryStrategy.CURRENT_LOCAL_BRANCH, null, null);
    assertEquals(2, commits.size());
    for(CommitCharacteristics commit : commits) {
      assertTrue(commit.getPlotCommit().getParentCount() < 2); // test if every commit has maximum one parent.
    }
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    assertEquals("squash content", TestUtil.read(file2.toURI().toURL()));
    
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.SQUASH_MERGE_ACTION_NAME + "...");
    flushAWT();
    
    final boolean[] dialogPresentedFlags = new boolean[1];
    dialogPresentedFlags[0] = false;
    DialogPresenter.getInstance().setPresenter(new IDialogPresenter() {
      @Override
      public void showWarningMessage(
          final String title, 
          final List<String> files, 
          final String message) {
        dialogPresentedFlags[0] = true;
      }
    });
    squashMergeBranchesDialog = (SquashMergeDialog)findDialog(
        translator.getTranslation(Tags.SQUASH_MERGE));
    assertNotNull(squashMergeBranchesDialog);
    mergeOkButton = findFirstButton(squashMergeBranchesDialog, Tags.SQUASH_AND_COMMIT);
    mergeOkButton.doClick();
    
    flushAWT();
    waitForScheduler();
    
    assertTrue(dialogPresentedFlags[0]);
    
    commits = GitAccess.getInstance().getCommitsCharacteristics(
        HistoryStrategy.CURRENT_LOCAL_BRANCH, null, null);
    assertEquals(2, commits.size());
    for(CommitCharacteristics commit : commits) {
      assertTrue(commit.getPlotCommit().getParentCount() < 2); // test if every commit has maximum one parent.
    }
    assertEquals("local content for merging", TestUtil.read(file.toURI().toURL()));
    assertEquals("squash content", TestUtil.read(file2.toURI().toURL()));

  }
  
  
  /**
   * <p><b>Description:</b>Tests squash and merge action case when we don't have new commits.</p>
   * <p><b>Bug ID:</b> EXM-50356</p>
   * 
   * @author alex_smarandache
   * 
   * @throws Exception
   */
  public void testBranchSquashMergingWithoutCommits() throws Exception{
    final File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    
    final String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    //Make the first commit for the local repository on the main branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);
    
    //------------- Checkout the other branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitControllerBase mock = new GitController();
    final BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    refreshSupport.call();
    final GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    final GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    final String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    refreshSupport.call();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.CHECKOUT);
    refreshSupport.call();
    flushAWT();
    
    assertEquals(LOCAL_BRANCH_NAME1, gitAccess.getRepository().getBranch());
    
    // Move back to the main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getBranchInfo().getBranchName());
    refreshSupport.call();
    // Merge LocalBranch into main
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(firstLeaf), Tags.SQUASH_MERGE_ACTION_NAME + "...");
    flushAWT();
    
    final JDialog noCommitsInSquashDialog = findDialog(
        translator.getTranslation(Tags.SQUASH_NO_COMMITS_DETECTED_TITLE));
    assertNotNull(noCommitsInSquashDialog);
    JButton mergeOkButton = findFirstButton(noCommitsInSquashDialog, Tags.CLOSE);
    mergeOkButton.doClick();
    
    sleep(200);
    
    List<CommitCharacteristics> commits = GitAccess.getInstance().getCommitsCharacteristics(
        HistoryStrategy.CURRENT_LOCAL_BRANCH, null, null);
    assertEquals(1, commits.size());
    assertEquals("local content", TestUtil.read(file.toURI().toURL()));

  }
  
  
  /**
   * <p><b>Description:</b>Tests the branch merging. Conflict happens.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
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
    refreshSupport.call();
    gitAccess.commit("First local commit on main.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);

    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    refreshSupport.call();
    
    // ------------- Checkout branch: LOCAL_BRANCH_NAME1  -------------
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    
    // Commit on this branch
    setFileContent(file1, "local file 1 on new branch");
    setFileContent(file2, "local file 2 on new branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    refreshSupport.call();
    gitAccess.commit("Commit on secondary branch");
    refreshSupport.call();

    // ------------- Move to the main branch and commit something there ---------------
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    setFileContent(file1, "local file 1 modifications");
    setFileContent(file2, "local file 2 modifications");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    refreshSupport.call();
    gitAccess.commit("2nd commit on main branch");
    refreshSupport.call();

    // Merge secondary branch into main
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
    String secondaryBranchPath = (String) secondaryBranchNode.getUserObject();
    assertTrue(secondaryBranchPath.contains(Constants.R_HEADS));
    refreshSupport.call();
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
    flushAWT();
    
    //Confirm merge dialog
    JDialog mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
    JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
    mergeOkButton.doClick();
    
    flushAWT();
    sleep(200);
    waitForScheduler();

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
      GitControllerBase mock = new GitController();
      BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
      branchManagementPanel.refreshBranches();
      flushAWT();
      
      BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
      GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
      GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
      
      executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
      
      flushAWT();
      sleep(300);
      
      //Confirm merge dialog
      JDialog mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
      JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
      mergeOkButton.doClick();
      
      sleep(200);

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

      executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
      
      flushAWT();
      sleep(300);
      
      //Confirm merge dialog
      mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
      mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
      mergeOkButton.doClick();
      
      sleep(200);

      mergeFailDialog = findDialog(translator.getTranslation(Tags.MERGE_FAILED_UNCOMMITTED_CHANGES_TITLE));
      assertNotNull(mergeFailDialog);

    } finally {
      if (mergeFailDialog != null) {
        mergeFailDialog.setVisible(false);
        mergeFailDialog.dispose();
      }
    }
  }
  
  /**
   *<p><b>Description:</b>Tests the merging after you have an ignored conflict.</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  public void testBranchMergingWithoutResolvingConflict() throws Exception { 
    
    JDialog conflictMergeDialog = null;
    JDialog mergeOkDialog = null;

    File file = new File(LOCAL_TEST_REPOSITORY, "local.txt");
    file.createNewFile();

    setFileContent(file, "local file 1 content");

    // Make the first commit for the local repository and create a new branch
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("1st commit on main.");
    
    gitAccess.createBranch(LOCAL_BRANCH_NAME1);

    GitControllerBase mock = new GitController(GitAccess.getInstance());
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    
    // ------------- Checkout branch: LOCAL_BRANCH_NAME1  -------------
    gitAccess.setBranch(LOCAL_BRANCH_NAME1);
    
    // Commit on this branch
    setFileContent(file, "local file ... new branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("Commit on secondary branch");

    // ------------- Move to the main branch and commit something there ---------------
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    setFileContent(file, "file modifications");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("2nd commit on main");

    // Merge secondary branch into main
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode) (branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode secondaryBranchNode = (GitTreeNode) root.getFirstLeaf();
    String secondaryBranchPath = (String) secondaryBranchNode.getUserObject();
    assertTrue(secondaryBranchPath.contains(Constants.R_HEADS));
    
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
    flushAWT();
    
    //Confirm merge dialog
    mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
    JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
    mergeOkButton.doClick();
    
    sleep(200);
    
    conflictMergeDialog = findDialog(translator.getTranslation(Tags.MERGE_CONFLICTS_TITLE));
    assertNotNull(conflictMergeDialog);
    conflictMergeDialog.dispose();
    
    sleep(200);
    
    //Don't resolve merge conflicts and try to do the merge again and we will get an errMsg
    
    //Mock showErrorMessage
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        errMsg[0] = message;
        return null;
      }
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString());
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    executeActionByName(branchTreeMenuActionsProvider.getActionsForNode(secondaryBranchNode), Tags.MERGE_BRANCH1_INTO_BRANCH2 +  "...");
    flushAWT();
    
    sleep(200);
    assertEquals(Tags.RESOLVE_CONFLICTS_FIRST, errMsg[0]);
  }
  
  
  /**
   * Perform the first action founded with the given name.
   * 
   * @param actions            List of all actions.
   * @param searchedActionName The name of action to be executed.
   */
  private void executeActionByName(final List<AbstractAction> actions, final String searchedActionName) {
    actions.stream()
    .filter(a -> Objects.nonNull(a))
    .filter(a -> searchedActionName.equals(a.getValue(AbstractAction.NAME).toString())).findFirst()
    .ifPresent(a -> SwingUtilities.invokeLater(() -> a.actionPerformed(null)));
  }
}
