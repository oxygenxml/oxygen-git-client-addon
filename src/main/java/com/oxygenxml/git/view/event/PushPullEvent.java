package com.oxygenxml.git.view.event;

import java.util.Set;

/**
 * Event created when an the push or the pull action is initiated
 * 
 * @author intern2
 *
 */
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
