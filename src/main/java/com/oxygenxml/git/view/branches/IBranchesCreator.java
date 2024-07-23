package com.oxygenxml.git.view.branches;

/**
 * The branches creator.
 * 
 * @author alex_smarandache
 */
public interface IBranchesCreator {

  /**
   * Create a new repository branch.
   * 
   * @param branchName             The name of the branch to be created.
   * @param shouldCheckoutBranch   <code>true</code> if the branch should checkout branch after its creation. 
   */
  void createBranch(String branchName, boolean shouldCheckoutBranch);
  
}
