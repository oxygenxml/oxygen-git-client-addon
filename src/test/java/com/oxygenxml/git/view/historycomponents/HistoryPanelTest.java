package com.oxygenxml.git.view.historycomponents;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.MenuElement;

import org.apache.commons.io.FileUtils;
import org.powermock.api.mockito.PowerMockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.GitController;

/**
 * UI level tests for history.
 *  
 * @author alex_jitianu
 */
public class HistoryPanelTest extends GitTestBase {

  private HistoryPanel historyPanel;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    setUpHistoryPanel();
  }

  /**
   * Tests the affected files presented when a revision is selected inside the history panel.
   * 
   * @throws Exception If it fails.
   */
  public void testAffectedFiles() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/history_script.txt");

    File wcTree = new File("target/gen/HistoryPanelTest/testAffectedFiles");
    RepoGenerationScript.generateRepository(script, wcTree);

    try {

      GitAccess.getInstance().setRepositorySynchronously(wcTree.getAbsolutePath());

      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

      String dump = dumpHistory(commitsCharacteristics);

      String expected =  
          "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
              "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]\n" + 
              "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]\n" + 
              "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]\n" + 
              "";

      expected = replaceDate(expected);

      assertEquals(
          expected, dump);

      historyPanel.showRepositoryHistory();

      JTable historyTable = historyPanel.historyTable;

      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

      dump = dumpHistory(model.getAllCommits());

      assertEquals(
          expected, dump);

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, 0, "[ Root file changed. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=CHANGED, fileLocation=root.txt)\n");


      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, 1, "[ Root file. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , [3] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=ADD, fileLocation=root.txt)\n");

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, 2, "[ Changes. , {date} , Alex <alex_jitianu@sync.ro> , 3 , AlexJitianu , [4] ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, 
          "(changeType=ADD, fileLocation=f2/file1.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file3_renamed.txt)\n" + 
              "(changeType=CHANGED, fileLocation=f2/file2.txt)\n" + 
              "(changeType=REMOVED, fileLocation=f2/file3.txt)\n" + 
              "(changeType=REMOVED, fileLocation=f2/file4.txt)\n" + 
          "");

      //-----------
      // Select an entry in the revision table.
      //-----------
      selectAndAssertRevision(historyTable, 3, "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 4 , AlexJitianu , null ]");

      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, 
          "(changeType=ADD, fileLocation=f1/file1.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file2.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file3.txt)\n" + 
              "(changeType=ADD, fileLocation=f2/file4.txt)\n" + 
              "(changeType=ADD, fileLocation=newProject.xpr)\n" + 
          "");

    } finally {
      GitAccess.getInstance().closeRepo();

      FileUtils.deleteDirectory(wcTree);
    }

  }

  private void setUpHistoryPanel() {
    // Initialize history panel.
    historyPanel = new HistoryPanel(new GitController()) {
      @Override
      protected int getUpdateDelay() {
        return 0;
      }

      @Override
      public boolean isShowing() {
        // Branch related refresh is done only if the view is displayed.
        return true;
      }
    };

  }

  /**
   * Asserts the presented affected files.
   * 
   * @param historyPanel History table.
   * @param expected The expected content.
   */
  private void assertAffectedFiles(HistoryPanel historyPanel, String expected) {
    JTable affectedFilesTable = historyPanel.affectedFilesTable;
    StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) affectedFilesTable.getModel();
    String dumpFS = dumpFS(affectedFilesModel.getFilesStatuses());

    assertEquals(expected, dumpFS);
  }

  /**
   * Selects a specific revision in the history table and asserts its description. 
   * 
   * @param historyTable History table.
   * @param row Which row to select.
   * @param expected The expected revision description.
   */
  private void selectAndAssertRevision(JTable historyTable, int row, String expected) {
    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
    historyTable.getSelectionModel().setSelectionInterval(row, row);
    // There is a timer involved.
    sleep(10);
    flushAWT();
    CommitCharacteristics selectedObject = (CommitCharacteristics) model.getValueAt(historyTable.getSelectedRow(), 0);
    assertEquals(replaceDate(expected), toString(selectedObject));
  }

  private String replaceDate(String expected) {
    return expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
  }

  /**
   * Changing branches fires notification.
   * 
   * @throws Exception If it fails.
   */
  public void testChangeBranchEvent() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/git_branch_events.txt");

    File wcTree = new File("target/gen/GitHistoryTest_testChangeBranchEvent");
    generateRepositoryAndLoad(script, wcTree);

    List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);

    String dump = dumpHistory(commitsCharacteristics);

    String expected =  
        "[ New branch , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
            "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
            "";
    expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));

    expected = replaceDate(expected);

    assertEquals(
        expected, dump);

    historyPanel.showRepositoryHistory();

    JTable historyTable = historyPanel.historyTable;

    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits());

    assertEquals(
        expected, dump);

    //=======================
    // Change branch.
    //=======================
    GitAccess.getInstance().setBranch("master");
    
    // History panel uses the scheduler to perform the change.
    ScheduledFuture schedule = GitOperationScheduler.getInstance().schedule(() -> {});
    schedule.get();

    model = (HistoryCommitTableModel) historyTable.getModel();

    dump = dumpHistory(model.getAllCommits());

    expected = "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n";
    expected = expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
    assertEquals(expected, dump);
  }

  /**
   * Tests the affected files presented when a revision is selected inside the history panel.
   * 
   * @throws Exception If it fails.
   */
  public void testAffectedFiles_ShowRenames() throws Exception {
    generateRepositoryAndLoad(
        getClass().getClassLoader().getResource("scripts/history_script_rename.txt"), 
        new File("target/gen/HistoryPanelTest/testAffectedFiles_ShowRenames"));
  
    try {
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
  
      String dump = dumpHistory(commitsCharacteristics, true);
  
      String expected =  
          "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]\n" + 
          "[ First commit. , {date} , Alex <alex_jitianu@sync.ro> , 2 , AlexJitianu , null ]\n" + 
          "";
  
      assertEquals(expected, dump);
  
      historyPanel.showRepositoryHistory();
  
      JTable historyTable = historyPanel.historyTable;
  
      HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
  
      dump = dumpHistory(model.getAllCommits(), true);
  
      assertEquals(expected, dump);
  
      //-----------
      // Select an entry in the revision table.
      //-----------
      expected = "[ Rename. , {date} , Alex <alex_jitianu@sync.ro> , 1 , AlexJitianu , [2] ]";
      expected = replaceDate(expected);
      
      selectAndAssertRevision(historyTable, 0, expected);
  
      //-----------
      // Assert the affected files
      //-----------
      assertAffectedFiles(historyPanel, "(changeType=RENAME, fileLocation=file_renamed.txt)\n");
      
      //---------------
      // Invoke the Diff action to see if the built URLs are O.K.
      //---------------
      HistoryViewContextualMenuPresenter menuPresenter = 
          new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
      JPopupMenu jPopupMenu = new JPopupMenu();
      
      StagingResourcesTableModel affectedFilesModel = (StagingResourcesTableModel) historyPanel.affectedFilesTable.getModel();
      FileStatus fileStatus = affectedFilesModel.getFilesStatuses().get(0);
      
      CommitCharacteristics cc = model.getAllCommits().get(0);
      menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
      
      MenuElement[] subElements = jPopupMenu.getSubElements();
      
      List<Action> actions = Arrays.asList(subElements).stream()
          .map(t -> ((JMenuItem) t).getAction())
          .filter(t -> ((String) t.getValue(Action.NAME)).startsWith("Compare_file_with_previous_"))
          .collect(Collectors.toList());

      assertFalse("Unable to find the 'Compare with previous version' action.", actions.isEmpty());
      
      Action action = actions.get(0);
      
      action.actionPerformed(null);
      
      assertEquals("Unexpected number of URLs intercepted in the comparison support:" + urls2compare.toString(), 2, urls2compare.size());

      URL left = urls2compare.get(0);
      URL right = urls2compare.get(1);
      
      assertEquals("git://" + model.getAllCommits().get(0).getCommitId() + "/file_renamed.txt", left.toString());
      assertEquals("git://" + model.getAllCommits().get(1).getCommitId() + "/file.txt", right.toString());
  
    } finally {
      GitAccess.getInstance().closeRepo();
  
    }
  
  }
}
