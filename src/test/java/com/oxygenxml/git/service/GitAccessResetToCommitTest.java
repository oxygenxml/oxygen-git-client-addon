package com.oxygenxml.git.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

public class GitAccessResetToCommitTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GitAccessRestoreLastCommitTest";
  private final static String LOCAL_FILE_NAME = "local.txt";
  private final static String LOCAL_FILE_NAME_2 = "local_2.txt";
  private File firstFile;
  private File secondFile;

  private GitAccess gitAccess;

  /**
   * Creates the local repository and commits a few files.
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();
    
    // Create the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    
    // Create first file make the first commit for the local repository.
    firstFile= new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME);
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
  }

  /**
   * Tests the soft reset to a commit functionality.
   * 
   * @throws Exception
   */
  @Test
  public void testSoftResetToCommit() throws Exception {
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    for (CommitCharacteristics commitCharacteristics : commitsCharacteristics) {
      gitAccess.resetToCommit(ResetType.SOFT, commitCharacteristics.getCommitId());
      assertEquals("modified content", getFileContent(LOCAL_TEST_REPOSITORY + LOCAL_FILE_NAME));
      assertEquals("second local file", getFileContent(LOCAL_TEST_REPOSITORY + LOCAL_FILE_NAME_2));
    }
  }
  
  /**
   * <p><b>Description:</b> test the mixed reset. The mixed reset keeps the
   * unstaged changes as they are and unstages the staged changes.</p>
   * <p><b>Bug ID:</b> EXM-46227</p>
   *
   * @author bogdan_draghici, sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testMixedResetToCommit() throws Exception {

    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);

    // Unstaged file
    setFileContent(firstFile, "modified content AGAIN");
    // Staged file
    setFileContent(secondFile, "Huh");
    gitAccess.add(new FileStatus(GitChangeType.ADD, secondFile.getName()));

    // Initial status
    GitStatus status = gitAccess.getStatus();
    assertEquals(
        "[(changeType=MODIFIED, fileLocation=local.txt)]",
        status.getUnstagedFiles().toString());
    assertEquals(
        "[(changeType=CHANGED, fileLocation=local_2.txt)]",
        status.getStagedFiles().toString());

    // The history at this moment
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    String expected = "[ Uncommitted changes , DATE , * , * , null , null ]\n" + 
        "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
        "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + 
        "";
    String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
    assertEquals(
        expected,
        dumpHistory(commitsCharacteristics).replaceAll(regex, "DATE"));
    
    // <<< MIXED RESET >>>
    gitAccess.resetToCommit(ResetType.MIXED, commitsCharacteristics.get(2).getCommitId());

    // The unstaged file is kept, and the staged one is now unstaged
    status = gitAccess.getStatus();
    assertEquals(
        "[(changeType=UNTRACKED, fileLocation=local_2.txt),"
        + " (changeType=MODIFIED, fileLocation=local.txt)]",
        status.getUnstagedFiles().toString());
    assertEquals(
        "[]",
        status.getStagedFiles().toString());
    
    commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    expected = "[ Uncommitted changes , DATE , * , * , null , null ]\n" + 
       // This commit is missing from the history, because we reset the branch to it
       // "[ Added a new file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
        "[ Modified a file , DATE , AlexJitianu <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
        "[ First commit. , DATE , AlexJitianu <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + 
        "";
    assertEquals(
        expected,
        dumpHistory(commitsCharacteristics).replaceAll(regex, "DATE"));
    
  }
  
  /**
   * Tests the hard reset to a commit functionality.
   * 
   * @throws Exception
   */
  @Test
  public void testHardResetToCommit() throws Exception {
    
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    List<CommitCharacteristics> commitsCharacteristics = gitAccess.getCommitsCharacteristics(null);
    //TODO see why it does not modify the files.
    gitAccess.resetToCommit(ResetType.HARD, commitsCharacteristics.get(1).getCommitId());
    gitAccess.fetch();
    assertEquals("modified content", getFileContent(LOCAL_TEST_REPOSITORY + LOCAL_FILE_NAME));
    assertEquals("", getFileContent(LOCAL_TEST_REPOSITORY + LOCAL_FILE_NAME_2));
  }
  
  /**
   * Gets the content from a file.
   * 
   * @param FilePath The path to the file from which to extract the content.
   * 
   * @return The content of the file.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private String getFileContent(String FilePath) throws FileNotFoundException, IOException {
    FileReader fr = new FileReader(FilePath);
    BufferedReader br = new BufferedReader(fr);

    String sCurrentLine;

    String content = "";
    while ((sCurrentLine = br.readLine()) != null) {
      content += sCurrentLine;
    }
    br.close();
    fr.close();
    return content;
  }
}
