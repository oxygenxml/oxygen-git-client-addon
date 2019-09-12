package com.oxygenxml.git.view.historycomponents;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RowHistoryTableSelectionListener implements ListSelectionListener {

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
	JTable historyTable;

	/**
	 * The CommitDescription EditorPane.
	 */
	JEditorPane commitDescriptionPane;

	/**
	 * The Vector with CommitCharcteristics.
	 */
	Vector<CommitCharacteristics> commitCharacteristicsVector;

	/*
	 * Coalescing for selecting the row in HistoryTable.
	 */
	private static final int TIMER_DELAY = 500;
	private ActionListener rowTableTimerListener = new TableTimerListener();
	private Timer updateTableTimer = new Timer(TIMER_DELAY, rowTableTimerListener);

	/**
	 * Construct the SelectionListener for HistoryTable.
	 * 
	 * @param historyTable                The historyTable
	 * @param commitDescriptionPane       The commitDescriptionPane
	 * @param commitCharacteristicsVector The commitCharacteristicsVector
	 */
	public RowHistoryTableSelectionListener(JTable historyTable, JEditorPane commitDescriptionPane,
			Vector<CommitCharacteristics> commitCharacteristicsVector) {
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

	}

}
