package com.oxygenxml.git.service.exceptions;

/**
 * This exception is throw when a squash commit contains an empty list of files.
 * 
 * @author alex_smarandache
 *
 */
public class NoChangesInSquashedCommitException extends Exception {

  /**
   * Constructor.
   * 
   * @param message The exception message.
   */
  public NoChangesInSquashedCommitException(final String message) {
    super(message);
  }
}
