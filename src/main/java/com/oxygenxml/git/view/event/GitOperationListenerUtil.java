package com.oxygenxml.git.view.event;

import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oxygenxml.git.service.GitEventAdapter;

/**
 * Contains usefully methods for git operation listeners.
 * 
 * @author alex_smarandache
 *
 */
public class GitOperationListenerUtil {

  /**
   * Hidden constructor.
   */
  private GitOperationListenerUtil() {
    // not needed
  }

  /**
   * Install a mouse listener to change mouse cursor when an operation is too long.
   * 
   * @param gitController The git controller.
   * @param component     The component to install the listener.
   */
  public static void installMouseListener(final GitController gitController, JComponent component) {
    final AtomicBoolean operationProgress = new AtomicBoolean(false);
    final Timer cursorTimer = new Timer(
        1000,
        e -> SwingUtilities.invokeLater(() -> {
          if (operationProgress.compareAndSet(true, true) && component != null) {
            //Operation process still running. Present a hint.
            component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          }
        }));
    
    gitController.addGitListener(new GitEventAdapter() {

      @Override
      public void operationAboutToStart(GitEventInfo info) {
        stopTimer(operationProgress, cursorTimer);
        operationProgress.getAndSet(true);
        cursorTimer.start();
      }

      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        stopTimer(operationProgress, cursorTimer);
        SwingUtilities.invokeLater(() -> component.setCursor(Cursor.getDefaultCursor()));
      }

      @Override
      public void operationFailed(GitEventInfo info, Throwable t) {
        stopTimer(operationProgress, cursorTimer);
        SwingUtilities.invokeLater(() -> component.setCursor(Cursor.getDefaultCursor()));
      }
    });
  }

  /**
   * Stops the timer that presents the operation in progress.
   * 
   * @param operationProgress The current operation.
   * @param cursorTimer       The timer that presents the operation in progress..
   */
  private static void stopTimer(final AtomicBoolean operationProgress, final Timer cursorTimer) {
    operationProgress.getAndSet(false);
    cursorTimer.stop();
  }
}
