package com.oxygenxml.git.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;
import com.oxygenxml.git.utils.URIUtil;
import com.oxygenxml.git.view.event.GitController;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Test cases for {@link GitAccess}.
 */
public class GitAccessTest extends TestCase {

  String[] errMsg = new String[1];

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
    return GitAccess.getInstance().push(new SSHCapableUserCredentialsProvider("", "", "", ""), Optional.empty());
  }

  /**
   * @see junit.framework.TestCase.setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
    
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String message = (String) invocation.getArguments()[0];
        errMsg[0] = message;
        return null;
      }
    }).when(pluginWSMock).showErrorMessage(Mockito.anyString());
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);

    errMsg[0] = "";
  }
  
  @Override
  protected void tearDown() throws Exception {
    PluginWorkspaceProvider.setPluginWorkspace(null);
    
    super.tearDown();
  }

  /**
   * <p>
   * <b>Description:</b> better message when trying to push while having a
   * conflict.
   * </p>
   * <p>
   * <b>Bug ID:</b> EXM-47439
   * </p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testPushWhileInConflictState() throws Exception {
    try {
      Git gitMock = Mockito.mock(Git.class);
      Repository repoMock = Mockito.mock(Repository.class);
      
      Mockito.when(repoMock.getRepositoryState()).thenReturn(RepositoryState.MERGING);
      Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
      
      GitAccess.getInstance().setGit(gitMock);
      
      PushResponse pushResp = push("", "");
      assertEquals("status: REJECTED_OTHER_REASON message Resolve_conflicts_first", pushResp.toString());
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }


  /**
   * <p>
   * <b>Description:</b> test host extraction from url.
   * </p>
   * <p>
   * <b>Bug ID:</b> EXM-49000
   * </p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testHostNameExtraction() throws Exception {
    String url = "https://github.com/oxygenxml/oxygen-git-plugin.git";
    String host = URIUtil.extractHostName(url);
    assertEquals("github.com", host);
    
    url = "git@github.com:oxygenxml/oxygen-git-plugin.git";
    host = URIUtil.extractHostName(url);
    assertEquals("github.com", host);
    
    url = "ssh://user@github.com:1234/path";
    host = URIUtil.extractHostName(url);
    assertEquals("github.com", host);
  }
  
  
  /**
   * <p><b>Description:</b> better message when trying to pull while having a conflict.</p>
   * <p><b>Bug ID:</b> EXM-47439</p>
   * @author sorin_carbunaru
   * @throws Exception
   */
  public void testPullWhileInConflictState() throws Exception {
    try {
      Git gitMock = Mockito.mock(Git.class);
      Repository repoMock = Mockito.mock(Repository.class);
      StatusCommand statusCmdMock = Mockito.mock(StatusCommand.class);
      Status statusMock = Mockito.mock(Status.class);
      StoredConfig configMock = Mockito.mock(StoredConfig.class);
      
      Mockito.when(repoMock.getRepositoryState()).thenReturn(RepositoryState.MERGING);
      Mockito.when(repoMock.getConfig()).thenReturn(configMock);
      Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
      
      Mockito.when(statusMock.getConflicting()).thenReturn(new HashSet<>(Arrays.asList("string1")));
      Mockito.when(statusCmdMock.call()).thenReturn(statusMock);
      Mockito.when(gitMock.status()).thenReturn(statusCmdMock);
      
      GitAccess.getInstance().setGit(gitMock);
      
      GitController gitCtrl = new GitController(GitAccess.getInstance());
      Future<?> pullResp = gitCtrl.pull(Optional.empty());
      pullResp.get();
      
      Awaitility.await().atMost(500, TimeUnit.MILLISECONDS)
        .until(() -> "Pull_when_repo_in_conflict".equals(errMsg[0]));
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }

}
