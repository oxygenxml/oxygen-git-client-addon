package com.oxygenxml.git.view.components;

import javax.swing.JLabel;
import javax.swing.JToolTip;

import com.oxygenxml.git.view.util.UIUtil;

/**
 * A label with several functionalities for a better integration with Oxygen(Multiline tool tip).
 * 
 * @author alex_smarandache
 *
 */
public class ApplicationLabel extends JLabel {

  /**
   * Creates a <code>JLabel</code> instance with the specified text.
   * The label is aligned against the leading edge of its display area,
   * and centered vertically.
   *
   * @param text  The text to be displayed by the label.
   */
  public ApplicationLabel(final String text) {
      super(text);
  }

  /**
   * Creates a <code>JLabel</code> instance with
   * no image and with an empty string for the title.
   * The label is centered vertically
   * in its display area.
   * The label's contents, once set, will be displayed on the leading edge
   * of the label's display area.
   */
  public ApplicationLabel() {
      super("", null, LEADING);
  }
  
  /**
   * Create a multiline tool tip.
   */
  @Override
  public JToolTip createToolTip() {
    return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
  }

}
