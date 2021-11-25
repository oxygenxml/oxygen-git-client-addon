package com.oxygenxml.git.view.history;

/**
 * Strategy to present repository history. 
 * 
 * @author alex_smarandache
 *
 */
public enum HistoryStrategy {
	
	/**
	 * Presents all branches, both local and remote.
	 */
	ALL_BRANCHES,
	
	/**
	 * Presents all local branches.
	 */
	ALL_LOCAL_BRANCHES,
	
	/**
	 * Presents the current branch, both local and remote.
	 */
	CURRENT_BRANCH,
	
	/**
	 * Presents only the current local branch.
	 */
	CURRENT_LOCAL_BRANCH

}
