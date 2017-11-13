package com.oxygenxml.git.service;

/**
 * A SSH pass is required.
 *  
 * @author alex_jitianu
 */
public class SSHPassphraseRequiredException extends Exception {
  /**
   * Constructor.
   * 
   * @param cause The cause.
   */
  public SSHPassphraseRequiredException(Throwable cause) {
    super("SSH Passphare required", cause);
  }
}
