package com.oxygenxml.git.editorvars;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.GitEditorVariablesNames;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;

import ro.sync.exml.workspace.api.util.EditorVariablesResolver;

/**
 * Tests the replacement of the git editor variables with the adequate
 * names/paths.
 * 
 * @author Bogdan Draghici
 *
 */
public class GitEditorVariablesTest extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/EditorVariablesTest";
  private final static String LOCAL_FILE_NAME = "local.txt";
  private File fileWithEditorVariables;
  private EditorVariablesResolver editorVariablesResolver;
  private GitAccess gitAccess;

  /**
   * Creates the file for editor variables, the EditorVariablesResolver and a
   * local repository, then sets the repository.
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();

    gitAccess = GitAccess.getInstance();

    // Create first file make the first commit for the local repository.
    fileWithEditorVariables = new File(LOCAL_TEST_REPOSITORY, LOCAL_FILE_NAME);

    // Create the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    gitAccess.setRepositorySynchronously(LOCAL_TEST_REPOSITORY);

    editorVariablesResolver = GitEditorVariablesResolver.createEditorVariablesResolver();
  }

  /**
   * Tests the replacement of the editor variable for the short branch name.
   * 
   * @throws Exception
   */
  @Test
  public void testShortBranchNameEditorVariable() throws Exception {
    setFileContent(fileWithEditorVariables,
        "the current branch name is: " + GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR);
    String actual = editorVariablesResolver.resolveEditorVariables(getFileContent(),
        LOCAL_TEST_REPOSITORY + "/" + LOCAL_FILE_NAME);
    String expected = "the current branch name is: master";
    assertEquals(expected, actual);
  }

  /**
   * Tests the replacement of the editor variable for the full branch name/ branch
   * path relative to the repository location.
   * 
   * @throws Exception
   */
  @Test
  public void testFullBranchNameEditorVariable() throws Exception {
    setFileContent(fileWithEditorVariables,
        "the current branch path is: " + GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR);
    String actual = editorVariablesResolver.resolveEditorVariables(getFileContent(),
        LOCAL_TEST_REPOSITORY + "/" + LOCAL_FILE_NAME);
    String expected = "the current branch path is: refs/heads/master";
    assertEquals(expected, actual);
  }

  /**
   * Tests the replacement of the editor variable for the working copy name.
   * 
   * @throws Exception
   */
  @Test
  public void testWorkingCopyNameEditorVariable() throws Exception {
    setFileContent(fileWithEditorVariables,
        "the working copy name is: " + GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR);
    String actual = editorVariablesResolver.resolveEditorVariables(getFileContent(),
        LOCAL_TEST_REPOSITORY + "/" + LOCAL_FILE_NAME);
    String expected = "the working copy name is: EditorVariablesTest";
    assertEquals(expected, actual);
  }

  /**
   * Tests the replacement of the editor variable for the working copy path.
   * 
   * @throws Exception
   */
  @Test
  public void testWorkingCopyPathEditorVariable() throws Exception {
    setFileContent(fileWithEditorVariables,
        "the working copy path is: " + GitEditorVariablesNames.WORKING_COPY_FILE_PATH_EDITOR_VAR);
    String actual = editorVariablesResolver.resolveEditorVariables(getFileContent(),
        LOCAL_TEST_REPOSITORY + "/" + LOCAL_FILE_NAME);
    String expected = "the working copy path is: " + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath();
    assertEquals(expected, actual);
  }

  /**
   * Gets the content of the file that contains the editor variables.
   * 
   * @return The file content.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private String getFileContent() throws FileNotFoundException, IOException {
    FileReader fr = new FileReader(LOCAL_TEST_REPOSITORY + "/" + LOCAL_FILE_NAME);
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
