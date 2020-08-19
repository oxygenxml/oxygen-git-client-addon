package com.oxygenxml.git.view;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Updater that executes a runnable with a given delay.
 * 
 * @author Bogdan Draghici
 *
 */
public class CoalescedEventUpdater {

  /**
   * Timer for the coalescing.
   */
  private Timer timer;

  /**
   * Constructor.
   * 
   * @param delay    The delay of the coalesced event.
   * @param callback Teh callback to be executed when the delay expires.
   */
  public CoalescedEventUpdater(int delay, Runnable callback) {
    timer = new Timer(delay, e -> {
      timer.stop();
      callback.run();
    });
  }

  /**
   * Update (restart the timer of AWT).
   */
  public void update() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(timer::restart);
    } else {
      timer.restart();
    }
  }

}
