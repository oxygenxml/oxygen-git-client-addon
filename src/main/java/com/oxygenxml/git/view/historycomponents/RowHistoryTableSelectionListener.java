package com.oxygenxml.git.view.historycomponents;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.view.StagingResourcesTableModel;

public class RowHistoryTableSelectionListener implements ListSelectionListener {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RowHistoryTableSelectionListener.class);

	/**
	 * The fields of commitDescription EditorPane.
	 */
	private final static String COMMIT = "Commit";
	private final static String PARENTS = "Parents";
	private final static String AUTHOR = "Author";
	private final static String DATE = "Date";
	private final static String COMMITTER = "Comitter";
	
	/**
	 * Fake commit URL to search for parents when using hyperlink.
	 */
	private static final String PARENT_COMMIT_URL = "http://gitplugin.com/parent/commit?id=";
	
	/**
	 * Table for Commit History.
	 */
	private JTable historyTable;

	/**
	 * The CommitDescription EditorPane.
	 */
	private JEditorPane commitDescriptionPane;

	/**
	 * The Vector with CommitCharcteristics.
	 */
	private List<CommitCharacteristics> commitCharacteristicsVector;

	/*
	 * Coalescing for selecting the row in HistoryTable.
	 */
	private static final int TIMER_DELAY = 500;
	private ActionListener rowTableTimerListener = new TableTimerListener();
	private Timer updateTableTimer = new Timer(TIMER_DELAY, rowTableTimerListener);
  private JTable changesTable;

	/**
	 * Construct the SelectionListener for HistoryTable.
	 * 
	 * @param historyTable                The historyTable
	 * @param commitDescriptionPane       The commitDescriptionPane
	 * @param commitCharacteristicsVector The commitCharacteristicsVector
	 * @param changesTable                The table that presents the files chaged in a commit.
	 */
	public RowHistoryTableSelectionListener(
	    JTable historyTable, 
	    JEditorPane commitDescriptionPane,
			List<CommitCharacteristics> commitCharacteristicsVector, 
			JTable changesTable) {
		this.changesTable = changesTable;
    this.updateTableTimer.setRepeats(false);
		this.historyTable = historyTable;
		this.commitDescriptionPane = commitDescriptionPane;
		this.commitCharacteristicsVector = commitCharacteristicsVector;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		updateTableTimer.restart();
	}

	/**
	 * Timer Listener when selecting a row in HistoryTable.
	 * 
	 * @author Alexandra_Dinisor
	 *
	 */
	private class TableTimerListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			setCommitDescription();
		}
	}
	
	/**
	 * Set the commitDescription in a non-editable EditorPane, including: CommitID,
	 * Parents IDs with hyperlink, Author, Committer and Commit Message.
	 * 
	 */
	private void setCommitDescription() {
		CommitCharacteristics commitCharacteristics = commitCharacteristicsVector.get(historyTable.getSelectedRow());
		String commitDescription = "";
		// Case for already committed changes.
		if (commitCharacteristics.getCommitter() != null) {
			commitDescription = "<html><b>" + COMMIT + "</b>: " + commitCharacteristics.getCommitId() + " ["
					+ commitCharacteristics.getCommitAbbreviatedId() + "]";

			// Add all parent commit IDs to the text
			if (commitCharacteristics.getParentCommitId() != null) {
				commitDescription += "<br> <b>" + PARENTS + "</b>: ";
				int parentSize = commitCharacteristics.getParentCommitId().size();

				for (int j = 0; j < parentSize - 1; j++) {
					commitDescription += "<a href=\"" + PARENT_COMMIT_URL + commitCharacteristics.getParentCommitId().get(j)
							+ "\">" + commitCharacteristics.getParentCommitId().get(j) + "</a> , ";
				}
				commitDescription += "<a href=\" " + PARENT_COMMIT_URL
						+ commitCharacteristics.getParentCommitId().get(parentSize - 1) + "\">"
						+ commitCharacteristics.getParentCommitId().get(parentSize - 1) + "</a> ";
			}
			commitDescription += "<br> <b>" + AUTHOR + "</b>: " + commitCharacteristics.getAuthor() + "<br>" 
					+ "<b>" + DATE + "</b>: " + commitCharacteristics.getDate() + "<br>" 
					+ "<b>" + COMMITTER + "</b>: " + commitCharacteristics.getCommitter() + "<br><br>"
					+ commitCharacteristics.getCommitMessage() + "</html>";
		}
		commitDescriptionPane.setText(commitDescription);
		commitDescriptionPane.setCaretPosition(0);

		StagingResourcesTableModel dataModel = (StagingResourcesTableModel) changesTable.getModel();
		if (GitAccess.UNCOMMITED_CHANGES != commitCharacteristics) {
		  try {
		    Repository repository = GitAccess.getInstance().getRepository();
		    ObjectId head = repository.resolve(commitCharacteristics.getCommitId());
		    ObjectId old = repository.resolve(commitCharacteristics.getParentCommitId().get(0));

		    try (RevWalk rw = new RevWalk(GitAccess.getInstance().getRepository())) {
		      RevCommit commit = rw.parseCommit(head);
		      RevCommit oldC = rw.parseCommit(old);
		      List<FileStatus> changes = RevCommitUtil.getChanges(repository, commit, oldC);

		      dataModel.setFilesStatus(changes);
		    }
		  } catch (GitAPIException | RevisionSyntaxException | IOException | NoRepositorySelected e) {
		    logger.error(e, e);
		  }
		} else {
		  // TODO Actually, the uncommitted changes are the files changed. A diff between head and working copy.
		  dataModel.setFilesStatus(Collections.emptyList());
		}
	}

}
