package com.oxygenxml.git.service;

/**
 * Commit identifiers.
 * 
 * @author adrian_sorop
 */
public enum Commit {
	/**
	 * Used when a conflict occurs. My own version of the file from the local repository that I committed before Pull.
	 */
	MINE,
	/**
	 * Used when a conflict occurs. The remote version of the file that came on Pull.
	 */
	THEIRS,
	/**
	 * Used when a conflict occurs. The common base, when the two versions forked (MINE, THEIRS).
	 */
	BASE,
	/**
	 * The last commit in the local repository. HEAD.
	 */
	LOCAL
}
