package com.oxygenxml.git.view.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.oxygenxml.git.view.CoalescedEventUpdater;

/**
 * A document Listener for coalescing events
 * 
 * @author gabriel_nedianu
 *
 */
public class CoalescingDocumentListener implements DocumentListener {

  /**
   * The delay for the updater
   */
  private static int DELAY = 400;
  /**
   * The updater for the coalesced event
   */
  private CoalescedEventUpdater updater;

  /**
   * Constructor
   * 
   * @param runnable The runnable to be executed
   */
  public CoalescingDocumentListener(Runnable runnable) {
    updater = new CoalescedEventUpdater(DELAY, runnable);
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    update();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    update();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    update();
  }

  private void update() {
    updater.update();
  }

}
