package com.oxygenxml.sdksamples.workspace.git.view.event;

import java.util.List;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

/**
 * Acts as an observer and listens for changes
 * 
 * @author intern2
 *
 */
public interface Observer {
	/**
	 * Changes its state based on the given change event
	 * 
	 * @param changeEvent
	 *          - event containing the changes
	 */
	public void stateChanged(ChangeEvent changeEvent);

}
