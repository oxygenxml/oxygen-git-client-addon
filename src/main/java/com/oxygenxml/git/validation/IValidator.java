package com.oxygenxml.git.validation;

import java.net.URL;
import java.util.List;

/**
 * Interface used to validate a list of files. 
 * 
 * @author alex_smarandache
 *
 */
public interface IValidator {
  
  /**
   * Validate the given files.
   * 
   * @param files The files to be validated.
   */
  public void validate(final List<URL> files);
  
  /**
   * @return <code>true</code> if the validator is available.
   */
  public boolean isAvailable();
  
  /**
   * @see ICollector
   * 
   * @return The collected problems.
   */
  public ICollector getCollector();
  
}
