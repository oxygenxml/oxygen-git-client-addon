package com.oxygenxml.git.view.staging;

import java.awt.Dimension;
import java.io.File;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.view.staging.ChangesPanel.ResourcesViewMode;

/**
 * Test cases for the Truncate filepaths in Git Staging 
 * 
 * @author gabriel_nedianu
 */
public class FlatView11Test extends FlatViewTestBase {

  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger logger = LogManager.getLogger(FlatView9Test.class.getName());

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    stagingPanel.getUnstagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
    stagingPanel.getStagedChangesPanel().setResourcesViewMode(ResourcesViewMode.FLAT_VIEW);
  }

  /**
   * <p>
   * <b>Description:</b> Truncate filepaths in Git Staging (list view mode)
   * </p>
   * <p>
   * <b>Bug ID:</b> EXM-48510
   * </p>
   *
   * @throws Exception
   */
  @Test
  public void testAmendCommitThatWasPushed_changeFileContent() throws Exception {
    // Create repositories
    String localTestRepository = "target/test-resources/testAmendCommitThatWasPushed_1_local";
    String unstagedFile = localTestRepository +"/test1/test2/test3/test4/test5/test6/test7/test8/test9/test10/test11/test12/test13/test14/test15";
    Repository localRepo = createRepository(localTestRepository);

    // Create a new file
    new File(localTestRepository).mkdirs();
    new File(unstagedFile).mkdirs();

    File file = createNewFile(unstagedFile, "test.txt", "content");

    String expectedPath = "test1/.../test9/test10/test11/test12/test13/test14/test15/test.txt";
    JTable filesTable = stagingPanel.getUnstagedChangesPanel().getFilesTable();
    DefaultTableCellRenderer tableCellRenderer = (DefaultTableCellRenderer) filesTable.getCellRenderer(0, 1)
        .getTableCellRendererComponent(filesTable, filesTable.getModel().getValueAt(0, 1), false, false, 0, 1);
    String actualPath = tableCellRenderer.getText();
    assertEquals(expectedPath, actualPath);

    mainFrame.setSize(new Dimension(320, 400));
    expectedPath = "test1/.../test11/test12/test13/test14/test15/test.txt";
    sleep(100);
    String actualPath2 = tableCellRenderer.getText();
    assertEquals(expectedPath, actualPath2);

    mainFrame.setSize(new Dimension(80, 400));
    expectedPath = "test1/.../test.txt";
    sleep(100);
    String actualPath3 = tableCellRenderer.getText();
    assertEquals(expectedPath, actualPath3);

  }

}
