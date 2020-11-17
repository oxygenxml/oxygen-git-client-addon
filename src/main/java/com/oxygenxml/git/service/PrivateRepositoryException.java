package com.oxygenxml.git.service;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

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
    super(Translator.getInstance().getTranslation(Tags.USERNAME_AND_PASSWORD_REQUIRED), cause);
  }
}
