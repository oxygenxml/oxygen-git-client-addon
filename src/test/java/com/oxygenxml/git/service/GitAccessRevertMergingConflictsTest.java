package com.oxygenxml.git.service;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchTreeMenuActionsProvider;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.HistoryStrategy;
import com.oxygenxml.git.view.history.RenameTracker;
import com.oxygenxml.git.view.history.actions.RevertCommitAction;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.ColorTheme;

public class GitAccessRevertMergingConflictsTest extends GitTestBase {
  private final static String LOCAL_FILE_NAME = "local.txt";
  private final static String LOCAL_FILE_NAME_2 = "local_2.txt";
  private File firstFile;
  private File secondFile;

  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private Repository remoteRepository;
  private Repository localRepository;
  private GitAccess gitAccess = GitAccess.getInstance();
  private String[] errMsg = new String[1];

  /**
   * Creates the local repository and commits a few files.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    // The plugin workspace is already mocked and initialized in super.
    StandalonePluginWorkspace pluginWSMock = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    Mockito.doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      errMsg[0] = arguments != null && arguments.length > 0 ? (String) arguments[0] : "";
      return null;
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString(), Mockito.any());
    errMsg[0] = "";
    
    ImageUtilities imageUtilities = Mockito.mock(ImageUtilities.class);
    Mockito.doReturn(null).when(imageUtilities).loadIcon((URL)Mockito.any());
    Mockito.doReturn(imageUtilities).when(pluginWSMock).getImageUtilities();
    ColorTheme colorTheme = Mockito.mock(ColorTheme.class);
    Mockito.when(colorTheme.isDarkTheme()).thenReturn(false);
    Mockito.when(pluginWSMock.getColorTheme()).thenReturn(colorTheme);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    // Creates the remote repository.
    createRepository(REMOTE_TEST_REPOSITORY);
    remoteRepository = gitAccess.getRepository();

    // Creates the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
    // Create the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    gitAccess = GitAccess.getInstance();

    // Create first file make the first commit for the local repository.
    firstFile = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME);
    firstFile.createNewFile();
    setFileContent(firstFile, "new local content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME));
    gitAccess.commit("First commit.");

    // Modify first file and make another commit.
    firstFile.createNewFile();
    setFileContent(firstFile, "modified content");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME));
    gitAccess.commit("Modified a file");

    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);

    // Create a second file and make a third commit
    secondFile = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME_2);
    secondFile.createNewFile();
    setFileContent(secondFile, "second local file");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME_2));
    gitAccess.commit("Added a new file");
  }

  /**
   * <p><b>Description:</b> test the "Revert commit" action when the repo has conflicts.</p>
   * <p><b>Bug ID:</b> EXM-47154</p>
   *
   * @author Tudosie Razvan
   * 
   * @throws Exception
   */
  @Test
  public void testRevertCommitWithRepoInConflict() throws Exception {
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

    GitControllerBase mock = new GitController();
    BranchManagementPanel branchManagementPanel = new BranchManagementPanel(mock);
    branchManagementPanel.refreshBranches();
    flushAWT();

    // ------------- Checkout branch: LOCAL_BRANCH_NAME1 -------------
     gitAccess.setBranch(LOCAL_BRANCH_NAME1);

    // Commit on this branch
    setFileContent(file1, "local file 1 on new branch");
    setFileContent(file2, "local file 2 on new branch");
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local1.txt"));
    gitAccess.add(new FileStatus(GitChangeType.ADD, "local2.txt"));
    gitAccess.commit("Commit on secondary branch");

    // ------------- Move to the main branch and commit something there
    // ---------------
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

    List<AbstractAction> actionsForSecondaryBranch = branchTreeMenuActionsProvider
        .getActionsForNode(secondaryBranchNode);
    for (AbstractAction action : actionsForSecondaryBranch) {
      if (action != null) {
        String actionName = action.getValue(AbstractAction.NAME).toString();
        if ((Tags.MERGE_BRANCH1_INTO_BRANCH2 + "...").equals(actionName)) {
          SwingUtilities.invokeLater(() -> action.actionPerformed(null));
          break;
        }
      }
    }
    flushAWT();

    // Confirm merge dialog
    JDialog mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
    JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.MERGE));
    mergeOkButton.doClick();
    flushAWT();
    
    JDialog conflictMergeDialog = findDialog(translator.getTranslation(Tags.MERGE_CONFLICTS_TITLE));
    assertNotNull(conflictMergeDialog);
    conflictMergeDialog.setVisible(false);
    conflictMergeDialog.dispose();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
      TestUtil.read(file1.toURI().toURL()).contains("<<<<<<< HEAD\n" + "local file 1 modifications\n"
        + "=======\n" + "local file 1 on new branch\n" + ">>>>>>>")
    );

    assertTrue(TestUtil.read(file2.toURI().toURL()).contains("<<<<<<< HEAD\n" + "local file 2 modifications\n"
        + "=======\n" + "local file 2 on new branch\n" + ">>>>>>>"));

    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker());
    String initialHistory = 
            "[ Uncommitted_changes , DATE , * , * , null , null ]\n" + 
            "[ 2nd commit on main branch , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
            "[ First local commit on main. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
            "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
            "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 4 , AlexJitianu , [5] ]\n" + 
            "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 5 , AlexJitianu , null ]\n" + 
            "";
    String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
    assertEquals(
        initialHistory,
        dumpHistory(commitsCharacteristics).replaceAll(regex, "DATE"));

    CommitCharacteristics commitToRevert = gitAccess.getCommitsCharacteristics(HistoryStrategy.CURRENT_BRANCH, null, new RenameTracker()).get(4);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();

    JDialog revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_CONFIRMATION, confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    revertOkButton.doClick();
    flushAWT();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> 
      "".equals(errMsg[0])
    );
  }

}

