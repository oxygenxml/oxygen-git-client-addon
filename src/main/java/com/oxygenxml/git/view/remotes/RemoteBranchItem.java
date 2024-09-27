package com.oxygenxml.git.view.remotes;

import org.eclipse.jgit.lib.Repository;

/**
 * Used to help us to store the remote branch informations.
 * 
 * @author alex_smarandache
 *
 */
public class RemoteBranchItem {
  
  /**
   * Constant when no remote or repo are selected.
   */
  private static final String NONE = "<none>";
  
  /**
   * The remote from config.
   */
  public final String remote;
  
  /**
   * A branch from current remote.
   */
  public final String branch;
  
  /**
   * The branch short name.
   */
  private final String branchShortName;
  
/**
* <code>true</code> if this item represents the first selection.
*/
  private boolean isFirstSelection = false;
  
  
  /**
   * Constructor.
   * 
   * @param remote
   * @param branch
   */
  public RemoteBranchItem(final String remote, final String branch) {
      this.remote = remote;
      this.branch = branch;
      this.branchShortName = branch != null ? Repository.shortenRefName(branch) : null;
  }
  
  
  /**
   * @return <code>true</code> if this item represents the first selection.
   */
  public boolean isFirstSelection() {
      return isFirstSelection;
  }

  /**
   * @param isFirstSelection <code>true</code> if this item represents the first selection.
   */
  public void setFirstSelection(boolean isFirstSelection) {
      this.isFirstSelection = isFirstSelection;
  }
  
  /**
   * @return <code>true</code> if the remote or branch are undefined.
   */
  public boolean isUndefined() {
      return remote == null || branch == null;
  }

  @Override
  public String toString() {
      return isUndefined() ? NONE : remote + "/" + branchShortName;
  }

}
