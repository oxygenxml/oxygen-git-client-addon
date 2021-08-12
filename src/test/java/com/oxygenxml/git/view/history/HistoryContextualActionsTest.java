package com.oxygenxml.git.view.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Tests the functionality of the created actions from com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.
 * 
 * @author Alex_Smarandache
 *
 */
public class HistoryContextualActionsTest {

  /**
   * The local repository.
   */
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GItAccessStagedFilesTest";

  /**
   * The GitAccess instance.
   */
  private GitAccess gitAccess;

  /**
   * Initialise the git, repository and first local commit.
   * 
   * @throws IllegalStateException
   * @throws GitAPIException
   */
  @Before
  public void init() throws IllegalStateException, GitAPIException {
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITORY);
    File file = new File(LOCAL_TEST_REPOSITORY + "/test.xpr");
    File file2 = new File(LOCAL_TEST_REPOSITORY + "/test2.txt");
    try {
      file.createNewFile();
      file2.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
    gitAccess.add(new FileStatus(GitChangeType.ADD, file2.getName()));
    gitAccess.commit("file test added");
  }

  /**
   * <p><b>Description:</b> Tests the com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.
   * createOpenWorkingCopyFileAction(FileStatus, String, boolean) API.</p>
   * 
   * <p><b>Bug ID:</b> EXM-47571</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */

  @Test
  public void testOpenWCMethod() throws Exception {
    String[] urlOpenedFile = new String[1];
    PluginWorkspace pluginWorkspace = Mockito.mock(PluginWorkspace.class);
    Mockito.when(pluginWorkspace.open(Mockito.any(URL.class), Mockito.any(String.class), Mockito.any(String.class)))
        .thenAnswer(new Answer<Boolean>() {
          @Override
          public Boolean answer(InvocationOnMock invocation) throws Throwable {
            File file = new File(((URL)(invocation.getArgument(0))).getFile());
            urlOpenedFile[0] = file.getName();
            return true;
          }
        });

    try (MockedStatic<PluginWorkspaceProvider> provider = Mockito.mockStatic(PluginWorkspaceProvider.class)) {

      provider.when(() -> PluginWorkspaceProvider.getPluginWorkspace()).thenReturn(pluginWorkspace);

      assertNotNull(PluginWorkspaceProvider.getPluginWorkspace());
      HistoryViewContextualMenuPresenter historyContextualMenu = new HistoryViewContextualMenuPresenter(null);
      historyContextualMenu
          .createOpenWorkingCopyFileAction(new FileStatus(GitChangeType.RENAME, LOCAL_TEST_REPOSITORY + "/test.xpr"),
              LOCAL_TEST_REPOSITORY + "/test.xpr", false)
          .actionPerformed(null);
    }
    
    assertEquals("test.xpr", urlOpenedFile[0]);
  }

}
