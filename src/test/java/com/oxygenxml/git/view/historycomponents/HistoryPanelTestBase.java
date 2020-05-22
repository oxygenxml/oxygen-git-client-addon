package com.oxygenxml.git.view.historycomponents;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.MenuElement;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.powermock.api.mockito.PowerMockito;

import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.StagingResourcesTableModel;
import com.oxygenxml.git.view.event.GitController;

/**
 * UI level tests for history.
 *  
 * @author alex_jitianu
 */
@Ignore
public class HistoryPanelTestBase extends GitTestBase { // NOSONAR squid:S2187

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
  
  /**
   * Computes the contextual action over the given file.
   * Returns the "Compare with each other" action available when you select 2 revisions.
   * 
   * @param fileStatus File information for which we want the action.
   * @param cc Revision information.
   * 
   * @return The action. If not found, an assert will fail.
   * 
   * @throws IOException If it fails.
   * @throws GitAPIException If it fails.
   */
  protected Action getCompareWithPreviousAction(
      FileStatus fileStatus, 
      CommitCharacteristics cc) throws IOException, GitAPIException {
    HistoryViewContextualMenuPresenter menuPresenter = 
        new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
    JPopupMenu jPopupMenu = new JPopupMenu();
    menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
    
    MenuElement[] subElements = jPopupMenu.getSubElements();
    
    List<Action> actions = Arrays.asList(subElements).stream()
        .map(t -> ((JMenuItem) t).getAction())
        .filter(t -> ((String) t.getValue(Action.NAME)).startsWith("Compare_file_with_previous_"))
        .collect(Collectors.toList());

    assertFalse("Unable to find the 'Compare with previous version' action.", actions.isEmpty());
    
    return actions.get(0);
  }
  
  /**
   * Computes the contextual action over the given file.
   * Returns the "Compare with each other" action available when you select 2 revisions.
   * 
   * @param fileStatus File informations for which we want the action.
   * @param cc Revision information.
   * 
   * @return The action. If not found, an assert will fail.
   * 
   * @throws IOException If it fails.
   * @throws GitAPIException If it fails.
   */
  protected Action getCompareWithEachOther(
      FileStatus fileStatus, 
      CommitCharacteristics... cc) throws IOException, GitAPIException {
    HistoryViewContextualMenuPresenter menuPresenter = 
        new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
    JPopupMenu jPopupMenu = new JPopupMenu();
    menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
    
    MenuElement[] subElements = jPopupMenu.getSubElements();
    
    List<Action> actions = Arrays.asList(subElements).stream()
        .map(t -> ((JMenuItem) t).getAction())
        .filter(t -> ((String) t.getValue(Action.NAME)).startsWith("Compare_with_each_other"))
        .collect(Collectors.toList());

    assertFalse("Unable to find the 'Compare_with_each_other' action.", actions.isEmpty());
    
    return actions.get(0);
  }
  
  /**
   * Computes the contextual action over the given file.
   * Returns the "Open file" action available when you select 2 revisions.
   * 
   * @param fileStatus File information for which we want the action.
   * @param cc Revision information.
   * 
   * @return The action. If not found, an assert will fail.
   * 
   * @throws IOException If it fails.
   * @throws GitAPIException If it fails.
   */
  protected Action getOpenFileAction(
      FileStatus fileStatus, 
      CommitCharacteristics... cc) throws IOException, GitAPIException {
    HistoryViewContextualMenuPresenter menuPresenter = 
        new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
    JPopupMenu jPopupMenu = new JPopupMenu();
    menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
    
    MenuElement[] subElements = jPopupMenu.getSubElements();
    
    List<Action> actions = Arrays.asList(subElements).stream()
        .map(t -> ((JMenuItem) t).getAction())
        .filter(t -> ((String) t.getValue(Action.NAME)).equals("Open_file"))
        .collect(Collectors.toList());

    return actions.get(0);
  }
  
  /**
   * Gets all the actions from the contextual menu for the given file.
   * 
   * @param fileStatus  File.
   * @param cc Revision information.
   * 
   * @return All the actions that will be put in the contextual menu for the received resource.
   * 
   * @throws IOException
   * @throws GitAPIException
   */
  protected List<Action> getAllActions(
      FileStatus fileStatus, 
      CommitCharacteristics cc) throws IOException, GitAPIException {
    HistoryViewContextualMenuPresenter menuPresenter = 
        new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
    JPopupMenu jPopupMenu = new JPopupMenu();
    menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
    
    MenuElement[] subElements = jPopupMenu.getSubElements();
    
    List<Action> actions = Arrays.asList(subElements).stream()
        .map(t -> ((JMenuItem) t).getAction())
        .collect(Collectors.toList());

    return actions;
  }
  
  /**
   * Computes the contextual action over the given file.
   * Returns the "Compare_file_with_working_tree_version" action available when you select 2 revisions.
   * 
   * @param fileStatus File information for which we want the action.
   * @param cc Revision information.
   * 
   * @return The action. If not found, an assert will fail.
   * 
   * @throws IOException If it fails.
   * @throws GitAPIException If it fails.
   */
  protected Action getCompareWithWCAction(
      FileStatus fileStatus, 
      CommitCharacteristics cc) throws IOException, GitAPIException {
    HistoryViewContextualMenuPresenter menuPresenter = 
        new HistoryViewContextualMenuPresenter(PowerMockito.mock(GitController.class));
    JPopupMenu jPopupMenu = new JPopupMenu();
    menuPresenter.populateContextualActions(jPopupMenu, fileStatus.getFileLocation(), cc);
    
    MenuElement[] subElements = jPopupMenu.getSubElements();
    
    List<Action> actions = Arrays.asList(subElements).stream()
        .map(t -> ((JMenuItem) t).getAction())
        .filter(t -> ((String) t.getValue(Action.NAME)).startsWith("Compare_file_with_working_tree_version"))
        .collect(Collectors.toList());

    assertFalse("Unable to find the 'Compare_with_working_tree_version' action.", actions.isEmpty());
    
    return actions.get(0);
  }
}
