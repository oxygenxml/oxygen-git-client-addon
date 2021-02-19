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
   * <code>true</code> if the WC is a submodule.
   */
  private boolean isWcSubmodule;

  /**
   * Constructor.
   * 
   * @param gitOp        Operation.
   * @param workingCopy  Working copy.
   */
  public WorkingCopyGitEventInfo(GitOperation gitOp, File workingCopy) {
    this(gitOp, workingCopy, false);
  }
  
  /**
   * Constructor.
   * 
   * @param gitOp       Operation.
   * @param workingCopy Working copy.
   * @param isSubmodule <code>true</code> if the WC is a submodule.
   */
  public WorkingCopyGitEventInfo(GitOperation gitOp, File workingCopy, boolean isSubmodule) {
    super(gitOp);
    this.workingCopy = workingCopy;
    this.isWcSubmodule = isSubmodule;
  }
  
  /**
   * @return the working copy directory.
   */
  public File getWorkingCopy() {
    return workingCopy;
  }
  
  /**
   * @return <code>true</code> if the WC is a submodule.
   */
  public boolean isWorkingCopySubmodule() {
    return isWcSubmodule;
  }
  
  @Override
  public String toString() {
    return "WorkinCopyGitEventInfo [Operation: " + gitOp + ", working-copy: " + workingCopy + "].";
  }

}
