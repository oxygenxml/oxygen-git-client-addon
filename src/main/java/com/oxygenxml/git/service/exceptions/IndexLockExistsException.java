package com.oxygenxml.git.service.exceptions;

/**
 * Exception thrown when an operation is about to start, 
 * but the index.lock file exists in the local repository.
 */
public class IndexLockExistsException extends Exception {
  
}
