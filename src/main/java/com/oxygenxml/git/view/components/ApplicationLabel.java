package com.oxygenxml.git.view.components;

import javax.swing.JLabel;
import javax.swing.JToolTip;

import com.oxygenxml.git.view.util.UIUtil;

/**
 * Custom implementation over JLabel.
 * <br>
 * The default tooltip is replaced with a Multiline Tooltip.
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
      super();
  }
  
  /**
   * Create a multiline tool tip.
   */
  @Override
  public JToolTip createToolTip() {
    return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
  }

}
