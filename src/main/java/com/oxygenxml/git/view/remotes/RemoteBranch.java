package com.oxygenxml.git.view.remotes;

import org.eclipse.jgit.lib.Repository;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Used to store information about a remote branch.
 * 
 * @author alex_smarandache
 *
 */
@EqualsAndHashCode
@Getter
public class RemoteBranch {
  
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
   * The undefined branch.
   */
  public final static RemoteBranch UNDEFINED_BRANCH = new RemoteBranch(null, null) {
    @Override
    public String toString() {
      return "<none>";
    }
  };
  
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

  @Override
  public String toString() {
      String branchShortName = branchFullName != null ? Repository.shortenRefName(branchFullName) : null;
      return  remote + "/" + branchShortName;
  }

}
