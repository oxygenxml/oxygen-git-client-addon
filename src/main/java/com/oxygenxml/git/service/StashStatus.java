package com.oxygenxml.git.service;

/**
 * The statuses for stash apply operation.
 * 
 * @author Alex_Smarandache
 *
 */
public enum StashStatus {
  /**
   * If the stash has been successfully applied.
   */
  POST_APPLY_SUCCESS,
  /**
   * If the stash has been successfully applied, but were generated conflicts.
   */
  POST_APPLY_SUCCESS_WITH_CONFLICTS,
  /**
   * If the stash was not applied because there are local changes that would conflict.
   */
  PRE_APPLY_UNCOMMITTED_FILES,
  /**
   * If there are staged changes when try to apply a stash.
  */
  PRE_APPLY_STAGED_FILES,
  /**
   * If the stash was not applied because there are conflicting files.
   */
  PRE_APPLY_CONFLICTS,
  /**
   * If the stash was not applied for an unknown reason.
   */
  POST_APPLY_UNKNOWN_CAUSE,
  /**
   * If the stash pass the pre tests and it's ready to be applied.
   */
  PRE_APPLY_READY_TO_APPLY
}
