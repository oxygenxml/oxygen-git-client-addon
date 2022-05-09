package com.oxygenxml.git.validation;

import org.eclipse.jgit.annotations.NonNull;

import ro.sync.document.DocumentPositionedInfo;

/**
 * Interface for a @DocumentPositionedInfo collector.
 * 
 * @author alex_smarandache
 *
 */
public interface ICollector {
  
  /**
   * Add the given items in the collected items.
   * 
   * @param dpis Items to be added in collected elements.
   */
  public void add(@NonNull final DocumentPositionedInfo[] dpis);
  
  /**
   * @return All collected items.
   */
  public @NonNull DocumentPositionedInfo[] getAll();
  
  /**
   * Reset the collected items.
   */
  public void reset();
  
  /**
   * @return <code>true</code> if the collector is empty.
   */
  public boolean isEmpty();
  
  /**
   * Set a filter to check if a problem should be collected.
   * 
   * @param filter The problems filter. 
   */
  public void setFilter(final IProblemFilter filter);

  /**
   * @return The current problem filter.
   */
  public IProblemFilter getFilter();
}
