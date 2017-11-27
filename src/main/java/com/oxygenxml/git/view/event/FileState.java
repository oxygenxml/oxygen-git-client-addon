package com.oxygenxml.git.view.event;

/**
 * The state of a file at a given moment.
 *  
 * @author Beniamin Savu
 *
 */
public enum FileState {
	STAGED,
	UNSTAGED,
	COMMITED,
	UNDEFINED,
	DISCARD,
	RESOLVED
}
