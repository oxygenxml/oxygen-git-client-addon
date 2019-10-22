package com.oxygenxml.git.view.event;

/**
 * The state of a Git command, such as "started", "ended", etc.
 */
public enum GitCommandState {
  /**
   * Command started.
   */
  STARTED,
  /**
   * Command successfully ended.
   */
  SUCCESSFULLY_ENDED,
  /**
   * Command failed.
   */
  FAILED;
}
