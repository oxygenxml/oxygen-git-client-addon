package com.oxygenxml.git.view.event;

/**
 * Git event information.
 */
public class GitEventInfo {
	
	/**
	 * The Git operation.
	 */
	protected GitOperation gitOp;
	
  /**
   * Object representing a state change.
   * 
   * @param gitOp The Git operation.
   */
  public GitEventInfo(GitOperation gitOp) {
    this.gitOp = gitOp;
  }

  /**
   * @return the operation.
   */
	public GitOperation getGitOperation() {
		return gitOp;
	}

	@Override
	public String toString() {
	  return "GitEventInfo [Operation: " + gitOp + "].";
	}
	
}