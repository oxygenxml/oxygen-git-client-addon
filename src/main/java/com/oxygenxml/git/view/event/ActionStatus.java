package com.oxygenxml.git.view.event;

/**
 * Used for the Push and Pull command to to know in which state it is
 * 
 * @author Beniamin Savu
 *
 */
public enum ActionStatus {
	/**
	 * Pull (rebase) just generated a conflict.
	 */
	PULL_REBASE_CONFLICT_GENERATED,
	/**
   * Pull (merge) just generated a conflict.
   */
	PULL_MERGE_CONFLICT_GENERATED,
}
