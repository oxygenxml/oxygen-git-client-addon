package com.oxygenxml.git.view.event;

/**
 * Used in the PushPullController to determine which command to execute
 * 
 * @author Beniamin Savu
 *
 */
public enum Command {
	/**
	 * The controller will execute push
	 */
	PUSH, 
	
	/**
	 * The controller will execute pull
	 */
	PULL;
}
