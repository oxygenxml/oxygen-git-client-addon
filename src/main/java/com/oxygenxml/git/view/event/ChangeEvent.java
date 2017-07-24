package com.oxygenxml.git.view.event;

import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;

/**
 * Event created when a file is changing its state, from staged to unstaged or
 * from unstaged to staged
 * 
 * @author intern2
 *
 */
public class ChangeEvent {
	private StageState newState;
	private StageState oldState;
	private List<FileStatus> fileToBeUpdated;

	public ChangeEvent(StageState newState, StageState oldState, List<FileStatus> fileToBeUpdated) {
		super();
		this.newState = newState;
		this.oldState = oldState;
		this.fileToBeUpdated = fileToBeUpdated;
	}

	public StageState getNewState() {
		return newState;
	}

	public StageState getOldState() {
		return oldState;
	}

	public List<FileStatus> getFileToBeUpdated() {
		return fileToBeUpdated;
	}
	

}