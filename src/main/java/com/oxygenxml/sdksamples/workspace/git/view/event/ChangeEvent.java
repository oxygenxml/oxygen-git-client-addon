package com.oxygenxml.sdksamples.workspace.git.view.event;

import java.util.List;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.view.StageState;

public class ChangeEvent {
	private StageState newState;
	private StageState oldState;
	private List<FileStatus> fileToBeUpdated;
	private Subject source;
	
	public ChangeEvent(StageState newState, StageState oldState, List<FileStatus> fileToBeUpdated, Subject source) {
		super();
		this.newState = newState;
		this.oldState = oldState;
		this.fileToBeUpdated = fileToBeUpdated;
		this.source = source;
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

	public Subject getSource() {
		return source;
	}
}