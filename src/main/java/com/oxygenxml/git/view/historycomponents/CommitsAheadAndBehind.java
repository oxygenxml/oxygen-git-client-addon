package com.oxygenxml.git.view.historycomponents;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Data structure that contains the lists of commits to be pushed to the server ("ahead")
 * and commits to be pulled from the server ("behind").
 */
public class CommitsAheadAndBehind {

  /**
   * The list of commits to be pushed to the server.
   */
  private List<RevCommit> commitsAhead;
  /**
   * The list of commits to be pulled from the server.
   */
  private List<RevCommit> commitsBehind;
  
  /**
   * Constructor.
   * 
   * @param commitsAhead  The list of commits to be pushed to the server.
   * @param commitsBehind The list of commits to be pulled from the server.
   */
  public CommitsAheadAndBehind(List<RevCommit> commitsAhead, List<RevCommit> commitsBehind) {
    this.commitsAhead = commitsAhead;
    this.commitsBehind = commitsBehind;
  }

  /**
   * @return The commits in the local branch that haven't been pushed yet.
   */
  public List<RevCommit> getCommitsAhead() {
    return commitsAhead;
  }
  
  /**
   * @return The commits present in the upstream branch that haven't been pulled yet.
   */
  public List<RevCommit> getCommitsBehind() {
    return commitsBehind;
  }
  
}
