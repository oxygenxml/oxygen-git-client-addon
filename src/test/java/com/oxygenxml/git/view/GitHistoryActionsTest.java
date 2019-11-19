package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;
import com.oxygenxml.git.view.historycomponents.HistoryViewContextualMenuPresenter;

/**
 * Tests for the code related with history.
 */
public class GitHistoryActionsTest extends GitTestBase {

  /**
   * Tests the commit revisions retrieval.
   * 
   * @throws Exception
   */
  @Test
  public void testHistory() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script_actions.txt");

    File wcTree = new File("target/gen/GitHistoryTest_testHistory");
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

      expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));

      assertEquals(
          expected, dump);

      HistoryViewContextualMenuPresenter presenter = new HistoryViewContextualMenuPresenter(null);

      final List<Action> actions = new ArrayList<Action>();
      JPopupMenu jPopupMenu = Mockito.mock(JPopupMenu.class);
      Mockito.when(jPopupMenu.add((Action) Mockito.anyObject())).thenAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          actions.add((Action) invocation.getArguments()[0]);
          return null;
        }
      });
      
      //////////////////////////
      //  Changes.
      //////////////////////////
      Iterator<CommitCharacteristics> iterator = commitsCharacteristics.iterator();
      CommitCharacteristics t = iterator.next();
      String dumpFS = dumpFS(RevCommitUtil.getChangedFiles(t.getCommitId()));
      assertEquals(
          "(changeType=CHANGED, fileLocation=file1.txt)\n" + 
          "(changeType=REMOVED, fileLocation=file2.txt)\n" + 
          "", dumpFS);

      // Assert the available actions for the changed file.
      actions.clear();
      presenter.populateContextualActions(jPopupMenu, "file1.txt", t);
      assertEquals("[Open_file, Compare_file_with_previous_version, Compare_file_with_working_tree_version]", dumpActions(actions));
      
      // A deleted file.
      actions.clear();
      presenter.populateContextualActions(jPopupMenu, "file2.txt", t);
      assertEquals("[Open_previous_version]", dumpActions(actions));
      
      // Next COMMIT / REVISION
      t = iterator.next();
      dumpFS = dumpFS(RevCommitUtil.getChangedFiles(t.getCommitId()));
      assertEquals(
          "(changeType=ADD, fileLocation=file2.txt)\n" + 
          "", dumpFS);
      actions.clear();
      presenter.populateContextualActions(jPopupMenu, "file2.txt", t);
      assertEquals("[Open_file]", dumpActions(actions));
      
      // Next COMMIT / REVISION
      t = iterator.next();
      dumpFS = dumpFS(RevCommitUtil.getChangedFiles(t.getCommitId()));
      assertEquals(
          "(changeType=ADD, fileLocation=file1.txt)\n" + 
          "", dumpFS);
      actions.clear();
      presenter.populateContextualActions(jPopupMenu, "file1.txt", t);
      assertEquals("[Open_file]", dumpActions(actions));

    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }
  }

  private String dumpActions(List<Action> actions) {
    return actions.stream().map(action -> action.getValue(Action.NAME)).collect(Collectors.toList()).toString();
  }
}
