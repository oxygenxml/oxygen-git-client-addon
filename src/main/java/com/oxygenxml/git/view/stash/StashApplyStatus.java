package com.oxygenxml.git.view.stash;

/**
 * The statuses for stash apply operation.
 * 
 * @author Alex_Smarandache
 *
 */
public enum StashApplyStatus {
  /**
   * If the stash was successfully applied.
   */
  APPLIED_SUCCESSFULLY,
  /**
   * If the stash was successfully applied, but conflicts were generated.
   */
  APPLIED_SUCCESSFULLY_WITH_CONFLICTS,
  /**
   * If the stash was not applied because of an unknown reason.
   */
  NOT_APPLIED_UNKNOWN_CAUSE,
  
  /**
   * If the "Apply stash" operation cannot be started because there are conflicting local changes.
   */
  CANNOT_START_APPLY_BECAUSE_UNCOMMITTED_FILES,
  /**
   * If the "Apply stash" operation cannot be started because there are staged changes.
   */
  CANNOT_START_BECAUSE_STAGED_FILES,
  /**
   * If the "Apply stash" operation cannot be started because there are conflicts in the WC.
   */
  CANNOT_START_APPLY_BECAUSE_CONFLICTS,

}
