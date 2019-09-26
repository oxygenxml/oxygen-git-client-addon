package com.oxygenxml.git.view.historycomponents;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Constructor;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * Presents the commits for a given resource. 
 */
public class HistoryPanel extends JPanel {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class);
  /**
   * Table view that presents the commits.
   */
  private JTable historyTable;
  private JEditorPane commitDescriptionPane;
  /**
   * The label that shows the resource for which we present the history.
   */
  private JLabel showCurrentRepoLabel;
  
  public HistoryPanel() {
    setLayout(new BorderLayout());

    historyTable = createTable();

    JScrollPane tableScrollPane = new JScrollPane(historyTable);
    historyTable.setFillsViewportHeight(true);

    // set Commit Description Pane with HTML content and hyperlink.
    commitDescriptionPane = new JEditorPane();
    commitDescriptionPane.setContentType("text/html");

    historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    commitDescriptionPane.setEditable(false);

    // set minimum width for commit message column
    TableColumnModel columnModel = historyTable.getColumnModel();
    columnModel.addColumn(new TableColumn(0));
    columnModel.getColumn(0).setMinWidth(400);

    JScrollPane commitDescriptionScrollPane = new JScrollPane(commitDescriptionPane);
    JScrollPane fileHierarchyScrollPane = new JScrollPane(new JEditorPane());

    Dimension minimumSize = new Dimension(500, 150);
    commitDescriptionScrollPane.setPreferredSize(minimumSize);
    fileHierarchyScrollPane.setPreferredSize(minimumSize);

    JSplitPane infoBoxesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commitDescriptionScrollPane,
        fileHierarchyScrollPane);
    infoBoxesSplitPane.setDividerLocation(0.5);
    infoBoxesSplitPane.setOneTouchExpandable(true);

    showCurrentRepoLabel = new JLabel();
    add(showCurrentRepoLabel, BorderLayout.NORTH);

    JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, infoBoxesSplitPane);
    
    //Customize the split pane.
    centerSplitPane.setBorder(BorderFactory.createEmptyBorder());
    centerSplitPane.setContinuousLayout(true);
    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        centerSplitPane.setDividerLocation(0.7);
        
        removeComponentListener(this);
      }
    });

    centerSplitPane.setResizeWeight(0.6);
    centerSplitPane.setDividerSize(3);
    
    add(centerSplitPane, BorderLayout.CENTER);
  }

  /**
   * Tries to use Oxygen's API to create a table.
   * 
   * @return An Oxygen's API table or a generic one if we run into an old Oxygen.
   */
  private JTable createTable() {
    JTable table = null;
    try {
      Class tableClass = Class.forName("ro.sync.exml.workspace.api.standalone.ui.Table");
      Constructor tableConstructor = tableClass.getConstructor();
      table = (JTable) tableConstructor.newInstance();
    } catch (Exception e) {
      // Running in an Oxygen version that lacks this API.
      table = new JTable();
    }
    
    return table;
  }

  /**
   * Shows the commit history for the entire repository.
   */
  public void showRepositoryHistory() {
    GitAccess gitAccess = GitAccess.getInstance();
    try {
      showCurrentRepoLabel.setText(
          Translator.getInstance().getTranslation(Tags.SHOWING_HISTORY_FOR) + " " + gitAccess.getRepository().getDirectory().toString());
      showCurrentRepoLabel.setBorder(BorderFactory.createEmptyBorder(0,2,5,0));
      
      historyTable.setDefaultRenderer(CommitCharacteristics.class, new HistoryTableRenderer(gitAccess, gitAccess.getRepository()));

      List<CommitCharacteristics> commitCharacteristicsVector = gitAccess.getCommitsCharacteristics();

      historyTable.setModel(new HistoryCommitTableModel(commitCharacteristicsVector));
      historyTable.getSelectionModel().addListSelectionListener(
          new RowHistoryTableSelectionListener(historyTable, commitDescriptionPane, commitCharacteristicsVector));

      commitDescriptionPane
      .addHyperlinkListener(new HistoryHyperlinkListener(historyTable, commitCharacteristicsVector));

      // preliminary select the first row in the historyTable
      historyTable.setRowSelectionInterval(0, 0);
    } catch (NoRepositorySelected e) {
      LOGGER.debug(e, e);
    }
  }

  /**
   *  Shows the commit history for the given file.
   *  
   * @param filePath Path of the file, relative to the working copy.
   * @param activeRevCommit The commit to select in the view.
   */
  public void showCommit(String filePath, RevCommit activeRevCommit) {
    // TODO Present revisions just for the given resource.
    if (activeRevCommit != null) {
      HistoryCommitTableModel model =  (HistoryCommitTableModel) historyTable.getModel();
      List<CommitCharacteristics> commitVector = model.getCommitVector();
      for (int i = 0; i < commitVector.size(); i++) {
        CommitCharacteristics commitCharacteristics = commitVector.get(i);

        if (activeRevCommit.getId().getName().equals(commitCharacteristics.getCommitId())) {
          final int sel = i;
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              historyTable.scrollRectToVisible(historyTable.getCellRect(sel, 0, true));
              historyTable.getSelectionModel().setSelectionInterval(sel, sel);
            }
          });
          break;
        }
      }
    }
  }
}
