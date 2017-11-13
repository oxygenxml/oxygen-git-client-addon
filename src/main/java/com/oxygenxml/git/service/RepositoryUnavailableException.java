package com.oxygenxml.git.service;

/**
 * Thrown when the repository is down.
 *  
 * @author alex_jitianu
 */
public class RepositoryUnavailableException extends Exception {
  /**
   * Constructor.
   * 
   * @param cause The cause.
   */
  public RepositoryUnavailableException(Throwable cause) {
    super("Unreachable repository: " + cause.getMessage(), cause);
  }
}
