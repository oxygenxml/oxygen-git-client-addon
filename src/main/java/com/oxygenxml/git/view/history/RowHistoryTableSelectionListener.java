package com.oxygenxml.git.view.history;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.staging.StagingResourcesTableModel;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.util.XMLUtilAccess;

public class RowHistoryTableSelectionListener implements ListSelectionListener {
  
  /**
   * Timer Listener when selecting a row in HistoryTable.
   */
  private class TableTimerListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      setCommitDescription();
    }
    
    /**
     * Set the commit description in a non-editable editor pane, including: CommitID,
     * Parents IDs with hyperlink, Author, Committer and Commit Message.
     */
    @SuppressWarnings("java:S1192")
    private void setCommitDescription() {
      int selectedRow = historyTable.getSelectedRow();
      if (selectedRow != -1) {
        CommitCharacteristics commitCharacteristics = allCommits.get(selectedRow);
        StringBuilder commitDescription = new StringBuilder();
        // Case for already committed changes.
        if (commitCharacteristics.getCommitter() != null) {
          XMLUtilAccess xmlUtilAccess = PluginWorkspaceProvider.getPluginWorkspace().getXMLUtilAccess();
          
          commitDescription.append("<html><b>").append(Translator.getInstance().getTranslation(Tags.COMMIT)).append("</b>: ")
              .append(commitCharacteristics.getCommitId())
              .append(" [").append(commitCharacteristics.getCommitAbbreviatedId()).append("]");

          // Add all parent commit IDs to the text
          if (commitCharacteristics.getParentCommitId() != null) {
            commitDescription.append("<br> <b>").append(Translator.getInstance().getTranslation(Tags.PARENTS)).append("</b>: ");
            int parentSize = commitCharacteristics.getParentCommitId().size();

            boolean isDarkTheme = PluginWorkspaceProvider.getPluginWorkspace().getColorTheme().isDarkTheme();
            for (int j = 0; j < parentSize - 1; j++) {
              commitDescription.append("<a href=\"")
                .append(PARENT_COMMIT_URL)
                .append(commitCharacteristics.getParentCommitId().get(j))
                .append("\"");
              if(isDarkTheme) {
                commitDescription.append("style=\"color: ")
                .append(UIUtil.PARENTS_LINK_COLOR_GRAPHITE_STRING)
                .append("\"");
              }
                
              commitDescription.append(">")
                .append(commitCharacteristics.getParentCommitId().get(j))
                .append("</a> , ");
            }
            commitDescription.append("<a href=\" ")
              .append(PARENT_COMMIT_URL)
              .append(commitCharacteristics.getParentCommitId().get(parentSize - 1))
              .append("\"");
            if(isDarkTheme) {
              commitDescription.append("style=\"color: ")
              .append(UIUtil.PARENTS_LINK_COLOR_GRAPHITE_STRING)
              .append("\"");
            }
            commitDescription.append(">")
              .append(commitCharacteristics.getParentCommitId().get(parentSize - 1))
              .append("</a> ");
          }
          
          commitDescription.append("<br> <b>").append(Translator.getInstance().getTranslation(Tags.AUTHOR))
              .append("</b>: ").append(xmlUtilAccess.escapeTextValue(commitCharacteristics.getAuthor())).append("<br>") 
              .append("<b>").append(Translator.getInstance().getTranslation(Tags.DATE)).append("</b>: ")
              .append(commitCharacteristics.getDate()).append("<br>") 
              .append("<b>").append(Translator.getInstance().getTranslation(Tags.AUTHOR)).append("</b>: ")
              .append(xmlUtilAccess.escapeTextValue(commitCharacteristics.getCommitter())).append("<br><br>")
              .append(xmlUtilAccess.escapeTextValue(commitCharacteristics.getCommitMessage()).replace("\n", "<br>"))
              .append("</html>");
        }
        commitDescriptionPane.setText(commitDescription.toString());
        commitDescriptionPane.setCaretPosition(0);

        updateDataModel(commitCharacteristics);
      }
    }

    /**
     * Update data model.
     * 
     * @param commitCharacteristics Details about the current commit.
     */
    private void updateDataModel(CommitCharacteristics commitCharacteristics) {
      StagingResourcesTableModel dataModel = (StagingResourcesTableModel) changesTable.getModel();
      if (GitAccess.UNCOMMITED_CHANGES != commitCharacteristics) {
        try {
          List<FileStatus> changes = RevCommitUtil.getChangedFiles(commitCharacteristics.getCommitId());
          dataModel.setFilesStatus(changes);
        } catch (GitAPIException | RevisionSyntaxException | IOException e) {
          logger.error(e, e);
        }
      } else {
        dataModel.setFilesStatus(GitAccess.getInstance().getUnstagedFiles());
      }
    }
  }
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RowHistoryTableSelectionListener.class);
	/**
	 * Fake commit URL to search for parents when using hyperlink.
	 */
	private static final String PARENT_COMMIT_URL = "http://gitplugin.com/parent/commit?id=";
	/**
	 * Table for Commit History.
	 */
	private JTable historyTable;
	/**
	 * Panel for commit description (author, date, etc.).
	 */
	private JEditorPane commitDescriptionPane;
	/**
	 * The list of commits and their characteristics.
	 */
	private List<CommitCharacteristics> allCommits;
	/**
	 * Coalescing listener for updating commit related data.
	 */
	private ActionListener descriptionUpdateListener = new TableTimerListener();
	/**
	 * Coalescing support for updating commit related data.
	 */
	private Timer descriptionUpdateTimer;
	/**
	 * Table that presents the resources changed inside a commit.
	 */
  private JTable changesTable;

	/**
	 * Construct the SelectionListener for HistoryTable.
	 * 
	 * @param updateDelay                 Milliseconds. Controls how fast the satellite views are updated after a new revision is selected.
	 * @param historyTable                The historyTable
	 * @param commitDescriptionPane       The commitDescriptionPane
	 * @param commits                     The list of commits and their characteristics.
	 * @param changesTable                The table that presents the files changed in a commit.
	 */
	public RowHistoryTableSelectionListener(
	    int updateDelay,
	    JTable historyTable, 
	    JEditorPane commitDescriptionPane,
			List<CommitCharacteristics> commits, 
			JTable changesTable) {
		this.changesTable = changesTable;
		descriptionUpdateTimer = new Timer(updateDelay, descriptionUpdateListener);
    this.descriptionUpdateTimer.setRepeats(false);
		this.historyTable = historyTable;
		this.commitDescriptionPane = commitDescriptionPane;
		this.allCommits = commits;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
	  if (!e.getValueIsAdjusting()) {
	    if (descriptionUpdateTimer.getDelay() == 0) {
	      // Probably from tests.
	      descriptionUpdateListener.actionPerformed(null);
	    } else {
	      descriptionUpdateTimer.restart();
	    }
	  }
	}

}
