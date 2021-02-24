package com.oxygenxml.git.service;

/**
 * The status after the pull finishes
 * 
 * @author Beniamin Savu
 *
 */
public enum PullStatus {
	/**
	 * The pull cannot execute if there are uncommitted files
	 */
	UNCOMITED_FILES(false),

	/**
	 * This is set when the pull gets conflicts
	 */
	CONFLICTS(false),

	/**
	 * The pull finished with no errors
	 */
	OK(true),

	/**
	 * The repository is already up to date
	 */
	UP_TO_DATE(true),

	/**
	 * The repository already has conflicts and cannot pull until they are resolved
	 */
	REPOSITORY_HAS_CONFLICTS(false),
	
	/**
	 * Lock failed.
	 */
	LOCK_FAILED(false);
  
  /**
   * <code>true</code> if this status represents a successful operation.
   */
  private boolean state = false;
  
  /**
   * Constructor.
   * 
   * @param state <code>true</code> if this status represents a successful operation.
   */
  PullStatus(boolean state) {
    this.state = state;
  }
  
  /**
   * @return <code>true</code> if this status represents a successful operation.
   */
  public boolean isSuccessful() {
    return state;
  }
	
}
