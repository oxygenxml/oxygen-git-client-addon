package com.oxygenxml.git.service.entities;

/**
 * Used in File Status to mark the file in which of the following states it is
 * 
 * @author Beniamin Savu
 *
 */
public enum GitChangeType {
  /**
   * This file is a submodule
   */
  SUBMODULE,

  //-------------------------------------------------
  // Staged files (files that are in the index) types.
  //-------------------------------------------------

  /** File is in conflict */
  CONFLICT,

  /** 
   * Newly created files added in the INDEX.
   */
  ADD,

  /**
   * File from INDEX, modified compared to HEAD
   */
  CHANGED,

  /**
   * A delete added in the INDEX, file is present in HEAD.
   */
  REMOVED,

  //------------------------END-------------------------

  //-------------------------------------------------
  // Unstaged files types.
  //-------------------------------------------------

  /**
   * A newly created file, not yet in the INDEX.
   */
  UNTRACKED,

  /** 
   *  A file that was modified compared to the one from INDEX. 
   */
  MODIFIED,

  /**
   * A file that is missing on the FS but is present in the INDEX.
   */
  MISSING,
  //-------------------------END------------------------
  
  /**
   * The state is unknown.
   */
  UNKNOWN
}
