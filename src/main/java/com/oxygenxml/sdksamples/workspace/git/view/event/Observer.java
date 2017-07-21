package com.oxygenxml.sdksamples.workspace.git.view.event;


/**
 * Acts as an observer and listens for changes
 * 
 * @author intern2
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
