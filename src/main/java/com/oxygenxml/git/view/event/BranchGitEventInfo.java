package com.oxygenxml.git.view.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Information about an event that is related to a branch.
 */
public class BranchGitEventInfo extends GitEventInfo {

  /**
   * The branch related to this event.
   */
  private Collection<String> branches;
  
  /**
   * Constructor.
   * 
   * @param gitOp  Branch-related Git operation.
   * @param branch The branch (its name).
   */
  public BranchGitEventInfo(GitOperation gitOp, String branch) {
    super(gitOp);
    this.branches = Arrays.asList(branch);
  }
  
  /**
   * Constructor.
   * 
   * @param gitOp    Branch-related Git operation.
   * @param branches The branches.
   */
  public BranchGitEventInfo(GitOperation gitOp, Collection<String> branches) {
    super(gitOp);
    this.branches = new ArrayList<>(branches);
  }
  
  /**
   * @return the name of the branch related to this event.
   */
  public String getBranches() {
    return branches.toString();
  }
  
  @Override
  public String toString() {
    return "BranchGitEventInfo [Operation: " + gitOp + ", branches: " + branches + "].";
  }

}
