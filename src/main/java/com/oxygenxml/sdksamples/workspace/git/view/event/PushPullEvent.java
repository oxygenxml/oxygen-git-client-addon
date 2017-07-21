package com.oxygenxml.sdksamples.workspace.git.view.event;

public class PushPullEvent {

	private ActionStatus actionStatus;

	public PushPullEvent(ActionStatus actionStatus) {
		this.actionStatus = actionStatus;
	}

	public ActionStatus getActionStatus() {
		return actionStatus;
	}

	public void setActionStatus(ActionStatus actionStatus) {
		this.actionStatus = actionStatus;
	}

}
