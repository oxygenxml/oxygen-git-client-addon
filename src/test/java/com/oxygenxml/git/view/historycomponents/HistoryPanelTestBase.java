package com.oxygenxml.git.view.historycomponents;

import java.util.Date;

import javax.swing.JTable;

import org.junit.Ignore;

import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.GitController;

/**
 * UI level tests for history.
 *  
 * @author alex_jitianu
 */
@Ignore
public class HistoryPanelTestBase extends GitTestBase {

  protected HistoryPanel historyPanel;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    setUpHistoryPanel();
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
  protected void assertAffectedFiles(HistoryPanel historyPanel, String expected) {
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
  protected void selectAndAssertRevision(JTable historyTable, int row, String expected) {
    HistoryCommitTableModel model = (HistoryCommitTableModel) historyTable.getModel();
    historyTable.getSelectionModel().setSelectionInterval(row, row);
    // There is a timer involved.
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {}
    flushAWT();
    CommitCharacteristics selectedObject = (CommitCharacteristics) model.getValueAt(historyTable.getSelectedRow(), 0);
    assertEquals(replaceDate(expected), toString(selectedObject));
  }

  protected String replaceDate(String expected) {
    return expected.replaceAll("\\{date\\}",  DATE_FORMAT.format(new Date()));
  }
}
