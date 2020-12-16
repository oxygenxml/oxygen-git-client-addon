/*
 * Copyright (c) 2020 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.UIUtil;

/**
 * Search field with hint.
 * 
 */
@SuppressWarnings("java:S110")
public abstract class FilterTextField extends JTextField implements FocusListener {
  /**
   * empty string
   */
  private static final String EMPTY = "";
  /**
   * colaescing delay
   */
  private static final int DELAY = 400;
  /**
   * Coalescing updater
   */
  private CoalescedEventUpdater updater;
  /**
   * UID
   */
  private static final long serialVersionUID = 1L;

  /**
   * Hint to present when the field is not focused.
   */
  private String noFocusHint;

  /**
   * Hint to present when the filed is focused.
   */
  private String focusHint;

  /**
   * The listener that manages the filter text modifications.
   */
  private transient DocumentListener docListener;

  /**
   * Internal focus listener to force hints to repaint when focus changes.
   */
  private final transient FocusListener internalFocusListenerToForceRepaintOnFocusChange = new FocusListener() {
    @Override
    public void focusLost(FocusEvent e) {
      focusGained(e);
    }

    @Override
    public void focusGained(FocusEvent e) {
      repaint();
    }
  };

  /**
   * Creates a filter component with hints.
   * 
   * @param noFocusHint Hint to present when the field is not focused.
   */
  protected FilterTextField(String noFocusHint) {
    this(noFocusHint, null);
    updater = new CoalescedEventUpdater(DELAY,()-> filterChanged(getText()));
  }

  /**
   * Creates a filter component with hints.
   * 
   * @param noFocusHint Hint to present when the field is not focused.
   * @param focusHint   Hint to present when the filed is focused.
   */
  protected FilterTextField(String noFocusHint, String focusHint) {
    this.noFocusHint = noFocusHint;
    this.focusHint = focusHint;
    setFont(this.getFont());
    addFocusListener(internalFocusListenerToForceRepaintOnFocusChange);
    addFocusListener(this);

    docListener = new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        updater.update();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        // not used
      }
    };
    getDocument().addDocumentListener(docListener);
    this.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clear_field");
    this.getActionMap().put("clear_field", new AbstractAction() {
      /**
       * generated UID
       */
      private static final long serialVersionUID = 6936768622117942289L;

      @Override
      public void actionPerformed(ActionEvent e) {
        setText(EMPTY);
        filterChanged(EMPTY);
      }
    });
    setToolTipText(Translator.getInstance().getTranslation(Tags.USE_FILTER_TO_SEARCH));
  }

  /**
   * @param text new filter text.
   */
  public abstract void filterChanged(String text);

  /**
   * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    // Paint any hints only if component is enabled and has no content
    if (isEnabled()) {
      Document document = getDocument();
      if (document != null && document.getLength() == 0) {
        if (hasFocus()) {
          if (focusHint != null && !focusHint.isEmpty()) {
            UIUtil.drawHint(this, g, focusHint, getSelectedTextColor());
          }
        } else {
          if (noFocusHint != null && !noFocusHint.isEmpty()) {
            UIUtil.drawHint(this, g, noFocusHint, null);
          }
        }
      }
    }
  }

  @Override
  public void focusGained(FocusEvent e) {
    Document document = getDocument();
    if (document != null && document.getLength() > 0) {
      selectAll();
    }
  }

  @Override
  public void focusLost(FocusEvent e) {
    // empty
  }
}
