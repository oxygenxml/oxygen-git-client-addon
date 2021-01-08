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
   * An exception if the operation failed.
   */
  private Exception cause;

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

  /**
   * Constructor.
   * 
   * @param operation The executed operation.
   * @param message An optional message about the operation.
   * @param cause  An exception if the operation failed.
   */
  public PushPullEvent(GitOperation operation, String message, Exception cause) {
    super(operation);
    this.message = message;
    this.cause = cause;
  }
  
  /**
   * @return  An exception if the operation failed.
   */
  public Exception getCause() {
    return cause;
  }

  /**
   * @return Extra details about the result.
   */
  public ActionStatus getActionStatus() {
    return actionStatus;
  }

  /**
   * @return An information about the curent state of the operation.
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Status: " + actionStatus + ", message: " + message + " " + super.toString();
  }

  /**
   * @return <code>true</code> if the operation finished and generated conflicts.
   */
  public boolean hasConficts() {
    return actionStatus == ActionStatus.PULL_MERGE_CONFLICT_GENERATED || actionStatus == ActionStatus.PULL_REBASE_CONFLICT_GENERATED;
  }

}
