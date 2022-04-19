package com.oxygenxml.git.service;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * This exception is throw when a squash commit contains an empty list of files.
 * 
 * @author alex_smarandache
 *
 */
public class NoFilesInSquashedCommitException extends GitAPIException {

  /**
   * Constructor.
   * 
   * @param message The exception message.
   */
  public NoFilesInSquashedCommitException(final String message) {
    super(message);
  }
}
