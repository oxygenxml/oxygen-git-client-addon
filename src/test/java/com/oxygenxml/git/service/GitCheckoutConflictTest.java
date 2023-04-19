package com.oxygenxml.git.service;

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchTreeMenuActionsProvider;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.refresh.PanelsRefreshSupport;
import com.oxygenxml.git.view.staging.BranchSelectionCombo;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

/**
 * Test cases for checkout conflicts.
 */
public class GitCheckoutConflictTest extends JFCTestCase {
  
  /**
   * i18n
   */
  protected static final Translator translator = Translator.getInstance();
  
  PanelsRefreshSupport refreshSupport = new PanelsRefreshSupport(null) {
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
  
  /**
   * Push changes.
   * 
   * @param username User name.
   * @param password Password.
   * 
   * @return push response.
   * 
   * @throws GitAPIException 
   */
  protected final PushResponse push(String username, String password) throws GitAPIException {
    return GitAccess.getInstance().push(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()));
  }
  
  /**
   * Pull.
   * 
   * @param username          Username.
   * @param password          Password.
   * @param pullType          Pull type.
   * @param updateSubmodules  <code>true</code> to update submodules.
   * 
   * @return Pull response.
   * 
   * @throws GitAPIException
   */
  protected PullResponse pull(String username, String password, PullType pullType, boolean updateSubmodules) throws GitAPIException {
    return GitAccess.getInstance().pull(
        new SSHCapableUserCredentialsProvider("", "", "", GitAccess.getInstance().getHostName()),
        pullType,
        updateSubmodules);
  }

