package com.oxygenxml.git.view.event;

/**
 * Information about an event that is related to a branch.
 */
public class BranchGitEventInfo extends GitEventInfo {

  /**
   * The branch related to this event.
   */
  private String branch;
  
  /**
   * Constructor.
   * 
   * @param gitOp  Branch-related Git operation.
   * @param branch The branch (its name).
   */
  public BranchGitEventInfo(GitOperation gitOp, String branch) {
    super(gitOp);
    this.branch = branch;
  }
  
  /**
   * @return the name of the branch related to this event.
   */
  public String getBranch() {
    return branch;
  }
  
  @Override
  public String toString() {
    return "BranchGitEventInfo [Operation: " + gitOp + ", branch: " + branch + "].";
  }

}
