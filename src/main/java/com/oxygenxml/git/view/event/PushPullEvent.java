package com.oxygenxml.git.view.event;

/**
 * Event created when an the push or the pull action is initiated
 * 
 * @author Beniamin Savu
 *
 */
public class PushPullEvent {

	/**
	 * The state in which the push or pull is (Started or Finished)
	 */
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
