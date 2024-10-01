package com.oxygenxml.git.view.remotes;

import org.eclipse.jgit.lib.Repository;

/**
 * Used to store information about a remote branch.
 * 
 * @author alex_smarandache
 *
 */
public class RemoteBranch {
  
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
  public final String branchFullName;
  
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
   * @param branchFullName
   */
  public RemoteBranch(final String remote, final String branchFullName) {
      this.remote = remote;
      this.branchFullName = branchFullName;
      this.branchShortName = branchFullName != null ? Repository.shortenRefName(branchFullName) : null;
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
      return remote == null || branchFullName == null;
  }

  @Override
  public String toString() {
      return isUndefined() ? NONE : remote + "/" + branchShortName;
  }

}
