package com.oxygenxml.git.view.event;

import java.io.File;

/**
 * Information about a Git event that affected a working copy.
 */
public class WorkingCopyGitEventInfo extends GitEventInfo {

  /**
   * Working copy.
   */
  private File workingCopy;

  /**
   * Constructor.
   * 
   * @param gitOp Operation.
   * @param workingCopy  Working copy.
   */
  public WorkingCopyGitEventInfo(GitOperation gitOp, File workingCopy) {
    super(gitOp);
    this.workingCopy = workingCopy;
  }
  
  /**
   * @return the working copy directory.
   */
  public File getWorkingCopy() {
    return workingCopy;
  }
  
  @Override
  public String toString() {
    return "WorkinCopyGitEventInfo [Operation: " + gitOp + ", working-copy: " + workingCopy + "].";
  }

}
