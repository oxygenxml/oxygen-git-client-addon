package com.oxygenxml.git.service;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.awaitility.Awaitility;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Supplier;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchTreeMenuActionsProvider;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.refresh.PanelRefresh;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

/**
 * Test cases for checkout conflicts. With visual interaction too.
 */
public class GitCheckoutConflict2Test extends GitTestBase {
  PanelRefresh refreshSupport = new PanelRefresh(null) {
    @Override
    protected int getScheduleDelay() {
      // Execute refresh events immediately from tests.
      return 1;
    }
  };
  
  protected final static String FIRST_LOCAL_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/local";
  protected final static String SECOND_LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessConflictTest/local2";
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/GitAccessConflictTest/remote";
  private Repository localRepo1;
  private Repository localRepo2;
  private Repository remoteRepo;
  protected GitAccess gitAccess;
  private String[] shownWarningMess = new String[1];
  private String[] errMsg = new String[1];
  
  @Override
  public void setUp() throws Exception {
    super.setUp();

    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(FIRST_LOCAL_TEST_REPOSITPRY);
    localRepo1 = gitAccess.getRepository();
    gitAccess.createNewRepository(SECOND_LOCAL_TEST_REPOSITORY);
    localRepo2 = gitAccess.getRepository();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();

    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    File file = new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt");
    file.createNewFile(); 
    StoredConfig config = gitAccess.getRepository().getConfig();
    RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
    URIish uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec1 = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec1);
    remoteConfig.update(config);
    config.save();
    
