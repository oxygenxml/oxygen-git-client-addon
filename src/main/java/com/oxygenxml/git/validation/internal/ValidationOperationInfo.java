package com.oxygenxml.git.validation.internal;

/**
 * Contains information about a validation operation.
 * 
 * @author alex_smarandache
 *
 */
public class ValidationOperationInfo {
  
  /**
   * The validation operation.
   */
  protected ValidationOperationType validationOp;
  
  /**
   * Object representing a state change.
   * 
   * @param gitOp The validationOp operation.
   */
  public ValidationOperationInfo(final ValidationOperationType validationOp) {
    this.validationOp = validationOp;
  }

  /**
   * @return the operation.
   */
  public ValidationOperationType getOperation() {
    return validationOp;
  }

  @Override
  public String toString() {
    return "ValidationOperationInfo [Operation: " + validationOp + "].";
  }

}
