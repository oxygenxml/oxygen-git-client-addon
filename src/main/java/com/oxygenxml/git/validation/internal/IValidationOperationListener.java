package com.oxygenxml.git.validation.internal;

/**
 * Receives notifications when the state of the validation operations.
 * 
 * @author alex_smarandache
 *
 */
public interface IValidationOperationListener {

  /**
   * Appears before operation start.
   * 
   * @param info The operation that will be started.
   */
  public void start(final ValidationOperationInfo info);
  
  /**
   * Appears when operation has been canceled.
   * 
   * @param info The operation that will be started.
   */
  public void canceled(final ValidationOperationInfo info);
  
  /**
   * Appears when operation has been finished.
   * 
   * @param info The operation that will be started.
   */
  public void finished(final ValidationOperationInfo info);

}
