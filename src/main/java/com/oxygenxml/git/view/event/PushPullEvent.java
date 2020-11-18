package com.oxygenxml.git.view.event;

/**
 * Event created when an the push or the pull action is initiated
 * 
 * @author Beniamin Savu
 *
 */
public class PushPullEvent extends GitEventInfo {

  /**
   *  An optional status about the operation.
   */
  private ActionStatus actionStatus;

  /**
   * Additional information for the fired event
   */
  private String message = "";

  /**
   * Constructor.
   * 
   * @param op The executed operation.
   * @param actionStatus An optional status about the operation.
   */
  public PushPullEvent(GitOperation op, ActionStatus actionStatus) {
    super(op);
    this.actionStatus = actionStatus;
  }

  /**
   * Constructor.
   * 
   * @param op The executed operation.
   * @param message An optional message about the operation.
   */
  public PushPullEvent(GitOperation op, String message) {
    super(op);
    this.message = message;
  }

  public ActionStatus getActionStatus() {
    return actionStatus;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Status: " + actionStatus + ", message: " + message;
  }

  public boolean hasConficts() {
    return actionStatus == ActionStatus.PULL_MERGE_CONFLICT_GENERATED || actionStatus == ActionStatus.PULL_REBASE_CONFLICT_GENERATED;
  }

}
