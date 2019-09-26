package com.oxygenxml.git.view.event;

/**
 * Pull types: merge (ff, no-ff, ff-only), rebase, unknown.
 */
public enum PullType {
  /**
   * Unknown.
   */
  UKNOWN,
  /**
   * When the merge resolves as a fast-forward, only update the branch pointer,
   * without creating a merge commit. This is the default behavior.
   */
  MERGE_FF,
  /**
   * Create a merge commit even when the merge resolves as a fast-forward.
   * This is the default behaviour when merging an annotated (and possibly signed) tag
   * that is not stored in its natural place in "refs/tags/" hierarchy.
   */
  MERGE_NO_FF,
  /**
   * Refuse to merge and exit with a non-zero status
   * unless the current HEAD is already up to date 
   * or the merge can be resolved as a fast-forward.
   */
  MERGE_FF_ONLY,
  /**
   * Rebase the current branch on top of the upstream branch after fetching.
   */
  REBASE
}
