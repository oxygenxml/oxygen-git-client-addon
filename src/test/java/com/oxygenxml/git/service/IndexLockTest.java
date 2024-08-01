package com.oxygenxml.git.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.OxygenGitPluginExtension;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.service.exceptions.IndexLockExistsException;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Tests related to having an index.lock file in the repo.
 */
public class IndexLockTest extends GitTestBase {

  /**
   * The local repo.
   */
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/IndexLockTest/localRepository1";
  
  /**
   * Access to the Git operations.
   */
  private GitAccess gitAccess;
  
  /**
   * Set up.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    gitAccess = GitAccess.getInstance();
    
    createRepository(LOCAL_TEST_REPOSITORY);
  }
  
  /**
   * <p><b>Description:</b> check that the proper message is shown
   * to the user when index.lock exists.</p>
   * <p><b>Bug ID:</b> EXM-46411</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testShowMessageIndexLockExists() throws Exception {
    String repoPath = gitAccess.getRepository().getDirectory().getAbsolutePath();
    new File(repoPath, "index.lock").createNewFile();
    
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    List<String> errorMessages = new ArrayList<>();
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        errorMessages.add((String) invocation.getArguments()[0]);
        return null;
      }
    }).when(pluginWS).showErrorMessage(Mockito.anyString());
    new OxygenGitPluginExtension().applicationStarted(pluginWS);

    // The following operations should fail with index.lock-related message
    gitAccess.abortMerge();
    gitAccess.abortRebase();
    gitAccess.add(new FileStatus(GitChangeType.ADD, ""));
    gitAccess.addAll(Collections.emptyList());
    gitAccess.applyStash("");
    gitAccess.checkoutCommit("", "");
    gitAccess.checkoutRemoteBranchWithNewName("", "", "");
    try {
      gitAccess.commit("");
    } catch (IndexLockExistsException e) {
      // ignore
    }
    gitAccess.continueRebase();
    gitAccess.createStash(false, "");
    gitAccess.mergeBranch("");
    gitAccess.popStash("");
    gitAccess.resetAll(Collections.emptyList());
    gitAccess.resetToCommit(ResetType.MIXED, "");
    gitAccess.resetToCommit(ResetType.HARD, "");
    gitAccess.revertCommit("");
    gitAccess.squashAndMergeBranch("", "");
    
    assertEquals(17, errorMessages.size());
    assertEquals(17, errorMessages.stream().filter(msg -> msg.equals("Lock_failed_explanation")).count());
    
    // The following operations don't fail
    gitAccess.createBranch("");
    gitAccess.createBranch("", "");
    gitAccess.deleteBranch("");
    gitAccess.deleteTags(false, "");
    gitAccess.dropAllStashes();
    gitAccess.dropStash(0);
    
    assertEquals(17, errorMessages.size());
    assertEquals(17, errorMessages.stream().filter(msg -> msg.equals("Lock_failed_explanation")).count());
    
  }
  
}
