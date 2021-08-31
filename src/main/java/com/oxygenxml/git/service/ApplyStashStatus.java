package com.oxygenxml.git.service;

/**
 * The statuses for stash apply operation.
 * 
 * @author Alex_Smarandache
 *
 */
public enum ApplyStashStatus {
  /**
   * If the stash has been successfully applied.
   */
  SUCCESSFULLY,
  /**
   * If the stash has been successfully applied, but were generated conflicts.
   */
  APPLIED_WITH_GENERATED_CONFLICTS,
  /**
   * If the stash was not applied because there are local changes that would conflict.
   */
  UNCOMMITTED_FILES, 
  /**
   * A conflict caused by a JGit bug, if we apply to an added file and the staged area is not empty.
  */
  BUG_CONFLICT,
  /**
   * If the stash was not applied because there are conflicting files.
   */
  CONFLICTS,
  /**
   * If the stash was not applied for an unknown reason.
   */
  UNKNOWN_CAUSE
}
