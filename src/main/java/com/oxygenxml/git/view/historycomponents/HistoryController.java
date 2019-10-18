package com.oxygenxml.git.view.historycomponents;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * An interface to the history support.
 */
public interface HistoryController {

  /**
   * Present the history for the entire repository in the dedicated view.
   */
  void showRepositoryHistory();
  
  /**
   * Present a resource's history in the dedicated view.
   * 
   * @param path The resource for which to present the history.
   */
  void showResourceHistory(String path);

  /**
   * Show commit.
   * 
   * @param filePath        Path of the file, relative to the working copy.
   * @param activeRevCommit The commit to select in the view.
   */
  void showCommit(String filePath, RevCommit activeRevCommit);
}