  @Override
  protected void setUp() throws Exception {
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

    WSOptionsStorage mockedWsOptionsStorage = new WSOptionsStorageTestAdapter();
    Mockito.doAnswer(invocation -> mockedWsOptionsStorage).when(pluginWSMock).getOptionsStorage();
    
    XMLUtilAccess xmlUtilAccess = Mockito.mock(XMLUtilAccess.class);
    Mockito.when(xmlUtilAccess.escapeTextValue(Mockito.anyString())).thenAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      return arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
    });
    
    ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);
    Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
    Mockito.doAnswer(invocation -> null).when(projectCtrlMock).refreshFolders(Mockito.any());
  
    Mockito.doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      shownWarningMess[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
      return null;
    }).when(pluginWSMock).showWarningMessage(Mockito.anyString());
    shownWarningMess[0] = "";
    
    Mockito.doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      errMsg[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
      return null;
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString());
    errMsg[0] = "";
    
    ImageUtilities imageUtilities = Mockito.mock(ImageUtilities.class);
    // Dummy icon
    Mockito.doReturn(Icons.getIcon(Icons.AMEND_COMMIT)).when(imageUtilities).loadIcon((URL)Mockito.any());
    Mockito.doReturn(imageUtilities).when(pluginWSMock).getImageUtilities();
    
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
  }
  
  @Override
  protected void tearDown() throws Exception {
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
   * <p><b>Description:</b> try to switch branch from Git Branch Manager when repo is in conflict state.
   * The branch switch also generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSwitchBranchWhenRepoInConflict_checkoutConflict_1() throws Exception {
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
    sleep(500);
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(Tags.CHECKOUT)) {
        SwingUtilities.invokeLater(() -> abstractAction.actionPerformed(null));
        break;
      }
    }
    
    sleep(500);
    
    
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
    
    assertEquals("Branch_switch_when_repo_in_conflict_error_msg", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch branch from Git Staging when repo is in conflict state.
   * The branch witch also generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSwitchBranchWhenRepoInConflict_checkoutConflict_2() throws Exception {
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
    
    // move to main
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    
    // Pull to create conflict
    PullResponse pullResp = pull("", "", PullType.MERGE_FF, false);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullResp.toString());
    
    // Simulate branch checkout from Git Staging
    GitController gitController = new GitController(gitAccess);
    
    BranchSelectionCombo branchesCombo = new BranchSelectionCombo(gitController);
    branchesCombo.refresh();
    flushAWT();
    SwingUtilities.invokeLater(() -> branchesCombo.setSelectedIndex(1));
    flushAWT();
    
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
    assertEquals("Branch_switch_when_repo_in_conflict_error_msg", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch branch from Git Staging when repo is in conflict state.
   * This should succeed.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSwitchBranchWhenRepoInConflict_succeed_1() throws Exception {
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
    gitAccess.setBranch("new_branch");;
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
    
    // Simulate branch checkout from Git Staging
    GitController gitController = new GitController(gitAccess);
    BranchSelectionCombo branchesCombo = new BranchSelectionCombo(gitController);
    branchesCombo.refresh();
    flushAWT();
    SwingUtilities.invokeLater(() -> branchesCombo.setSelectedIndex(1));
    flushAWT();
    
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
    assertEquals("Branch_switch_when_repo_in_conflict_error_msg", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch branch from Git Branch Manager.
   * The branch witch generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSwitchBranchFromGitBranchManager_checkoutConflict_1() throws Exception {
    // Push from first repo
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  
    // Change file on the new branch
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    gitAccess.setBranch("new_branch");
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "altfel");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("commit 2");
    
    // move to main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "new content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    sleep(1000);
    BranchTreeMenuActionsProvider branchTreeMenuActionsProvider = new BranchTreeMenuActionsProvider(mock);
   
    // Simulate branch checkout from Git Branch Manager view
    GitTreeNode node = new GitTreeNode(
        new TreePath(
            new String[] {"refs", "heads", "new_branch"}));
    node.setUserObject("refs/heads/new_branch");
    List<AbstractAction> actionsForNode = branchTreeMenuActionsProvider.getActionsForNode(node);
    for (AbstractAction abstractAction : actionsForNode) {
      if(abstractAction.getValue(AbstractAction.NAME).equals(Tags.CHECKOUT)) {
        abstractAction.actionPerformed(null);
        break;
      }
    }
    sleep(1000);
    
    Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    
    JButton yesButton = TestUtil.findButton(focusedWindow, translator.getTranslation(Tags.MOVE_CHANGES));
    yesButton.doClick();
    sleep(1000);
    
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
    
    assertEquals("Branch_switch_checkout_conflict_error_msg", errMsg[0]);
  }
  
  /**
   * <p><b>Description:</b> try to switch branch from Git Staging.
   * The branch witch generates a checkout conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSwitchBranchFromGitBranchManager_checkoutConflict_2() throws Exception {
    // Push from first repo
    gitAccess.setRepositorySynchronously(FIRST_LOCAL_TEST_REPOSITPRY);

    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "hellllo");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("file test added");
    push("", "");
  
    // Change file on the new branch
    gitAccess.createBranchFromLocalBranch(
        "new_branch",
        gitAccess.getGit().getRepository().getFullBranch());
    gitAccess.setBranch("new_branch");
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "altfel");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("commit 2");
    
    // move to main branch
    gitAccess.setBranch(GitAccess.DEFAULT_BRANCH_NAME);
    writeToFile(new File(FIRST_LOCAL_TEST_REPOSITPRY + "/test.txt"), "new content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    GitControllerBase gitCtrl = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(gitCtrl);
    branchManagementPanel.refreshBranches();
    flushAWT();
   
    BranchSelectionCombo branchesCombo = new BranchSelectionCombo((GitController) gitCtrl);
    branchesCombo.refresh();
    flushAWT();
    SwingUtilities.invokeLater(() -> branchesCombo.setSelectedIndex(1));
    flushAWT();
    
    Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    
    JButton yesButton = TestUtil.findButton(focusedWindow, translator.getTranslation(Tags.MOVE_CHANGES));
    yesButton.doClick();
    sleep(1000);
    assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
    
    assertEquals("Branch_switch_checkout_conflict_error_msg", errMsg[0]);
    
  }
  

	/**
   * Sleep well!
   * 
   * @param delay Delay.
   * 
   * @throws InterruptedException
   */
  private void sleep(int delay) throws InterruptedException {
    Thread.sleep(delay); // NOSONAR
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
