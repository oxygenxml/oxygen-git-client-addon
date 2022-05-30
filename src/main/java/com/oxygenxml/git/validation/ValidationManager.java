package com.oxygenxml.git.validation;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.validation.gitoperation.PreCommitValidation;
import com.oxygenxml.git.validation.gitoperation.PrePushValidation;
import com.oxygenxml.git.validation.internal.IPreOperationValidation;
import com.oxygenxml.git.validation.internal.IValidationOperationListener;
import com.oxygenxml.git.validation.internal.ValidationListenersManager;

/**
 * Manage the validations.
 * 
 * @author alex_smarandache
 *
 */
public class ValidationManager {

  /**
   * The commit pre-validation operation.
   */
  private IPreOperationValidation commitPreValidation;

  /**
   * The push pre-validation operation.
   */
  private IPreOperationValidation pushPreValidation;
  
  /**
   * The listeners manager for validation operations.
   */
  private ValidationListenersManager listeners;


  /**
   * Hidden constructor.
   */
  private ValidationManager() {
    listeners = new ValidationListenersManager();
    commitPreValidation = new PreCommitValidation(new FilesValidator(
        new ProblemsCollector()), listeners);
    pushPreValidation = new PrePushValidation(new FilesValidator(
        new ProblemsCollector()), listeners);
  }

  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class SingletonHelper {
    static final ValidationManager INSTANCE = new ValidationManager();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  public static ValidationManager getInstance() {
    return SingletonHelper.INSTANCE;
  }

  /**
   * @param set the new pre-commit validator.
   */
  @TestOnly
  public void setPreCommitValidator(@NonNull final IPreOperationValidation preCommitValidator) {
    this.commitPreValidation = preCommitValidator;
  }

  /**
   * @param set the new pre-push validator.
   */
  @TestOnly
  public void setPrePushValidator(@NonNull final IPreOperationValidation prePushValidator) {
    this.pushPreValidation = prePushValidator;
  }
  
  /**
   * @return <code>true</code> if the files can and should be validated before commit.
   */
  public boolean isPreCommitValidationEnabled() {
    return commitPreValidation.isEnabled();
  }

  /**
   * @return <code>true</code> if the validation support is available(for Oxygen 25 and newer).
   */
  public boolean isAvailable() {
    return OxygenAPIWrapper.getInstance().isAvailable();
  }  

  /**
   * Check all staged files without missed or removed files.
   * <br>
   * If problems are detected, are presented for a user confirmation if the commit should be canceled or performed.
   * 
   * @see GitChangeType
   * 
   * @return <code>true</code> if the commit resources are valid to perform a commit.
   */
  public boolean checkCommitValid() {
    return commitPreValidation.checkValid();
  }

  /**
   * @return <code>true</code> if the pre push validation is enabled.
   */
  public boolean isPrePushValidationEnabled() {
    return pushPreValidation.isEnabled();
  }

  /**
   * Check all main files to test if the project is valid or no.
   * <br>
   * If problems are detected, are presented for a user confirmation if the push should be canceled or performed.
   * 
   * @see GitChangeType
   * 
   * @return <code>true</code> if the project is valid and the push should be performed.
   */
  public boolean checkPushValid() {
    return pushPreValidation.checkValid();
  }

  /** 
   * @param commitPreValidation The new pre-commit validator.
   */
  public void setCommitPreValidation(IPreOperationValidation commitPreValidation) {
    this.commitPreValidation = commitPreValidation;
  }

  /**
   * @param pushPreValidation The new pre-push validator.
   */
  public void setPushPreValidation(IPreOperationValidation pushPreValidation) {
    this.pushPreValidation = pushPreValidation;
  }

  /**
   * Add a new validation listener.
   * 
   * @param listener The listener to be added.
   */
  public void addListener(IValidationOperationListener listener) {
    this.listeners.addListener(listener);
  }
 
  
}
