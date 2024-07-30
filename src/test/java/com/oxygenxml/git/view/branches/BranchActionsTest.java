package com.oxygenxml.git.view.branches;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.GitController;
/**
 * Test cases for the actions that can be done on a branch.
 * 
 * @author Bogdan Draghici
 */
public class BranchActionsTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessCheckoutNewBranch/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private final static String REMOTE_BRANCH_NAME1 = "RemoteBranch";
  private final static String LOCAL_BRANCH_NAME2 = "LocalBranch2";
  private final static String REMOTE_BRANCH_NAME2 = "RemoteBranch2";
  private final static String LOCAL_BRANCH_COPY_NAME = "LocalBranchCopy";
  private final static String REMOTE_BRANCH_NAME1_COPY = "RemoteBranchCopy";
  private final static String REMOTE_BRANCH_NAME2_COPY = "RemoteBranch2Copy";
  private GitAccess gitAccess = GitAccess.getInstance();
  private Repository remoteRepository;
  private Repository localRepository;
  
  @Override
  public void setUp() throws Exception {
    
    super.setUp();
    
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
  public void testCheckoutLocalBranchAction() throws Exception {
    final Supplier<String> branchName = new Supplier<String>() { 
      @Override
      public String get() {
        try {
          return gitAccess.getRepository().getBranch();
        } catch (IOException | NoRepositorySelected e) {
          return null;
        }
      }
    };
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
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    GitController mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
   
    //------------- Checkout the first branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] branchPath = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT))) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
   
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
      LOCAL_BRANCH_NAME1.equals(branchName.get())
    );
    
    //------------- Checkout the next branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode nextLeaf = (GitTreeNode)firstLeaf.getNextLeaf();
    String nextLeafPath = (String) nextLeaf.getUserObject();
    assertTrue(nextLeafPath.contains(Constants.R_HEADS));
    
    branchPath = nextLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME2, branchPath[branchPath.length - 1]);
    
    AbstractAction checkoutAction = branchTreeMenuActionsProvider.getCheckoutAction(nextLeaf);
    if (checkoutAction != null) {
      checkoutAction.actionPerformed(null);
    }
     
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
      LOCAL_BRANCH_NAME2.equals(branchName.get())
    );
  }
  
  /**
   * Tests the action of creating a new branch from a local branch.
   * 
   * @throws Exception
   */
  public void testCreateLocalBranchAction() throws Exception{
    boolean initialIsCheckoutNewBranch = OptionsManager.getInstance().isCheckoutNewlyCreatedLocalBranch();
    try {
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
      assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);

      GitController mock = new GitController();
      BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
      branchManagementPanel.refreshBranches();
      flushAWT();
      BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
      GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());

      //------------- Create branch LOCAL_BRANCH_COPY_NAME from first branch in the tree: LOCAL_BRANCH_NAME1 -------------
      GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
      String firstLeafPath = (String)firstLeaf.getUserObject();
      assertTrue(firstLeafPath.contains(Constants.R_HEADS));
      String[] split = firstLeafPath.split("/");
      assertEquals(LOCAL_BRANCH_NAME1, split[split.length - 1]);

      List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
      for (AbstractAction abstractAction : actionsForNode) {
        if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
          SwingUtilities.invokeLater(() -> {
            abstractAction.actionPerformed(null);
          });
          flushAWT();

          JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
          JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
          assertNotNull(checkoutBranchCheckBox);
          checkoutBranchCheckBox.setSelected(true);
          flushAWT();

          JTextField branchNameTextField = findComponentNearJLabel(createBranchDialog,
              translator.getTranslation(Tags.BRANCH_NAME) + ": ", JTextField.class);
          branchNameTextField.setText(LOCAL_BRANCH_COPY_NAME);
          JButton okButton = findFirstButton(createBranchDialog, "Create");
          if (okButton != null) {
            okButton.setEnabled(true);
            okButton.doClick();
          }
          break;
        }
      }
      
      sleep(200);
      branchManagementPanel.refreshBranches();
      flushAWT();
      root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
      StringBuilder actualTree = new StringBuilder();
      BranchManagementTest.serializeTree(actualTree, root);
      assertEquals(
          "localRepository\n" + 
              "  refs/heads/\n" + 
              "    refs/heads/LocalBranch\n" +
              "    refs/heads/LocalBranch2\n" +
              "    refs/heads/LocalBranchCopy\n" +
              "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
              actualTree.toString());

      assertEquals("LocalBranchCopy", gitAccess.getBranchInfo().getBranchName());
    } finally {
      OptionsManager.getInstance().setCheckoutNewlyCreatedLocalBranch(initialIsCheckoutNewBranch);
    }
  }
  
  /**
   * <p><b>Description:</b> create local branch but don't check it out.</p>
   * <p><b>Bug ID:</b> EXM-47204</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCreateLocalBranchAction_dontCheckout() throws Exception{
    boolean initialIsCheckoutNewBranch = OptionsManager.getInstance().isCheckoutNewlyCreatedLocalBranch();
    try {
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
      assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);

      GitController mock = new GitController();
      BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
      branchManagementPanel.refreshBranches();
      flushAWT();
      BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
      GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());

      //------------- Create branch LOCAL_BRANCH_COPY_NAME from first branch in the tree: LOCAL_BRANCH_NAME1 -------------
      GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
      String firstLeafPath = (String)firstLeaf.getUserObject();
      assertTrue(firstLeafPath.contains(Constants.R_HEADS));
      String[] split = firstLeafPath.split("/");
      assertEquals(LOCAL_BRANCH_NAME1, split[split.length - 1]);

      List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
      for (AbstractAction abstractAction : actionsForNode) {
        if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
          SwingUtilities.invokeLater(() -> {
            abstractAction.actionPerformed(null);
          });
          flushAWT();

          JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
          JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
          assertNotNull(checkoutBranchCheckBox);
          checkoutBranchCheckBox.setSelected(false);
          flushAWT();

          JTextField branchNameTextField = findComponentNearJLabel(createBranchDialog,
              translator.getTranslation(Tags.BRANCH_NAME) + ": ", JTextField.class);
          branchNameTextField.setText(LOCAL_BRANCH_COPY_NAME);
          JButton okButton = findFirstButton(createBranchDialog, "Create");
          if (okButton != null) {
            okButton.setEnabled(true);
            okButton.doClick();
          }
          break;
        }
      }
      sleep(200);
      branchManagementPanel.refreshBranches();
      flushAWT();
      root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
      StringBuilder actualTree = new StringBuilder();
      BranchManagementTest.serializeTree(actualTree, root);
      assertEquals(
          "localRepository\n" + 
              "  refs/heads/\n" + 
              "    refs/heads/LocalBranch\n" +
              "    refs/heads/LocalBranch2\n" +
              "    refs/heads/LocalBranchCopy\n" +
              "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
              actualTree.toString());

      assertEquals(initialBranchName, gitAccess.getBranchInfo().getBranchName());
    } finally {
      OptionsManager.getInstance().setCheckoutNewlyCreatedLocalBranch(initialIsCheckoutNewBranch);
    }
  }
  
  /**
   * Tests the action of deleting a local branch.
   * 
   * @throws Exception
   */
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
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    GitController mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    
    //------------- Delete first branch in the tree: LOCAL_BRANCH_NAME1 -------------
    GitTreeNode firstLeaf = (GitTreeNode)root.getFirstLeaf();
    String firstLeafPath = (String)firstLeaf.getUserObject();
    assertTrue(firstLeafPath.contains(Constants.R_HEADS));
    
    String[] split = firstLeafPath.split("/");
    assertEquals(LOCAL_BRANCH_NAME1, split[split.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(firstLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction == null) {
        // Probably separator. Continue.
        continue;
      }
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.DELETE) + "...")) {
        SwingUtilities.invokeLater(() -> {
          abstractAction.actionPerformed(null);
        });
        JDialog deleteBranchDialog = findDialog(translator.getTranslation(Tags.DELETE_BRANCH));
        JButton yesButton = findFirstButton(deleteBranchDialog, translator.getTranslation(Tags.YES));
        yesButton.doClick();
        
        break;
      }
    }
    sleep(200);
    branchManagementPanel.refreshBranches();
    flushAWT();
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    BranchManagementTest.serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/LocalBranch2\n" +
        "    refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + "\n",
        actualTree.toString());
  }
  
  /**
   * Tests the action of checkout a remote branch.
   * 
   * @throws Exception
   */
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
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    GitController mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    
    //------------- Checkout the last branch in the tree: REMOTE_BRANCH_NAME2, with the name REMOTE_BRANCH_NAME2_COPY-------------
    GitTreeNode lastLeaf = (GitTreeNode)root.getLastLeaf();
    String lastLeafPath = (String)lastLeaf.getUserObject();
    assertTrue(lastLeafPath.contains(Constants.R_REMOTES));
    
    String[] branchPath = lastLeafPath.split("/");
    assertEquals(REMOTE_BRANCH_NAME2, branchPath[branchPath.length - 1]);
    
    AbstractAction checkoutAction = branchTreeMenuActionsProvider.getCheckoutAction(lastLeaf);
    assertNotNull(checkoutAction);
    SwingUtilities.invokeLater(() -> checkoutAction.actionPerformed(null));
    JDialog checkoutBranchDialog = findDialog(translator.getTranslation(Tags.CHECKOUT_BRANCH));
    assertNotNull(checkoutBranchDialog);

    final JTextField branchNameTextField = findComponentNearJLabel(
        checkoutBranchDialog,
        translator.getTranslation(Tags.BRANCH_NAME) + ": ",
        JTextField.class);
    assertNotNull(branchNameTextField);
    SwingUtilities.invokeLater(() -> branchNameTextField.setText(REMOTE_BRANCH_NAME2_COPY));
    flushAWT();

    final JButton checkoutBtn = findFirstButton(checkoutBranchDialog, Tags.CHECKOUT);
    assertNotNull(checkoutBtn);
    SwingUtilities.invokeLater(() -> {
      checkoutBtn.setEnabled(true);
      checkoutBtn.doClick();
    });
    flushAWT();
    sleep(200);
    
    branchManagementPanel.refreshBranches();
    flushAWT();
    sleep(400);
    
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    StringBuilder actualTree = new StringBuilder();
    BranchManagementTest.serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/RemoteBranch2Copy\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
    
    //------------- Checkout the previous branch in the tree: REMOTE_BRANCH_NAME1, with the name REMOTE_BRANCH_NAME1_COPY-------------
    GitTreeNode previousLeaf = (GitTreeNode)lastLeaf.getPreviousLeaf();
    String previousLeafPath = (String)previousLeaf.getUserObject();
    assertTrue(lastLeafPath.contains(Constants.R_REMOTES));
    
    branchPath = previousLeafPath.split("/");
    assertEquals(REMOTE_BRANCH_NAME1, branchPath[branchPath.length - 1]);
    
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(previousLeaf);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CHECKOUT) + "...")) {
        SwingUtilities.invokeLater(() -> abstractAction.actionPerformed(null));
        JDialog checkoutDialog = findDialog(translator.getTranslation(Tags.CHECKOUT_BRANCH));
        assertNotNull(checkoutDialog);
        JTextField branchNameTextField2 = findComponentNearJLabel(
            checkoutDialog,
            translator.getTranslation(Tags.BRANCH_NAME) + ": ",
            JTextField.class);
        assertNotNull(branchNameTextField2);
        SwingUtilities.invokeLater(() -> branchNameTextField2.setText(REMOTE_BRANCH_NAME1_COPY));
        flushAWT();
        JButton checkoutBtn2 = findFirstButton(checkoutDialog, Tags.CHECKOUT);
        assertNotNull(checkoutBtn2);
        SwingUtilities.invokeLater(() -> {
          checkoutBtn2.setEnabled(true);
          checkoutBtn2.doClick();
        });
        break;
      }
    }
    flushAWT();
    sleep(200);
    
    branchManagementPanel.refreshBranches();
    flushAWT();
    sleep(200);
    
    root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    actualTree = new StringBuilder();
    BranchManagementTest.serializeTree(actualTree, root);
    assertEquals(
        "localRepository\n" + 
        "  refs/heads/\n" + 
        "    refs/heads/RemoteBranch2Copy\n" + 
        "    refs/heads/RemoteBranchCopy\n" + 
        "  refs/remotes/\n" + 
        "    refs/remotes/origin/\n" + 
        "      refs/remotes/origin/" + GitAccess.DEFAULT_BRANCH_NAME + "\n" + 
        "      refs/remotes/origin/RemoteBranch\n" + 
        "      refs/remotes/origin/RemoteBranch2\n" + 
        "",
        actualTree.toString());
  }
  
  /**
   *<p><b>Description:</b>Tests the action created on the current Branch</p>
   * <p><b>Bug ID:</b> EXM-43410</p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  public void testContextualMenuActionsOnCurrentBranch() throws Exception{
    
    File file = new File(LOCAL_TEST_REPOSITORY + "local.txt");
    file.createNewFile();
    setFileContent(file, "local content");
    
    //Make the first commit for the local repository
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local.txt"));
    gitAccess.commit("First local commit.");
    
    //Verify we are on main branch
    String initialBranchName = gitAccess.getBranchInfo().getBranchName();
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, initialBranchName);
    
    GitController mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
    GitTreeNode root = (GitTreeNode)(branchManagementPanel.getTree().getModel().getRoot());
    GitTreeNode mainBranchNode = (GitTreeNode)root.getFirstLeaf();
    String mainBranchPath = (String)mainBranchNode.getUserObject();

    assertEquals("refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME, mainBranchPath);
    
    //------------- Verify the actions on the selected branch -------------

    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(mainBranchNode);
    int noOfActions = actionsForNode.size();
    assertEquals(1, noOfActions);
    
    String actionName = (String) actionsForNode.get(0).getValue(AbstractAction.NAME);
    assertEquals(translator.getTranslation(Tags.CREATE_BRANCH) + "...", actionName);
  }
  
}
