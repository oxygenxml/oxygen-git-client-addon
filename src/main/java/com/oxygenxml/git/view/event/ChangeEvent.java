package com.oxygenxml.git.view.event;

import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;

/**
 * Event created when a file is changing its state, from staged to unstaged or
 * from unstaged to staged
 * 
 * @author Beniamin Savu
 *
 */
public class ChangeEvent {
	
	/**
	 * The new state in which the files will be
	 */
	private FileState newState;
	
	/**
	 * The old state in which the files were
	 */
	private FileState oldState;
	
	/**
	 * The files that are changing their state
	 */
	private List<FileStatus> fileToBeUpdated;

	public ChangeEvent(FileState newState, FileState oldState, List<FileStatus> fileToBeUpdated) {
		super();
		this.newState = newState;
		this.oldState = oldState;
		this.fileToBeUpdated = fileToBeUpdated;
	}

	public FileState getNewState() {
		return newState;
	}

	public FileState getOldState() {
		return oldState;
	}

	public List<FileStatus> getFileToBeUpdated() {
		return fileToBeUpdated;
	}

}