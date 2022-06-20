package com.oxygenxml.git.validation.internal.exception;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

/**
 * This exception occurs when Main Files support is not configured and it is needed for validation.
 * 
 * @author alex_smarandache
 *
 */
public class MainFilesNotAvailableException extends ValidationException {

  /**
   * Constructor.
   */
  public MainFilesNotAvailableException() {
    super(Translator.getInstance().getTranslation(Tags.MAIN_FILES_SUPPORT_NOT_ENABLED));
  }

}
