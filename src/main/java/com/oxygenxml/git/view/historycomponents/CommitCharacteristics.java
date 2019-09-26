package com.oxygenxml.git.view.historycomponents;

import java.util.List;
import java.util.Vector;

/**
 * Class for Commit Characteristics shown in historyTable.
 * 
 * @Alexandra_Dinisor
 *
 */
public class CommitCharacteristics {

	/**
	 * The commit message.
	 */
	private String commitMessage;

	/**
	 * The commit date.
	 */
	private String date;

	/**
	 * The commit author with its email.
	 */
	private String author;

	/**
	 * The abbreviated commitId.
	 */
	private String commitAbbreviatedId;

	/**
	 * The long version of the commitId.
	 */
	private String commitId;

	/**
	 * The committer name.
	 */
	private String committer;

	/**
	 * The abbreviated commitId of the parent.
	 */
	private List<String> parentCommitId;

	/**
	 * Construct the CommitCharacteristics.
	 * 
	 * @param graph               The graph representation
	 * @param commitMessage       The commit message
	 * @param date                The date
	 * @param author              The commit author
	 * @param commitAbbreviatedId The abbreviated commit id
	 * @param commitId            The commit id
	 * @param committer           The committer
	 * @param parentCommitId      The parent commit id
	 */
	public CommitCharacteristics(String commitMessage, String date, String author,
			String commitAbbreviatedId, String commitId, String committer, List<String> parentCommitId) {

		this.commitMessage = commitMessage;
		this.date = date;
		this.author = author;
		this.commitAbbreviatedId = commitAbbreviatedId;
		this.commitId = commitId;
		this.committer = committer;
		this.parentCommitId = parentCommitId;
	}

	@Override
	public String toString() {
		return "[ " + commitMessage + " , " + date + " , " + author + " , " + commitAbbreviatedId + " , " + commitId + " , "
				+ committer + " , " + parentCommitId + " ]";

	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public String getDate() {
		return date;
	}

	public String getAuthor() {
		return author;
	}

	public String getCommitAbbreviatedId() {
		return commitAbbreviatedId;
	}

	public String getCommitId() {
		return commitId;
	}

	/**
	 * Getter for committer name.
	 * 
	 * @return the committer name. <code>Null</code> for "uncommitted changes".
	 */
	public String getCommitter() {
		return committer;
	}

	public List<String> getParentCommitId() {
		return parentCommitId;
	}
	
	/**
	 * Get the correspondent CommitCharacterstics element index from history table.
	 * 
	 * @param commitCharacteristicsVector The CommitCharactersitics Vector
	 * @param commitId                    The CommitId
	 * @return the specific element index in the table. Returns <code>-1</code> if no match.
	 */
	public static int getCommitTableIndex(List<CommitCharacteristics> commitCharacteristicsVector, String commitId) {	
		for (int i = 0 ; i < commitCharacteristicsVector.size(); i++) {
			if (commitCharacteristicsVector.get(i).getCommitAbbreviatedId().equals(commitId)) {
				return i;
			}
		}
		return -1;
	}
	
}
