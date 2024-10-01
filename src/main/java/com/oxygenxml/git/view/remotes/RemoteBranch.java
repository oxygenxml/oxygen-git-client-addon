package com.oxygenxml.git.view.remotes;

import org.eclipse.jgit.lib.Repository;

import lombok.Getter;

/**
 * Used to store information about a remote branch.
 * 
 * @author alex_smarandache
 *
 */
@Getter
public class RemoteBranch {
  
  /**
   * Constant when no remote or repo are selected.
   */
  private static final String NONE = "<none>";
  
  /**
   * The remote from config.
   */
  private final String remote;
  
  /**
   * A branch from current remote.
   */
  private final String branchFullName;

  /**
   * <code>true</code> if this item represents the current branch.
   */
  private boolean isCurrentBranch = false;
  
  /**
   * Constructor.
   * 
   * @param remote
   * @param branchFullName
   */
  public RemoteBranch(final String remote, final String branchFullName) {
      this.remote = remote;
      this.branchFullName = branchFullName;
  }
  
  /**
   * @param isCurrentBranch <code>true</code> if this item represents the current branch.
   */
  public void setIsCurrentBranch(boolean isCurrentBranch) {
      this.isCurrentBranch = isCurrentBranch;
  }
  
  /**
   * @return <code>true</code> if the remote or branch are undefined.
   */
  public boolean isUndefined() {
      return remote == null || branchFullName == null;
  }

  @Override
  public String toString() {
      String branchToString = NONE;
      if(!isUndefined()) {
        String branchShortName = branchFullName != null ? Repository.shortenRefName(branchFullName) : null;
        branchToString = remote + "/" + branchShortName;
      } 
      
      return branchToString;
  }

}
