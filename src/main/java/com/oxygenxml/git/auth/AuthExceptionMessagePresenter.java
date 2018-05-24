package com.oxygenxml.git.auth;

/**
 * Can be used to present an authentication exception message to the user.
 * 
 * @author sorin_carbunaru
 */
public interface AuthExceptionMessagePresenter {
  
  /**
   * Present exception message.
   * 
   * @param ex The message.
   */
  void presentMessage(String message);

}
