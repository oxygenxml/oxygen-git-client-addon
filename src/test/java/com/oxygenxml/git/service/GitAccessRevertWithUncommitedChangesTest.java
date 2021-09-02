package com.oxygenxml.git.service;

import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.history.CommitCharacteristics;
import com.oxygenxml.git.view.history.actions.RevertCommitAction;


/**
 * Tests the revert action 
 *
 * @author Tudosie Razvan
 */
public class GitAccessRevertWithUncommitedChangesTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessRevertCommitTest";
  private final static String LOCAL_FILE_NAME = "local.txt";
  private final static String LOCAL_FILE_NAME_2 = "local_2.txt";
  private File firstFile;
  private File secondFile;
  private GitAccess gitAccess;

  /**
   * Creates the local repository and commits a few files.
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

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

    // Create a second file and make a third commit
    secondFile = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME_2);
    secondFile.createNewFile();
    setFileContent(secondFile, "second local file");
    gitAccess.add(new FileStatus(GitChangeType.ADD, LOCAL_FILE_NAME_2));
    gitAccess.commit("Added a new file");

    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    // Unstaged file
    setFileContent(firstFile, "modified content AGAIN");
    // Staged file
    setFileContent(secondFile, "Huh");
    gitAccess.add(new FileStatus(GitChangeType.ADD, secondFile.getName()));
  }
  
  /**
   * <p><b>Description:</b> test the revert.</p>
   * <p><b>Bug ID:</b> EXM-47154</p>
   *
   * @author Tudosie Razvan
   * 
   * @throws Exception
   */
  @Test
  public void testRevertCommitWithUncommittedChanges() throws Exception {
    JDialog revertConfirmationDlg = null;

    // Initial status
    GitStatus status = gitAccess.getStatus();
    assertEquals(
        "[(changeType=MODIFIED, fileLocation=local.txt)]",
        status.getUnstagedFiles().toString());
    assertEquals(
        "[(changeType=CHANGED, fileLocation=local_2.txt)]",
        status.getStagedFiles().toString());

    // The history at this moment
    final List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    String initialHistory = "[ Uncommitted_changes , DATE , * , * , null , null ]\n" + 
        "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
        "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + 
        "";
    String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
    assertEquals(
        initialHistory,
        dumpHistory(commitsCharacteristics).replaceAll(regex, "DATE"));

    CommitCharacteristics commitToRevert = commitsCharacteristics.get(2);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();

    revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_CONFIRMATION,confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    revertOkButton.doClick();
    
    flushAWT();
    JDialog errorDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea errorTextArea = findFirstTextArea(errorDlg);
    assertEquals(Tags.REVERT_COMMIT_FAILED_UNCOMMITTED_CHANGES_MESSAGE, errorTextArea.getText().toString());
    JButton errorOkButton = findFirstButton(errorDlg, "Close");
    errorOkButton.doClick();

    flushAWT();
    List<CommitCharacteristics> commitsChr =  gitAccess.getCommitsCharacteristics(null);
    assertEquals(
        initialHistory,
        dumpHistory(commitsChr).replaceAll(regex, "DATE"));

  }
}
