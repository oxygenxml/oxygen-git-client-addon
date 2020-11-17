package com.oxygenxml.git.view.event;

/**
 * Possible conflict resolutions.
 * 
 */
public enum ConflictResolution {
  /**
   * Conflict resolution. Resolve using mine.
   */
  RESOLVE_USING_MINE,
  /**
   * Conflict resolution. Resolve using theirs.
   */
  RESOLVE_USING_THEIRS,
}
