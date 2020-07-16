package com.oxygenxml.git.view.historycomponents;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.border.AbstractBorder;

/**
 * LineBorder can create a rounded border with only the top left corner slightly rounded.
 * This class is needed to create a border with all the corners rounded, allowing us to 
 * set the diameter of the round corner and the width of the line to any preferred size
 */
@SuppressWarnings("serial")
public class RoundedLineBorder extends AbstractBorder {
  /**
   * lineSize   an integer that specifies the width of the border
   * cornerSize an integer that specifies the diameter of the round corner
   * fill       a paint that specifies the color of the foreground
   * stroke     a stroke that is initialized in constructor with a BasicStroke with line width of lineSize
   * aaHint     an object that gets the renderingHint for anti aliasing
   */
  private int lineSize;
  private int cornerSize;
  private Paint fill;
  private Stroke stroke;
  private Object aaHint;

  /**
   * A class that extends AbstractBorder and is used for creating borders with
   * rounded corners
   * 
   * @param fill       a paint that specifies the color of the foreground
   * @param lineSize   an integer that specifies the width of the border
   * @param cornerSize an integer that specifies the diameter of the round corner
   * @param antiAlias  a boolean that sets the anti aliasing on or off
   */
  public RoundedLineBorder(Paint fill, int lineSize, int cornerSize, boolean antiAlias) {
    this.fill = fill;
    this.lineSize = lineSize;
    this.cornerSize = cornerSize;
    stroke = new BasicStroke(lineSize);
    aaHint = antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
  }

  @Override
  public Insets getBorderInsets(Component c, Insets insets) {
    int size = Math.max(lineSize, cornerSize);
    if (insets == null)
      insets = new Insets(size, size, size, size);
    else
      insets.left = insets.top = insets.right = insets.bottom = size;
    return insets;
  }
  /**
   * @param lineSize integer that specifies the width of the line
   * @param cornerSize integer that specifies the diameter of the rounded corner
   * @return size of the inset for the top margin
   */
  public static int getTopInset(int lineSize, int cornerSize) {
    int size = Math.max(lineSize, cornerSize);
    return size/2;
  }

  /**
   * Paints the rounded border using the parameters from AbstractBorder
   * and the variables from this class
   */
  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2d = (Graphics2D) g;
    Paint oldPaint = g2d.getPaint();
    Stroke oldStroke = g2d.getStroke();
    Object oldAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    try {
      g2d.setPaint(fill != null ? fill : c.getForeground());
      g2d.setStroke(stroke);
      if (aaHint != null)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaHint);
      int off = lineSize << 1;
      // Draws a rectangle with rounded corners, using the coordinates of
      // AbstractBorder, width of lines and the diameter of the corners
      g2d.drawRoundRect(x + off, y + off, width - off * 3, height - off * 3, cornerSize, cornerSize);
    } finally {
      g2d.setPaint(oldPaint);
      g2d.setStroke(oldStroke);
      if (aaHint != null)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }
  }
}
