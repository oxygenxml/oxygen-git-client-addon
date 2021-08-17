package com.oxygenxml.git.service;

import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.branches.BranchTreeMenuActionsProvider;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.actions.RevertCommitAction;

public class GitAccessRevertMergingConflicts extends GitTestBase {
  private final static String LOCAL_FILE_NAME = "local.txt";
  private final static String LOCAL_FILE_NAME_2 = "local_2.txt";
  private File firstFile;
  private File secondFile;

  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest/localRepository";
  private final static String REMOTE_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest/remoteRepository";
  private final static String LOCAL_BRANCH_NAME1 = "LocalBranch";
  private Repository remoteRepository;
  private Repository localRepository;

  private GitAccess gitAccess;

  /**
   * Creates the local repository and commits a few files.
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    gitAccess = GitAccess.getInstance();

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
   * Tests the revert commit functionality. Needs to find the conflicts
   * <p>
   * <b>Description:</b> test the revert.
   * </p>
   * <p>
   * <b>Bug ID:</b> EXM-47154
   * </p>
   *
   * @author Tudosie Razvan
   * 
   * @throws Exception
   */
  @Test
  public void testRevertCommit() throws Exception {
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
          if ("Merge_Branch1_Into_Branch2".equals(actionName)) {
            SwingUtilities.invokeLater(() -> action.actionPerformed(null));
            break;
          }
        }
      }
      flushAWT();

      // Confirm merge dialog
      JDialog mergeOkDialog = findDialog(translator.getTranslation(Tags.MERGE_BRANCHES));
      JButton mergeOkButton = findFirstButton(mergeOkDialog, translator.getTranslation(Tags.YES));
      mergeOkButton.doClick();

      sleep(200);

      conflictMergeDialog = findDialog(translator.getTranslation(Tags.MERGE_CONFLICTS_TITLE));
      assertNotNull(conflictMergeDialog);

      assertTrue(TestUtil.read(file1.toURI().toURL()).contains("<<<<<<< HEAD\n" + "local file 1 modifications\n"
          + "=======\n" + "local file 1 on new branch\n" + ">>>>>>>"));

      assertTrue(TestUtil.read(file2.toURI().toURL()).contains("<<<<<<< HEAD\n" + "local file 2 modifications\n"
          + "=======\n" + "local file 2 on new branch\n" + ">>>>>>>"));
      
      sleep(200);
      
      flushAWT();
      CommitCharacteristics commitToRevert = gitAccess.getCommitsCharacteristics(null).get(2);
      RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
      SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
      flushAWT();

      JDialog revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
      JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
      assertEquals(Tags.REVERT_COMMIT_WARNING, confirmationTextArea.getText().toString());
      JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
      revertOkButton.doClick();

      flushAWT();
     // sleep(700);
      JDialog errorDlg = findDialog(Tags.REVERT_COMMIT);
      JTextArea errorTextArea = findFirstTextArea(errorDlg);
      //assertEquals(Tags.UNCOMMITTED_CHANGES, errorTextArea.getText().toString());
      //JButton errorOkButton = findFirstButton(errorDlg, "OK");
      //errorOkButton.doClick();

    } finally {
      if (conflictMergeDialog != null) {
        conflictMergeDialog.setVisible(false);
        conflictMergeDialog.dispose();
      }
    }

  }

}
