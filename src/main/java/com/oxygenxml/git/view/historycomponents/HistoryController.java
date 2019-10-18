package com.oxygenxml.git.view.historycomponents;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * An interface to the history support.
 */
public interface HistoryController {

  /**
   * Present the repository for the entire repository in the dedicated view.
   */
  void showRepositoryHistory();
  
  /**
   * Present the resource's history into the dedicated view.
   * 
   * @param path The resource for which to present the history.
   */
  void showResourceHistory(String path);

  void showCommit(String filePath, RevCommit activeRevCommit);
}