    String branchName = "main";
    String remoteName = "origin";
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();

    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    config = gitAccess.getRepository().getConfig();
    remoteConfig = new RemoteConfig(config, "origin");
    uri = new URIish(remoteRepo.getDirectory().toURI().toURL());
    remoteConfig.addURI(uri);
    RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    remoteConfig.addFetchRefSpec(spec);
    remoteConfig.update(config);
    config.save();
    
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
    config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
    config.save();

    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage mockedWsOptionsStorage = Mockito.mock(WSOptionsStorage.class);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(mockedWsOptionsStorage).setOption(Mockito.anyString(), Mockito.any());
    Mockito.doAnswer(new Answer<WSOptionsStorage>() {
      @Override
      public WSOptionsStorage answer(InvocationOnMock invocation) throws Throwable {
        return mockedWsOptionsStorage;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getOptionsStorage();
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        return arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
      }
    });
    Mockito.doAnswer(new Answer<XMLUtilAccess>() {
      @Override
      public XMLUtilAccess answer(InvocationOnMock invocation) throws Throwable {
        return xmlUtilAccess;
      }
    }).when((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getXMLUtilAccess();

    ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);
    Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        return null;
      }
    }).when(projectCtrlMock).refreshFolders(Mockito.any());
  
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        shownWarningMess[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
        return null;
      }
    }).when(pluginWSMock).showWarningMessage(Mockito.anyString());
    shownWarningMess[0] = "";
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        errMsg[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
        return null;
      }
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString());
    errMsg[0] = "";
    
    ImageUtilities imageUtilities = Mockito.mock(ImageUtilities.class);
    // Dummy icon
    Mockito.doReturn(null).when(imageUtilities).loadIcon((URL)Mockito.any());
    Mockito.when(pluginWSMock.getImageUtilities()).thenReturn(imageUtilities);
  }
  
  @Override
  public void tearDown() throws Exception {
    // JGit relies on GC to release some file handles. See org.eclipse.jgit.internal.storage.file.WindowCache.Ref
    // When an object is collected by the GC, it releases a file lock.
    System.gc();
    
    gitAccess.closeRepo();
    localRepo1.close();
    localRepo2.close();
    remoteRepo.close();
    File dirToDelete = new File(FIRST_LOCAL_TEST_REPOSITPRY);
    FileUtil.deleteRecursivelly(dirToDelete);
    dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
    FileUtil.deleteRecursivelly(dirToDelete);
    dirToDelete = new File(SECOND_LOCAL_TEST_REPOSITORY);
    FileUtil.deleteRecursivelly(dirToDelete);
    
    super.tearDown();
  }
  
  /**
   * <p><b>Description:</b> try to switch to a newly created branch from Git Branch Manager 
   * when repo is in conflict state.
   * The branch checkout also generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCheckoutNewBranchWhenRepoInConflict_checkoutConflict_1() throws Exception {
    // Push from first repo
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  
    // Commit from second repo
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    
    File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
    file.createNewFile();
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "teeeeeest");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("conflict");
    
    // Change file on the new branch
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    gitAccess.setBranch("new_branch");
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "altfel");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("commit on ew branch");
    
    // move to main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    // Pull to create conflict
    PullResponse pullResp = pull("", "", PullType.MERGE_FF, false);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullResp.toString());
    
    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
        SwingUtilities.invokeLater(() -> {
          abstractAction.actionPerformed(null);
        });
       
        JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
        JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
        assertNotNull(checkoutBranchCheckBox);
        checkoutBranchCheckBox.setSelected(true);
        flushAWT();

        JTextField branchNameTextField = findComponentNearJLabel(createBranchDialog,
            translator.getTranslation(Tags.BRANCH_NAME) + ": ", JTextField.class);
        branchNameTextField.setText("a_new_day");
        JButton okButton = findFirstButton(createBranchDialog, "Create");
        if (okButton != null) {
          okButton.setEnabled(true);
          okButton.doClick();
        }
        break;
      }
    }
    
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
    
    Awaitility.waitAtMost(500, TimeUnit.MILLISECONDS).until(() -> 
      GitAccess.DEFAULT_BRANCH_NAME.equals(branchName.get()));
    
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, branchName.get());
    
    assertEquals("Cannot_checkout_new_branch_when_having_conflicts", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch to a newly created branch from Git Branch Manager 
   * when repo is in conflict state.
   * The branch checkout also generates a checkout conflict, but not on the resource that is in pull conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCheckoutNewBranchWhenRepoInConflict_checkoutConflict_2() throws Exception {
    // Push test.txt from first repo
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  
    // Commit test.txt from second repo
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    
    File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
    file.createNewFile();
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "teeeeeest");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("conflict");
    
    // Change file.txt file on the new branch
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    gitAccess.setBranch("new_branch");
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/file.txt"), "altfel");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "file.txt"));
    gitAccess.commit("commit on nnew branch");
    
    // move to main
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    // change file.txt to create checkout conflict
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/file.txt"), "new changes");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "file.txt"));
    
    // Pull to create conflict o text.txt
    PullResponse pullResp = pull("", "", PullType.MERGE_FF, false);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullResp.toString());
    
    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
        SwingUtilities.invokeLater(() -> {
          abstractAction.actionPerformed(null);
        });

        JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
        JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
        assertNotNull(checkoutBranchCheckBox);
        checkoutBranchCheckBox.setSelected(true);
        flushAWT();

        JTextField branchNameTextField = findComponentNearJLabel(createBranchDialog,
            translator.getTranslation(Tags.BRANCH_NAME) + ": ", JTextField.class);
        branchNameTextField.setText("a_new_day");
        JButton okButton = findFirstButton(createBranchDialog, "Create");
        if (okButton != null) {
          okButton.setEnabled(true);
          okButton.doClick();
        }
        break;
      }
    }
    
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
    
    Awaitility.waitAtMost(500, TimeUnit.MILLISECONDS).until(() -> 
      GitAccess.DEFAULT_BRANCH_NAME.equals(branchName.get()));

    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());

    assertEquals("Cannot_checkout_new_branch_when_having_conflicts", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch to a newly created branch from Git Branch Manager 
   * when repo is in conflict state.
   * This should succeed.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCheckoutNewBranchWhenRepoInConflict_succeed_1() throws Exception {
    // Push test.txt from first repo
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  
    // Commit test.txt from second repo
    gitAccess.setRepositorySynchronously(SECOND_LOCAL_TEST_REPOSITORY);
    
    File file = new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt");
    file.createNewFile();
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/test.txt"), "teeeeeest");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("conflict");
    
    // Change file.txt file on the new branch
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    gitAccess.setBranch("new_branch");
    writeToFile(new File(SECOND_LOCAL_TEST_REPOSITORY + "/file.txt"), "altfel");;
    gitAccess.add(new FileStatus(GitChangeType.ADD, "file.txt"));
    gitAccess.commit("commit on nnew branch");
    
    // move to main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    // Pull to create conflict o text.txt
    PullResponse pullResp = pull("", "", PullType.MERGE_FF, false);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullResp.toString());
    
    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
        SwingUtilities.invokeLater(() -> {
          abstractAction.actionPerformed(null);
        });
      
        JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
        JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
        assertNotNull(checkoutBranchCheckBox);
        checkoutBranchCheckBox.setSelected(true);
        flushAWT();

        JTextField branchNameTextField = findComponentNearJLabel(createBranchDialog,
            translator.getTranslation(Tags.BRANCH_NAME) + ": ", JTextField.class);
        branchNameTextField.setText("a_new_day");
        JButton okButton = findFirstButton(createBranchDialog, "Create");
        if (okButton != null) {
          okButton.setEnabled(true);
          okButton.doClick();
        }
        break;
      }
    }
    
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
    
    Awaitility.waitAtMost(500, TimeUnit.MILLISECONDS).until(() -> 
    "a_new_day".equals(branchName.get()));

    assertEquals("a_new_day", gitAccess.getRepository().getBranch());
  }
  
  /**
   * <p><b>Description:</b> try to switch to a newly created branch from Git Branch Manager.
   * Repo is not in conflict.
   * The branch checkout generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCheckoutNewBranch_checkoutConflict_1() throws Exception {
    // Push from main branch
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");

    // Create new_branch, change file and commit
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "altfel");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("commit on ew branch");

    // Move to main branch and change file
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "new content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));

    GitControllerBase gitCtrl = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(gitCtrl);
    branchManagementPanel.refreshBranches();
    flushAWT();
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(gitCtrl);

    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    AbstractAction createBranchAction[] = new AbstractAction[1];
    for (AbstractAction abstractAction : actionsForNode) {
      if (abstractAction.getValue(AbstractAction.NAME).equals(translator.getTranslation(Tags.CREATE_BRANCH) + "...")) {
        createBranchAction[0] = abstractAction;
        break;
      }
    }
    assertNotNull(createBranchAction[0]);

    SwingUtilities.invokeLater(() -> createBranchAction[0].actionPerformed(null));
    waitForScheduler();
  
    JDialog createBranchDialog = findDialog(translator.getTranslation(Tags.CREATE_BRANCH));
    assertNotNull(createBranchDialog);
    JCheckBox checkoutBranchCheckBox = findCheckBox(createBranchDialog, Tags.CHECKOUT_BRANCH);
    assertNotNull(checkoutBranchCheckBox);
    SwingUtilities.invokeLater(() -> checkoutBranchCheckBox.setSelected(true));
    flushAWT();

    JTextField branchNameTextField = findComponentNearJLabel(
        createBranchDialog,
        translator.getTranslation(Tags.BRANCH_NAME) + ": ",
        JTextField.class);
    assertNotNull(branchNameTextField);
    branchNameTextField.setText("a_new_day");

    JButton okButton = findFirstButton(createBranchDialog, "Create");
    assertNotNull(okButton);
    okButton.setEnabled(true);
    SwingUtilities.invokeLater(() -> okButton.doClick());
    flushAWT();

    Awaitility.waitAtMost(1000, TimeUnit.MILLISECONDS).until(() -> {
      final Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager()
          .getFocusedWindow();
      if(Objects.nonNull(focusedWindow)) {
        final JButton yesButton = TestUtil.findButton(focusedWindow, 
            translator.getTranslation(Tags.MOVE_CHANGES));
        if(Objects.nonNull(yesButton)) {
          yesButton.doClick();
          return true;
        }
      }
      return false;
    });

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

    Awaitility.waitAtMost(1000, TimeUnit.MILLISECONDS).untilAsserted(
        () -> assertEquals(GitAccess.DEFAULT_BRANCH_NAME, branchName.get()));

    assertEquals("Cannot_checkout_new_branch_because_uncommitted_changes", errMsg[0]);
  }
  

  /**
   * Writes content to file.
   * 
   * @param file Target file.
   * @param content Content to write in the file.
   * 
   * @throws FileNotFoundException File not found.
   */
  private void writeToFile(File file, String content) throws FileNotFoundException {
    PrintWriter out = new PrintWriter(file);
    try {
    out.print(content);
    } finally {
      out.close();
    }
  }

}
