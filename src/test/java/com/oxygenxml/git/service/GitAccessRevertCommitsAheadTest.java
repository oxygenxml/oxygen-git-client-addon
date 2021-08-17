package com.oxygenxml.git.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
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
public class GitAccessRevertCommitsAheadTest extends GitTestBase{
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
    JDialog revertConfirmationDlg = null;

    // Initial status
    GitStatus status = gitAccess.getStatus();

    // The history at this moment
    final List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    String expected = "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
        + "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
        + "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + "";
    String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
    assertEquals(expected, dumpHistory(commitsCharacteristics).replaceAll(regex, "DATE"));

    CommitCharacteristics commitToRevert = commitsCharacteristics.get(2);
    RevertCommitAction revertAction = new RevertCommitAction(commitToRevert);
    SwingUtilities.invokeLater(() -> revertAction.actionPerformed(null));
    flushAWT();

    revertConfirmationDlg = findDialog(Tags.REVERT_COMMIT);
    JTextArea confirmationTextArea = findFirstTextArea(revertConfirmationDlg);
    assertEquals(Tags.REVERT_COMMIT_WARNING, confirmationTextArea.getText().toString());
    JButton revertOkButton = findFirstButton(revertConfirmationDlg, Tags.YES);
    revertOkButton.doClick();
    
    expected = "";
    //sleep(1000);
    RevCommit lastCommit = getCommitsAhead();
    //assertEquals(expected, lastCommit.getName());
    
    
    List<CommitCharacteristics> commitsChr = gitAccess.getCommitsCharacteristics(null);
    expected = "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n"
        + "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n"
        + "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + "";
    assertEquals(expected, dumpHistory(commitsChr).replaceAll(regex, "DATE"));

  }
  
  /**
   * 
   * @return the last commit on this branch
   * @throws MissingObjectException
   * @throws IncorrectObjectTypeException
   * @throws IOException
   * @throws NoRepositorySelected
   */
  private RevCommit getCommitsAhead() throws MissingObjectException, IncorrectObjectTypeException, IOException, NoRepositorySelected
  {
   

      String shortBranchName = Repository.shortenRefName(gitAccess.getBranchInfo().getBranchName());
      String fullBranchName = Constants.R_HEADS + shortBranchName;
      Repository repository = gitAccess.getRepository();
      BranchConfig branchConfig = new BranchConfig(repository.getConfig(), shortBranchName);

      String trackingBranch = branchConfig.getTrackingBranch();
      if (trackingBranch == null) {
        return null;
      }

      Ref tracking = repository.exactRef(trackingBranch);
      if (tracking == null) {
        return null;
      }

      Ref local = repository.exactRef(fullBranchName);
      if (local == null) {
        return null;
      }

      try (RevWalk walk = new RevWalk(repository)) {
        RevCommit localCommit = walk.parseCommit(local.getObjectId());
        RevCommit trackingCommit = walk.parseCommit(tracking.getObjectId());

        walk.setRevFilter(RevFilter.MERGE_BASE);
        walk.markStart(localCommit);
        walk.markStart(trackingCommit);
        RevCommit mergeBase = walk.next();

        walk.reset();
        walk.setRevFilter(RevFilter.ALL);
        List<RevCommit> commitsAhead = RevWalkUtils.find(walk, localCommit, mergeBase);

        return commitsAhead.get(0);
      }
  }
}
