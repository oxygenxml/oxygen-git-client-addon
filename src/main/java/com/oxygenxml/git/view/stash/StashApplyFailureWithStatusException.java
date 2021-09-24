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
  private final StashApplyStatus status;

  
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
