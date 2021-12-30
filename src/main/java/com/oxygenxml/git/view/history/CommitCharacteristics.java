package com.oxygenxml.git.view.history;

import java.util.Date;
import java.util.List;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revplot.PlotCommit;

import com.oxygenxml.git.service.RevCommitUtil;
import com.oxygenxml.git.service.RevCommitUtilBase;
import com.oxygenxml.git.view.history.graph.VisualCommitsList;

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
	private final String commitMessage;

	/**
	 * The commit date.
	 */
	private final Date date;

	/**
	 * The commit author with its email.
	 */
	private final String author;

	/**
	 * The abbreviated commitId.
	 */
	private final String commitAbbreviatedId;

	/**
	 * The long version of the commitId.
	 */
	private final String commitId;

	/**
	 * The committer name.
	 */
	private final String committer;

	/**
	 * The abbreviated commitId of the parent.
	 */
	private final List<String> parentCommitId;
	
	/**
	 * The plot for current commit. 
	 */
	private final PlotCommit<VisualCommitsList.VisualLane> plotCommit;
	
	
	/**
	 * Constructor.
	 * 
	 * @param plotCommit The plot commit from which the details are extracted.
	 */
	public CommitCharacteristics(PlotCommit<VisualCommitsList.VisualLane> plotCommit) {
		commitMessage = plotCommit.getFullMessage();
	    PersonIdent authorIdent = plotCommit.getAuthorIdent();
	    author = authorIdent.getName() + " <" + authorIdent.getEmailAddress() + ">";
	    date = authorIdent.getWhen();
	    commitAbbreviatedId = plotCommit.getId().abbreviate(RevCommitUtilBase.ABBREVIATED_COMMIT_LENGTH).name();
	    commitId = plotCommit.getId().getName();
	    PersonIdent committerIdent = plotCommit.getCommitterIdent();
	    committer = committerIdent.getName();
	    parentCommitId = RevCommitUtil.getParentsId(plotCommit);
        this.plotCommit = plotCommit;
	}

	/**
	 * Construct the CommitCharacteristics.
	 * 
	 * @param commitMessage       The commit message
	 * @param authorDate          The date
	 * @param author              The commit author
	 * @param commitAbbreviatedId The abbreviated commit id
	 * @param commitId            The commit id
	 * @param committer           The committer
	 * @param parentCommitId      The parent commit id
	 */
	public CommitCharacteristics(String commitMessage, Date authorDate, String author,
			String commitAbbreviatedId, String commitId, String committer, List<String> parentCommitId) {

		this.commitMessage = commitMessage;
		this.date = authorDate;
		this.author = author;
		this.commitAbbreviatedId = commitAbbreviatedId;
		this.commitId = commitId;
		this.committer = committer;
		this.parentCommitId = parentCommitId;
		this.plotCommit = null;
	}

	@Override
	public String toString() {
		return "[ " + commitMessage + " , " + date + " , " + author + " , " + commitAbbreviatedId + " , " + commitId + " , "
				+ committer + " , " + parentCommitId + " ]";

	}

	
	/**
	 * @return The message of commit.
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	
	/**
	 * @return The commit date.
	 */
	public Date getDate() {
		return date;
	}

	
	/**
	 * @return The commit's author.
	 */
	public String getAuthor() {
		return author;
	}

	
	/**
	 * @return The abbreviated commit id.
	 */
	public String getCommitAbbreviatedId() {
		return commitAbbreviatedId;
	}

	/**
	 * @return The commit id.
	 */
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
	

	/**
	 * @return List with parents id.
	 */
	public List<String> getParentCommitId() {
		return parentCommitId;
	}
	
	
	/**
	 * Get the correspondent CommitCharacterstics element index from history table.
	 * 
	 * @param commits    The CommitCharactersitics list.
	 * @param commitId   The CommitId
	 * 
	 * @return the specific element index in the table. Returns <code>-1</code> if no match.
	 */
	public static int getCommitTableIndex(List<CommitCharacteristics> commits, String commitId) {
		int toReturn = -1;
		for (int i = 0 ; i < commits.size(); i++) {
			if (commits.get(i).getCommitAbbreviatedId().equals(commitId)) {
				toReturn = i;
				break;
			}
		}
		
		return toReturn;
	}

	
	/**
	 *
	 * @return The plot commit.
	 */ 
	public PlotCommit<VisualCommitsList.VisualLane> getPlotCommit() {
		return plotCommit;
	}
	
	
	
}
