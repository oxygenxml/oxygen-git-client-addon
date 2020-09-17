package com.oxygenxml.git.view.branches;

public interface BranchManagementViewPresenter {

  /**
   * Presents the branch management options.
   */
  void showGitBranchManager();
  
  /**
   * @return <code>true</code> if the branch management view is showing.
   */
  boolean isBranchManagementShowing();
}
