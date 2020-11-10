package com.oxygenxml.git.view.event;

/**
 * Git commands: stage, unstage, discard, commit, resolve conflict, etc.
 */
public enum GitOperation {
  /**
   * Stage the given files.
   */
  STAGE,
  /**
   * Remove the files from the INDEX.
   */
  UNSTAGE,
  /**
   * Discard changes.
   */
  DISCARD,
  /**
   * Conflict resolution. Resolve using mine.
   */
  RESOLVE_USING_MINE,
  /**
   * Conflict resolution. Resolve using theirs.
   */
  RESOLVE_USING_THEIRS,
  /**
   * Commit the given resources.
   */
  COMMIT,
  /**
   * Reset the branch to a given commit.
   */
  RESET_TO_COMMIT,
  /**
   * Restart the merge process.
   */
  MERGE_RESTART,
  /**
   * Abort merge.
   */
  ABORT_MERGE,
  /**
   * Abort rebase.
   */
  ABORT_REBASE,
  /**
   * Continue rebase
   */
  CONTINUE_REBASE,
  /**
   * Create branch.
   */
  CREATE_BRANCH,
  /**
   * Checkout.
   */
  CHECKOUT,
  /**
   * Delete branch.
   */
  DELETE_BRANCH,
  /**
   * Open working copy.
   */
  OPEN_WORKING_COPY
}