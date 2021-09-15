package com.oxygenxml.git.view.stash;

import org.eclipse.jgit.api.errors.StashApplyFailureException;

/**
 * Extends {@link StashApplyFailureException} to return the status of operation.
 *
 * @author Alex_Smarandache
 */
public class StashApplyFailureWithStatusException extends StashApplyFailureException {

  /**
   * The stash apply operation status.
   */
  private StashApplyStatus status = StashApplyStatus.NOT_APPLIED_UNKNOWN_CAUSE;


  /**
   * Constructor.
   *
   * @param message  The message.
   * @param cause    The cause.
   */
  public StashApplyFailureWithStatusException(String message, Throwable cause) {
    super(message, cause);
  }


  /**
   * Constructor.
   *
   * @param message  The message.
   */
  public StashApplyFailureWithStatusException(String message) {
    super(message);
  }


  /**
   * Constructor.
   *
   * @param status   The stash operation status.
   * @param message  The message.
   * @param cause    The cause.
   */
  public StashApplyFailureWithStatusException(StashApplyStatus status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }


  /**
   * Constructor.
   *
   * @param status   The stash operation status.
   * @param message  The message.
   */
  public StashApplyFailureWithStatusException(StashApplyStatus status, String message) {
    super(message);
    this.status = status;
  }


  /**
   * @return the status of operation.
   */
  public StashApplyStatus getStatus() {
    return status;
  }

}
