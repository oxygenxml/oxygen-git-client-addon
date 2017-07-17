package com.oxygenxml.sdksamples.workspace.git.view.event;

public interface Subject {
	// methods to register and unregister observers
	public void addObserver(Observer obj);

	public void removeObserver(Observer obj);
}
