package com.oxygenxml.git.validation.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * Manage validation listeners.
 * 
 * @author alex_smarandache
 *
 */
public class ValidationListenersManager {
  
  /**
   * The listeners.
   */
  private final Set<IValidationOperationListener> listeners;
  
  /**
   * Constructor.
   */
  public ValidationListenersManager() {
    listeners = new HashSet<>();
  }
  
  /**
   * Add a new listener for validation operations.
   * 
   * @param listener The listener to be added.
   */
  public void addListener(final IValidationOperationListener listener) {
    listeners.add(listener);
  }
  
  /**
   * Remove the listener.
   * 
   * @param listener The listener to be removed.
   */
  public void removeListener(final IValidationOperationListener listener) {
    listeners.remove(listener);
  }
  
  /**
   * Notify the listeners about a validation operation is before starting.
   * 
   * @param info The informations about information that will start.
   */
  public void notifyListenersAboutStartOperation(final ValidationOperationInfo info) {
    listeners.forEach(listener -> listener.start(info));
  }
  
  /**
   * Notify the listeners about a validation operation has been finished.
   * 
   * @param info The informations about information that was ended.
   */
  public void notifyListenersAboutFinishedOperation(final ValidationOperationInfo info) {
    listeners.forEach(listener -> listener.finished(info));
  }
  
  /**
   * Notify the listeners about a validation operation has been canceled.
   * 
   * @param info The informations about information that was ended.
   */
  public void notifyListenersAboutCanceledOperation(final ValidationOperationInfo info) {
    listeners.forEach(listener -> listener.canceled(info));
  }

}
