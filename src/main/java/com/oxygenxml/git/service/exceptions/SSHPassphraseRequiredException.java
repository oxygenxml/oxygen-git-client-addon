package com.oxygenxml.git.service.exceptions;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

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
    super(Translator.getInstance().getTranslation(Tags.SSH_KEY_PASSPHRASE_REQUIRED), cause);
  }
}
