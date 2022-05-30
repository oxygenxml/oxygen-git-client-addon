package com.oxygenxml.git.validation.internal;

/**
 * Interface for a pre-operation validation.
 * 
 * @author alex_smarandache
 *
 */
public interface IPreOperationValidation {

  /**
   * @return <code>true</code> if the validation is enabled.
   */
  public boolean isEnabled();
  
  /**
   * @return <code>true</code> if the operation is valid to be performed.
   */
  public boolean checkValid();
}
