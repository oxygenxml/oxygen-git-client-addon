package com.oxygenxml.git.view;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;
import com.oxygenxml.git.view.historycomponents.HistoryViewContextualMenuPresenter;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Tests for the code related with history.
 */
public class GitHistoryActionsTest extends GitTestBase {

  /**
   * Tests the contextual actions that are presented for different types of changes.
   *
   * @throws Exception
   */
  @Test
  public void testHistoryContextualActions() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_actions.txt");

    File wcTree = new File("target/gen/GitHistoryActionsTest_testHistoryContextualActions");
    RepoGenerationScript.generateRepository(script, wcTree);

    try {
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dumpHistory(commitsCharacteristics);
      System.out.println(dump);

      String expected = 
          "[ Changes. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ Second commit. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
          "[ First commit. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , null ]\n" + 
          "";

      String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
      expected = expected.replaceAll(regex, "DATE");
      dump = expected.replaceAll(regex, "DATE");

      assertEquals(expected, dump);

      HistoryViewContextualMenuPresenter presenter = new HistoryViewContextualMenuPresenter(null);

      List<Action> actions = new ArrayList<>();
      JPopupMenu jPopupMenu = Mockito.mock(JPopupMenu.class);
      
      //////////////////////////
      //  Changes.
      //////////////////////////
      Iterator<CommitCharacteristics> iterator = commitsCharacteristics.iterator();
      CommitCharacteristics commitCharacteristic = iterator.next();
      List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitCharacteristic.getCommitId());
      String dumpFS = dumpFS(changedFiles);
      assertEquals(
          "(changeType=CHANGED, fileLocation=file1.txt)\n" + 
          "(changeType=REMOVED, fileLocation=file2.txt)\n" + 
          "", dumpFS);

      // Assert the available actions for the changed file.
      actions.clear();
      actions = presenter.getContextualActions(changedFiles.get(0), commitCharacteristic, true);
      assertEquals("["
          + "Compare_file_with_previous_version, "
          + "Compare_file_with_working_tree_version, "
          + "Open_file]", dumpActions(actions));
      
      // A deleted file.
      actions.clear();
      actions = presenter.getContextualActions(changedFiles.get(1), commitCharacteristic, true);
      presenter.populateContextualActionsHistoryContext(jPopupMenu, "file2.txt", commitCharacteristic);
      assertEquals("[Open_previous_version]", dumpActions(actions));
      
      // Next COMMIT / REVISION
      commitCharacteristic = iterator.next();
      changedFiles = RevCommitUtil.getChangedFiles(commitCharacteristic.getCommitId());
      dumpFS = dumpFS(changedFiles);
      assertEquals(
          "(changeType=ADD, fileLocation=file2.txt)\n" + 
          "", dumpFS);
      actions.clear();
      actions = presenter.getContextualActions(changedFiles.get(0), commitCharacteristic, true);
      presenter.populateContextualActionsHistoryContext(jPopupMenu, "file2.txt", commitCharacteristic);
      assertEquals("[Open_file]", dumpActions(actions));
      
