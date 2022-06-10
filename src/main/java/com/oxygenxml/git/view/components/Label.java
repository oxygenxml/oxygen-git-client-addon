package com.oxygenxml.git.view.components;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import com.oxygenxml.git.view.util.UIUtil;

/**
 * A label with several functionalities for a better integration with Oxygen.
 * 
 * @author alex_smarandache
 *
 */
public class Label extends JLabel {
  
  /**
   * Creates a <code>JLabel</code> instance with the specified
   * text, image, and horizontal alignment.
   * The label is centered vertically in its display area.
   * The text is on the trailing edge of the image.
   *
   * @param text  The text to be displayed by the label.
   * @param icon  The image to be displayed by the label.
   * @param horizontalAlignment  One of the following constants
   *           defined in <code>SwingConstants</code>:
   *           <code>LEFT</code>,
   *           <code>CENTER</code>,
   *           <code>RIGHT</code>,
   *           <code>LEADING</code> or
   *           <code>TRAILING</code>.
   */
  public Label(final String text, final Icon icon, final int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
  }

  /**
   * Creates a <code>JLabel</code> instance with the specified
   * text and horizontal alignment.
   * The label is centered vertically in its display area.
   *
   * @param text  The text to be displayed by the label.
   * @param horizontalAlignment  One of the following constants
   *           defined in <code>SwingConstants</code>:
   *           <code>LEFT</code>,
   *           <code>CENTER</code>,
   *           <code>RIGHT</code>,
   *           <code>LEADING</code> or
   *           <code>TRAILING</code>.
   */
  public Label(final String text, final int horizontalAlignment) {
      super(text, null, horizontalAlignment);
  }

  /**
   * Creates a <code>JLabel</code> instance with the specified text.
   * The label is aligned against the leading edge of its display area,
   * and centered vertically.
   *
   * @param text  The text to be displayed by the label.
   */
  public Label(final String text) {
      super(text);
  }

  /**
   * Creates a <code>JLabel</code> instance with the specified
   * image and horizontal alignment.
   * The label is centered vertically in its display area.
   *
   * @param image  The image to be displayed by the label.
   * @param horizontalAlignment  One of the following constants
   *           defined in <code>SwingConstants</code>:
   *           <code>LEFT</code>,
   *           <code>CENTER</code>,
   *           <code>RIGHT</code>,
   *           <code>LEADING</code> or
   *           <code>TRAILING</code>.
   */
  public Label(final Icon image, final int horizontalAlignment) {
      super(image, horizontalAlignment);
  }

  /**
   * Creates a <code>JLabel</code> instance with the specified image.
   * The label is centered vertically and horizontally
   * in its display area.
   *
   * @param image  The image to be displayed by the label.
   */
  public Label(final Icon image) {
      super(image);
  }

  /**
   * Creates a <code>JLabel</code> instance with
   * no image and with an empty string for the title.
   * The label is centered vertically
   * in its display area.
   * The label's contents, once set, will be displayed on the leading edge
   * of the label's display area.
   */
  public Label() {
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
