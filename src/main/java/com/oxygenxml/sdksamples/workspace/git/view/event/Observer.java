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

	/**
	 * 
	 * TODO Is this necessary? We could fire a stateChange, with a transition from 
	 * StageState.STAGED->StageState.COMMITTED.   
	 * 
	 * 
	 * Removes the given files from being observed
	 * 
	 * @param files
	 *          - files to be removed from the observers
	 */
	public void clear(List<FileStatus> files);
}
