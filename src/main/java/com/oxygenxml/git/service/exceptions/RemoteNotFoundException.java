package com.oxygenxml.git.service.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This exception appear when a remote is not found.
 * 
 * @author alex_smarandache
 */
@Getter
@AllArgsConstructor
public class RemoteNotFoundException extends Exception {
  
  /**
   * The exception status cause.
   */
  private final int status;
  
  /**
   * Constant for status when the remote doesn't exists.
   */
  public static final int STATUS_REMOTE_NOT_EXISTS = 1;

  /**
   * Constant for status when branches are not founded.
   */
  public static final int STATUS_BRANCHES_NOT_EXIST = 2;

}
