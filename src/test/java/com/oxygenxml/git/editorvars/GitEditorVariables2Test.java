package com.oxygenxml.git.editorvars;

import java.io.File;
import java.util.Map;

import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitController;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.event.BranchGitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;

/**
 * Git editor variables test.
 */
public class GitEditorVariables2Test extends GitTestBase {
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/EditorVariablesTest";
  private GitEditorVariablesResolver editorVariablesResolver;
  int noOfShortBranchCalls;
  int noOfFullBranchCalls;
  int noOfWCCalls;
  
  @Before
  public void setUp() throws Exception {
    super.setUp();
    
    noOfShortBranchCalls = 0;
    noOfFullBranchCalls = 0;
    noOfWCCalls = 0;
    
    Repository repo = createRepository(LOCAL_TEST_REPOSITORY);
    GitAccess.getInstance().setRepositorySynchronously(LOCAL_TEST_REPOSITORY);
    
    GitAccess gitAccessMock = Mockito.mock(GitAccess.class);
    Mockito.doAnswer(new Answer<BranchInfo>() {
      @Override
      public BranchInfo answer(InvocationOnMock invocation) throws Throwable {
        noOfShortBranchCalls++;
        return new BranchInfo("master", false);
      }
    }).when(gitAccessMock).getBranchInfo();
    
    Mockito.doAnswer(new Answer<Repository>() {
      @Override
      public Repository answer(InvocationOnMock invocation) throws Throwable {
        noOfFullBranchCalls++;
        return repo;
      }
    }).when(gitAccessMock).getRepository();
    
    Mockito.doAnswer(new Answer<File>() {
      @Override
      public File answer(InvocationOnMock invocation) throws Throwable {
        noOfWCCalls++;
        return new File(LOCAL_TEST_REPOSITORY);
      }
    }).when(gitAccessMock).getWorkingCopy();
    
    editorVariablesResolver = new GitEditorVariablesResolver(new GitController(gitAccessMock));
  }

  /**
   * <p><b>Description:</b> test Git editor variables cache.</p>
   * <p><b>Bug ID:</b> EXM-46457</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCache() throws Exception {
    Map<String, String> cache = editorVariablesResolver.getEditorVarsCacheFromTests();
    assertTrue(cache.isEmpty());
    
    String resolved = editorVariablesResolver.resolveEditorVariables("bla", null);
    assertEquals("bla", resolved);
    assertTrue(cache.isEmpty());
    
    // Short branch - git access should be called
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
        null);
    assertEquals("master", resolved);
    assertEquals("{${git(short_branch_name)}=master}", cache.toString());
    assertEquals(1, noOfShortBranchCalls);
    
    // Short branch again - get it from cache
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR,
        null);
    assertEquals("master", resolved);
    assertEquals("{${git(short_branch_name)}=master}", cache.toString());
    assertEquals(1, noOfShortBranchCalls);
    
    // Full branch name - git access should be called
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
        null);
    assertEquals("refs/heads/master", resolved);
    assertEquals(2, cache.size());
    assertTrue(cache.toString().contains("${git(short_branch_name)}=master"));
    assertTrue(cache.toString().contains("${git(full_branch_name)}=refs/heads/master"));
    assertEquals(1, noOfFullBranchCalls);
    
    // Full branch name again - get it from the cache
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR,
        null);
    assertEquals("refs/heads/master", resolved);
    assertEquals(2, cache.size());
    assertTrue(cache.toString().contains("${git(short_branch_name)}=master"));
    assertTrue(cache.toString().contains("${git(full_branch_name)}=refs/heads/master"));
    assertEquals(1, noOfFullBranchCalls);
    
    // WC name + WC path - get them from git access, but request WC only once
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR 
            + " "
            + GitEditorVariablesNames.WORKING_COPY_PATH_EDITOR_VAR,
        null);
    assertEquals("EditorVariablesTest " + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath(), resolved);
    assertEquals(4, cache.size());
    assertTrue(cache.toString().contains("${git(short_branch_name)}=master"));
    assertTrue(cache.toString().contains("${git(full_branch_name)}=refs/heads/master"));
    assertTrue(cache.toString().contains("${git(working_copy_name)}=EditorVariablesTest"));
    assertTrue(cache.toString().contains("${git(working_copy_path)}=" + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath()));
    assertEquals(1, noOfWCCalls);
    
    // WC name + WC path again - get them from git access
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.WORKING_COPY_NAME_EDITOR_VAR,
        null);
    assertEquals("EditorVariablesTest", resolved);
    assertEquals(4, cache.size());
    assertTrue(cache.toString().contains("${git(short_branch_name)}=master"));
    assertTrue(cache.toString().contains("${git(full_branch_name)}=refs/heads/master"));
    assertTrue(cache.toString().contains("${git(working_copy_name)}=EditorVariablesTest"));
    assertTrue(cache.toString().contains("${git(working_copy_path)}=" + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath()));
    assertEquals(1, noOfWCCalls);
    
    // Simulate branch switch
    editorVariablesResolver.getGitEventListenerFromTests().operationSuccessfullyEnded(
        new BranchGitEventInfo(GitOperation.CHECKOUT, ""));
    assertEquals(2, cache.size());
    assertTrue(cache.toString().contains("${git(working_copy_name)}=EditorVariablesTest"));
    assertTrue(cache.toString().contains("${git(working_copy_path)}=" + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath()));
    
    // Short + full branch name again - take them from git access
    resolved = editorVariablesResolver.resolveEditorVariables(
        GitEditorVariablesNames.SHORT_BRANCH_NAME_EDITOR_VAR + " " + GitEditorVariablesNames.FULL_BRANCH_NAME_EDITOR_VAR ,
        null);
    assertEquals("master refs/heads/master", resolved);
    assertEquals(4, cache.size());
    assertTrue(cache.toString().contains("${git(short_branch_name)}=master"));
    assertTrue(cache.toString().contains("${git(full_branch_name)}=refs/heads/master"));
    assertTrue(cache.toString().contains("${git(working_copy_name)}=EditorVariablesTest"));
    assertTrue(cache.toString().contains("${git(working_copy_path)}=" + new File(LOCAL_TEST_REPOSITORY).getAbsolutePath()));
    assertEquals(2, noOfShortBranchCalls);
    assertEquals(2, noOfFullBranchCalls);
    
    // Simulate repo switch
    editorVariablesResolver.getGitEventListenerFromTests().operationSuccessfullyEnded(
        new WorkingCopyGitEventInfo(GitOperation.OPEN_WORKING_COPY, new File(".")));
    assertTrue(cache.isEmpty());
  }

}
