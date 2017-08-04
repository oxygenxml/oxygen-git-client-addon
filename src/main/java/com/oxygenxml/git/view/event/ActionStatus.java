package com.oxygenxml.git.view.event;

/**
 * Used for the Push and Pull command to to know in which state it is
 * 
 * @author Beniamin Savu
 *
 */
public enum ActionStatus {
	/**
	 * Specifies that the Push/Pull command has started
	 */
	STARTED, 
	
	/**
	 * Specifies that the Push/Pull command has finished
	 */
	FINISHED, 
	
	/**
	 * Specifies to update the Push/Pull counters
	 */
	UPDATE_COUNT;
}
