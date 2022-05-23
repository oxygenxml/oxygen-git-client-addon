  package com.oxygenxml.git.editorvars;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.event.GitController;

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
  private EditorVariablesResolver editorVariablesResolver = new GitEditorVariablesResolver(new GitController());

  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Create the local repository.
    createRepository(LOCAL_TEST_REPOSITORY);
    GitAccess.getInstance().setRepositorySynchronously(LOCAL_TEST_REPOSITORY);

  }

  /**
   * Tests the replacement of the editor variable for the short branch name.
   * 
   * @throws Exception
   */
  @Test
  public void testShortBranchNameEditorVariable() throws Exception {
    String actual = editorVariablesResolver.resolveEditorVariables(
        "- " + GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR + " -",
        null);
    assertEquals("- " + GitAccess.DEFAULT_BRANCH_NAME + " -", actual);
  }

  /**
   * Tests the replacement of the editor variable for the full branch name/ branch
   * path relative to the repository location.
   * 
   * @throws Exception
   */
  @Test
  public void testFullBranchNameEditorVariable() throws Exception {
    String actual = editorVariablesResolver.resolveEditorVariables(
        "- " + GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR + " -",
        null);
    assertEquals("- refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + " -", actual);
  }

  /**
   * Tests the replacement of the editor variable for the working copy name.
   * 
   * @throws Exception
   */
  @Test
  public void testWorkingCopyNameEditorVariable() throws Exception {
    String actual = editorVariablesResolver.resolveEditorVariables(
        "- " +  GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR + " -",
        null);
    assertEquals("- EditorVariablesTest -", actual);
  }

  /**
   * Tests the replacement of the editor variable for the working copy path.
   * 
   * @throws Exception
   */
  @Test
  public void testWorkingCopyPathEditorVariable() throws Exception {
    String actual = editorVariablesResolver.resolveEditorVariables(
        "- " + GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR + " -",
        null);
    assertEquals("- " + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath() + " -", actual);
  }
  
  /**
   * Tests the replacement of the editor variable for the working copy URL.
   * 
   * @throws Exception
   */
  @Test
  public void testWorkingCopyURLEditorVariable() throws Exception {
    String actual = editorVariablesResolver.resolveEditorVariables(
        "- " + GitEditorVariablesNames.WORKING_COPY_URL_EDITOR_VAR + " -",
        null);
    assertEquals("- " + new File(LOCAL_TEST_REPOSITORY).toURI().toURL().toString() + " -", actual);
  }

  /**
   * <p><b>Description:</b> Tests if is OK to open the same repository on multiple times</p>
   * <p><b>Bug ID:</b> EXM-50482</p>
   *
   * @author Alex_Smarandache
   */ 
  @Test
  public void testSameRepositoryOpened() {
    boolean isOk = true;
    try(Git git1 = Git.open(new File(LOCAL_TEST_REPOSITORY))) {
      try(Git git2 = Git.open(new File(LOCAL_TEST_REPOSITORY))) {     
      } 
    } catch (IOException e) {
      isOk = false;
    }
    
    assertTrue(isOk);
  }
}
