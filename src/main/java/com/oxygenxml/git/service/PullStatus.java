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
	UNCOMITED_FILES,

	/**
	 * This is set when the pull gets conflicts
	 */
	CONFLICTS,

	/**
	 * The pull finished with no errors
	 */
	OK,

	/**
	 * The repository is already up to date
	 */
	UP_TO_DATE,

	/**
	 * The repository already has conflicts and cannot pull until they are resolved
	 */
	REPOSITORY_HAS_CONFLICTS;
}
