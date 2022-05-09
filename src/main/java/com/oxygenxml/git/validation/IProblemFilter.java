package com.oxygenxml.git.validation;

import ro.sync.document.DocumentPositionedInfo;

/**
 * Used to filter problems that be collected by an instance of @ICollector.
 * 
 * @see ICollector
 * 
 * @author alex_smarandache
 *
 */
public interface IProblemFilter {
  
  /**
   * @param dpi A detected problem at validation process.
   * 
   * @return <code>true</code> if the problem should be collected.
   */
  public boolean include(final DocumentPositionedInfo dpi);

}