      // Next COMMIT / REVISION
      commitCharacteristic = iterator.next();
      changedFiles = RevCommitUtil.getChangedFiles(commitCharacteristic.getCommitId());
      dumpFS = dumpFS(changedFiles);
      assertEquals(
          "(changeType=ADD, fileLocation=file1.txt)\n" + 
          "", dumpFS);
      actions.clear();
      actions = presenter.getContextualActions(changedFiles.get(0), commitCharacteristic, true);
      assertEquals("[Open_file]", dumpActions(actions));

    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }
  }
  
  /**
   * Tests the uncommitted changes to see if they have any contextual actions.
   *
   * @throws Exception
   */
  @Test
  public void testHistoryUncommittedChangesActions() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_actions.txt");

    File wcTree = new File("target/gen/GitHistoryActionsTest_testHistoryUncommitedChangesActions");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    File file = new File(wcTree,"file.txt");
    file.createNewFile();
    setFileContent(file, "modified content");

    try {
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
      
      HistoryViewContextualMenuPresenter presenter = new HistoryViewContextualMenuPresenter(null);

      //////////////////////////
      //  Changes.
      //////////////////////////
      CommitCharacteristics commitCharacteristic = GitAccess.UNCOMMITED_CHANGES;
      List<FileStatus> changedFiles = RevCommitUtil.getChangedFiles(commitCharacteristic.getCommitId());
      String dumpFS = dumpFS(changedFiles);
      assertEquals(
          "(changeType=UNTRACKED, fileLocation=file.txt)\n" + 
          "", dumpFS);

      // Assert the available actions for the changed file.
      JPopupMenu jPopupMenu = new JPopupMenu();
      presenter.populateContextualActionsHistoryContext(jPopupMenu, null,  GitAccess.UNCOMMITED_CHANGES);
      assertEquals(0, jPopupMenu.getComponentCount());

    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }
  }
  
  private String dumpActions(List<Action> actions) {
    return actions.stream().map(action -> action.getValue(Action.NAME)).collect(Collectors.toList()).toString();
  }

  /**
   * A commit with a removed file. The commit has 2 parents (it's a merge).
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testOpenPreviousVersion_MergedBranches() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_actions_branches_merged.txt");
  
    File wcTree = new File("target/gen/GitHistoryActionsTest_testHistoryActions_MergedBranches");
    RepoGenerationScript.generateRepository(script, wcTree);
  
    try {
      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());
  
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics);
      System.out.println(dump);
  
      String expected = 
          "[ Merge branch 'master' , 19 Nov 2019 , AlexJitianu <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2, 3] ]\n" + 
          "[ Change file1.txt on Feature branch. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [4] ]\n" + 
          "[ Delete file2.txt master branch. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
          "[ First commit. , 19 Nov 2019 , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
          "";
  
      String regex = "(([0-9])|([0-2][0-9])|([3][0-1]))\\ (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\ \\d{4}";
      expected = expected.replaceAll(regex, "DATE");
      dump = expected.replaceAll(regex, "DATE");
  
      assertEquals(expected, dump);
  
      HistoryViewContextualMenuPresenter presenter = new HistoryViewContextualMenuPresenter(null);
  
      List<Action> actions = new ArrayList<>();
      JPopupMenu jPopupMenu = Mockito.mock(JPopupMenu.class);
      
      //////////////////////////
      //  Changes.
      //////////////////////////
      Iterator<CommitCharacteristics> iterator = commitsCharacteristics.iterator();
      CommitCharacteristics commitCharacteristics = iterator.next();
      List<FileStatus> changes = RevCommitUtil.getChangedFiles(commitCharacteristics.getCommitId());
      String dumpFS = dumpFS(changes);
      assertEquals(
          "(changeType=REMOVED, fileLocation=file2.txt)\n" + 
          "", dumpFS);
  
      // A deleted file.
      actions.clear();
      actions = presenter.getContextualActions(changes.get(0), commitCharacteristics, true);
      presenter.populateContextualActionsHistoryContext(jPopupMenu, "file2.txt", commitCharacteristics);
      assertEquals("[Open_previous_version]", dumpActions(actions));
      
      final StringBuilder b = new StringBuilder();
      Mockito.when(PluginWorkspaceProvider.getPluginWorkspace().open((URL)Mockito.anyObject())).thenAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          b.append(invocation.getArguments()[0].toString());
          return null;
        }
      });
      actions.get(0).actionPerformed(null);
      
      assertTrue("Previous version URL was not detected", b.toString().length() > 0);
      
      try (InputStream openStream = new URL(b.toString()).openStream()) {
        assertEquals("[file 2 content]", IOUtils.readLines(openStream).toString());
      }
      
    } finally {
      GitAccess.getInstance().closeRepo();
  
      FileUtils.deleteDirectory(wcTree);
    }
  }
}
