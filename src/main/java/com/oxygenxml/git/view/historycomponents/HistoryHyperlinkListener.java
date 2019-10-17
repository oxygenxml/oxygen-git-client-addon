package com.oxygenxml.git.view.historycomponents;

import java.util.List;

import javax.swing.JTable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * When the user clicks on a revision, it selects that revision in the table.
 */
public class HistoryHyperlinkListener implements HyperlinkListener {

	/**
	 * Table for showing commit history.
	 */
	private JTable historyTable;
	
	/**
	 * The Vector with CommitCharcteristics.
	 */
	private List<CommitCharacteristics> commitCharacteristicsVector;

	/**
	 * Construct HyperlinkListener parent commit id shown in commitDescriptionPane.
	 * 
	 * @param historyTable                The historyTable
	 * @param commitCharacteristicsVector The Vector with commitCharacterstics.
	 */
	public HistoryHyperlinkListener(JTable historyTable, List<CommitCharacteristics> commitCharacteristicsVector) {
		this.historyTable = historyTable;
		this.commitCharacteristicsVector = commitCharacteristicsVector;
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent event) {
		// select the row of parent commit in history Table
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			String query = event.getURL().getQuery();
			int parentStringIdx = query.indexOf("=") + 1;
			String parentCommitId = query.substring(parentStringIdx);
			int parentTableIndex = CommitCharacteristics.getCommitTableIndex(commitCharacteristicsVector, parentCommitId);
			if (parentTableIndex != -1) {
				historyTable.getSelectionModel().setSelectionInterval(parentTableIndex, parentTableIndex);
			}
		}

	}

}
