package com.oxygenxml.git.view.event;

/**
 * Event created when an the push or the pull action is initiated
 * 
 * @author Beniamin Savu
 *
 */
public class PushPullEvent extends GitEventInfo {

	/**
	 * The state in which the push or pull is (Started or Finished)
	 */
	private ActionStatus actionStatus;
	
	/**
	 * Additional information for the fired event
	 */
	private String message;

	public PushPullEvent(GitOperation op, ActionStatus actionStatus, String message) {
	  super(op);
		this.actionStatus = actionStatus;
		this.message = message;
	}

	public ActionStatus getActionStatus() {
		return actionStatus;
	}

	public void setActionStatus(ActionStatus actionStatus) {
		this.actionStatus = actionStatus;
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	@Override
	public String toString() {
	  return "Status: " + actionStatus + ", message: " + message;
	}

}
