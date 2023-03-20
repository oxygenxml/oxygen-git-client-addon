package com.oxygenxml.git.view.event;

import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.validation.ValidationManager;
import com.oxygenxml.git.validation.internal.IValidationOperationListener;
import com.oxygenxml.git.validation.internal.ValidationOperationInfo;

/**
 * Contains usefully methods for operations.
 * 
 * @author alex_smarandache
 *
 */
public class OperationUtil {

  /**
   * Hidden constructor.
   */
  private OperationUtil() {
    // not needed
  }

  /**
   * Install a mouse listener to change mouse cursor when an operation is too long.
   * 
   * @param validationManager The manager responsible with validation.
   * @param component     The component to install the listener.
   */
  public static void installMouseBusyCursor(final ValidationManager validationManager, 
      final JComponent component) {
    final OperationTimer cursorTimer = new OperationTimer(new AtomicBoolean(false), component);

    validationManager.addListener(new IValidationOperationListener() {
      
      @Override
      public void start(ValidationOperationInfo info) {
        cursorTimer.startOperationProgress();
      }
      
      @Override
      public void canceled(ValidationOperationInfo info) {
        cursorTimer.stopOperationProgress();
      }
      
      @Override
      public void finished(ValidationOperationInfo info) {
        cursorTimer.stopOperationProgress();
      }
    });
  }
  
  /**
   * Install a mouse listener to change mouse cursor when an operation is too long.
   * 
   * @param gitController The git controller.
   * @param component     The component to install the listener.
   */
  public static void installMouseBusyCursor(final GitController gitController, final JComponent component) {
    final OperationTimer cursorTimer = new OperationTimer(new AtomicBoolean(false), component);

    gitController.addGitListener(new GitEventAdapter() {

      @Override
      public void operationAboutToStart(GitEventInfo info) {
        cursorTimer.startOperationProgress();
      }

      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        cursorTimer.stopOperationProgress();
      }

      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        cursorTimer.stopOperationProgress();
      }
    });
  }

  /**
   * A timer used to start and stop the busy cursor when an operation is in progress.
   * 
   * @author alex_smarandache
   *
   */
  private static class OperationTimer extends Timer {
    
    /**
     * <code>true</code> if the operation is still in progress.
     */
    final AtomicBoolean operationProgress;
    
    /**
     * The component to install busy cursor.
     */
    final JComponent component;
    
    /**
     * Constructor.
     * 
     * @param operationProgress <code>true</code> if the operation is still in progress.
     * @param component         The component to install busy cursor.
     */
    public OperationTimer(final AtomicBoolean operationProgress, final JComponent component) {
      super(
          1000,
          e -> SwingUtilities.invokeLater(() -> {
            if (operationProgress.compareAndSet(true, true) && component != null) {
              //Operation process still running. Present a hint.
              component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
          }));
      this.operationProgress = operationProgress;
      this.component = component;
    }
    
    /**
     * Start the timer that presents the operation in progress.
     */
    public void startOperationProgress() {
      stopOperationProgress();
      operationProgress.getAndSet(true);
      start();
    }
    
    /**
     * Stops the timer that presents the operation in progress.
     */
    public void stopOperationProgress() {
      operationProgress.getAndSet(false);
      this.stop();
      SwingUtilities.invokeLater(() -> component.setCursor(Cursor.getDefaultCursor()));
    }
  }
}
