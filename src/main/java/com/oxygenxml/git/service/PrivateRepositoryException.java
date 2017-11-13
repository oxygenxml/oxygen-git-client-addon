package com.oxygenxml.git.service;

/**
 * A private repository that requires authentication.
 *  
 * @author alex_jitianu
 */
public class PrivateRepositoryException extends Exception {
  /**
   * Constructor.
   * 
   * @param cause The cause.
   */
  public PrivateRepositoryException(Throwable cause) {
    super("Private repository", cause);
  }
}
