package com.oxygenxml.git.view.event;

public enum GitCommandEvent {
  /**
   * Stage the given files - START.
   */
  STAGE_STARTED,
  /**
   * Stage the given files - END.
   */
  STAGE_ENDED,
  /**
   * Remove the files from the INDEX - START.
   */
  UNSTAGE_STARTED,
  /**
   * Remove the files from the INDEX - END.
   */
  UNSTAGE_ENDED,
  /**
   * Discard changes - START.
   */
  DISCARD_STARTED,
  /**
   * Discard changes - END.
   */
  DISCARD_ENDED,
  /**
   * Conflict resolution. Resolve using mine. - START
   */
  RESOLVE_USING_MINE_STARTED,
  /**
   * Conflict resolution. Resolve using mine. - END
   */
  RESOLVE_USING_MINE_ENDED,
  /**
   * Conflict resolution. Resolve using theirs. - START
   */
  RESOLVE_USING_THEIRS_STARTED,
  /**
   * Conflict resolution. Resolve using theirs. - END
   */
  RESOLVE_USING_THEIRS_ENDED,
  /**
   * Commit the given resources. - START
   */
  COMMIT_STARTED,
  /**
   * Commit the given resources. - END
   */
  COMMIT_ENDED,
  /**
   * Restart the merge process. - START
   */
  MERGE_RESTART_STARTED,
  /**
   * Restart the merge process. - END
   */
  MERGE_RESTART_ENDED,
  /**
   * Abort rebase. - START
   */
  ABORT_REBASE_STARTED,
  /**
   * Abort rebase. - END
   */
  ABORT_REBASE_ENDED,
  /**
   * Continue rebase - START
   */
  CONTINUE_REBASE_STARTED,
  /**
   * Continue rebase - END
   */
  CONTINUE_REBASE_ENDED;
}