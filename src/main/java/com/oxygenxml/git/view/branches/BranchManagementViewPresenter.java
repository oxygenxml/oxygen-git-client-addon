package com.oxygenxml.git.view.branches;

public interface BranchManagementViewPresenter {

  /**
   * Presents the branch management options.
   */
  void showBranchManagement();
  
  /**
   * @return <code>true</code> if the branch management view is showing.
   */
  boolean isBranchManagementShowing();
}
