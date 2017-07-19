package com.oxygenxml.sdksamples.workspace.git.view.event;

import java.util.List;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

public interface Observer {
	public void stateChanged(ChangeEvent changeEvent);
	
	public void clear(List<FileStatus> files);
}
