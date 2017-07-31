package com.oxygenxml.git.view.event;


/**
 * Acts as an observer and listens for changes
 * 
 * @author Beniamin Savu
 *
 */
public interface Observer<T> {
	/**
	 * Changes its state based on the given change event
	 * 
	 * @param changeEvent
	 *          - event containing the changes
	 */
	public void stateChanged(T changeEvent);


}
