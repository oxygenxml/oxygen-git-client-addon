package com.oxygenxml.git.view.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.eclipse.jgit.api.Git;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.FileContextualAction;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Tests the functionality of the created actions from
 * com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.
 * 
 * @author Alex_Smarandache
 *
 */
public class HistoryContextualActionsTest extends GitTestBase {

  /**
   * The local repository.
   */
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GItAccessStagedFilesTest";

  /**
   * The GitAccess instance.
   */
  private GitAccess gitAccess;

  
  /**
   * <p>
   * <b>Description:</b> Tests the
   * com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.
   * createOpenWorkingCopyFileAction(FileStatus, String, boolean) API.
   * </p>
   * 
   * <p>
   * <b>Bug ID:</b> EXM-47571
   * </p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testOpenWCMethod() throws Exception {
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITORY);
    
    File file = new File(LOCAL_TEST_REPOSITORY + "/test.xpr");
    file.createNewFile();
    
    gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
    gitAccess.commit("file test added");
    String[] urlOpenedFile = new String[1];
    PluginWorkspace pluginWorkspace = Mockito.mock(PluginWorkspace.class);
    Mockito.when(pluginWorkspace.open(Mockito.any(URL.class), Mockito.any(String.class), Mockito.any(String.class)))
        .thenAnswer((Answer<Boolean>) invocation -> {
          File file1 = new File(((URL) (invocation.getArgument(0))).getFile());
          urlOpenedFile[0] = file1.getName();
          return true;
        });

    try (MockedStatic<PluginWorkspaceProvider> provider = Mockito.mockStatic(PluginWorkspaceProvider.class);
        MockedStatic<RevCommitUtil> revCommitUtil = Mockito.mockStatic(RevCommitUtil.class);) {
      
      revCommitUtil.when(() -> RevCommitUtil.getNewPathInWorkingCopy(
          (Git) Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(LOCAL_TEST_REPOSITORY + "/test.xpr");
      
      provider.when(() -> PluginWorkspaceProvider.getPluginWorkspace()).thenReturn(pluginWorkspace);
      assertNotNull(PluginWorkspaceProvider.getPluginWorkspace());
      
      HistoryViewContextualMenuPresenter historyContextualMenu = new HistoryViewContextualMenuPresenter(null);
      AbstractAction openWCVersionAction = historyContextualMenu.createOpenWorkingCopyFileAction(
          new FileStatus(GitChangeType.RENAME, LOCAL_TEST_REPOSITORY + "/test.xpr"),
          LOCAL_TEST_REPOSITORY + "/test.xpr",
          false);
      openWCVersionAction.actionPerformed(null);
    }

    assertEquals("test.xpr", urlOpenedFile[0]);

  }
  

  /**
   * <p>
   * <b>Description:</b> Tests the
   * com.oxygenxml.git.view.history.HistoryViewContextualMenuPresenter.createCheckoutFileAction(
   * String, FileStatus, boolean) API.
   * </p>
   * 
   * <p>
   * <b>Bug ID:</b> EXM-46986
   * </p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testCheckoutFileAction() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_actions.txt");
    File wcTree = new File("target/gen/GitHistoryActionsTest_testHistoryContextualActions");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

    List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
   
    Iterator<CommitCharacteristics> iterator = commitsCharacteristics.iterator();
    CommitCharacteristics commitCharacteristic = iterator.next();
    HistoryViewContextualMenuPresenter presenter = new HistoryViewContextualMenuPresenter(null);
    List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitCharacteristic.getCommitId());
    List<FileContextualAction> actions = presenter.getFileContextualActions(changedFiles.get(0), commitCharacteristic, true);
    actions.removeIf(e -> e == null || !e.getValue(Action.NAME).toString().contains("Reset_file"));
    String[] checkoutFile = new String[2];
    
    try (MockedStatic<GitAccess> git = Mockito.mockStatic(GitAccess.class)) {
      GitAccess gitAcc = Mockito.mock(GitAccess.class);
      Mockito.doAnswer((Answer<Void>) invocation -> {
        checkoutFile[0] = invocation.getArgument(0);
        checkoutFile[1] = invocation.getArgument(1);
        return null;
      }).when(gitAcc).checkoutCommitForFile(Mockito.any(String.class), Mockito.any(String.class));
      git.when(GitAccess::getInstance).thenReturn(gitAcc);
      
      try (MockedStatic<GitOperationScheduler> sch = Mockito.mockStatic(GitOperationScheduler.class)) {
        GitOperationScheduler scheduler = Mockito.mock(GitOperationScheduler.class);
        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class))).thenAnswer((Answer<ScheduledFuture<?>>) invocation -> {
          ((Runnable)invocation.getArgument(0)).run();
          return null;
        });
        sch.when(GitOperationScheduler::getInstance).thenReturn(scheduler);
        actions.get(0).actionPerformed(null);
      }     
    }
 
    assertEquals(checkoutFile[0], changedFiles.get(0).getFileLocation());
    assertEquals(checkoutFile[1], commitCharacteristic.getCommitId());
  }

  
  /**
   * <p>
   * <b>Description:</b> Tests the
   * com.oxygenxml.git.service.GitAccess.checkoutCommitForFile(String, String) API.
   * </p>
   * 
   * <p>
   * <b>Bug ID:</b> EXM-46986
   * </p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testCheckoutFileMethodFunctionality() throws Exception {
    final String location = "src/test/resources/EXM-46986";
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(location);
    
    File file = new File(location + "/source.txt");
    file.createNewFile();
    gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
    gitAccess.commit("file test added");
    
    try (PrintWriter out = new PrintWriter(location + "/source.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }   
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    gitAccess.commit("file test modified");
    
    String commitID = gitAccess.getLatestCommitOnCurrentBranch().getId().getName();
    BufferedReader reader = new BufferedReader(new FileReader(location + "/source.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
    
    try (PrintWriter out = new PrintWriter(location + "/source.txt")) {
      out.println("oxygen");
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    gitAccess.commit("file updated");
    
    reader = new BufferedReader(new FileReader(location + "/source.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("oxygen", content);
   
    gitAccess.checkoutCommitForFile("source.txt" , commitID);
    reader = new BufferedReader(new FileReader(location + "/source.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
  }
  
}
