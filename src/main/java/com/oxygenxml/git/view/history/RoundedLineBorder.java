package com.oxygenxml.git.view.history;

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
 * The RoundedLineBorder class is needed to create a border with all the corners rounded, allowing the user to set the 
 * diameter of the round corner and the width of the line to any preferred size.
 * 
 * We added this class because the in built-in {@link javax.swing.border.LineBorder} the rounded corner depends on the thickness of the line. For a thickness or 
 * 1 is not enough to create a visible rounded effect
 */
public class RoundedLineBorder extends AbstractBorder {
  /**
   * The width of the border.
   */
  private int lineSize;
  /**
   * The diameter of the round corner.
   */
  private int cornerSize;
  /**
   * The color of the foreground.
   */
  private Paint fill;
  /**
   * Initialized in constructor with a BasicStroke with line width of lineSize.
   */
  private Stroke stroke;
  /**
   * RenderingHint for anti aliasing.
   */
  private Object aaHint;

  /**
   * A class that extends AbstractBorder and is used for creating borders with
   * rounded corners
   * 
   * @param fill       The color of the foreground.
   * @param lineSize   the width of the border.
   * @param cornerSize The diameter of the round corner.
   * @param antiAlias  Sets the anti aliasing on or off.
   */
  public RoundedLineBorder(Paint fill, int lineSize, int cornerSize, boolean antiAlias) {
    this.fill = fill;
    this.lineSize = lineSize;
    this.cornerSize = cornerSize;
    stroke = new BasicStroke(lineSize);
    aaHint = antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
  }
  /**
   * Reinitializes the insets parameter with the maximum between line width and corner size of the current round border.
   *  
   * @param c       The component for which this border insets value applies
   * @param insets  The object to be reinitialized
   * 
   * @return The <code>insets</code> object
   */
  @Override
  public Insets getBorderInsets(Component c, Insets insets) {
    int size = Math.max(lineSize, cornerSize);
    int offs = lineSize << 1;
    int top = offs + lineSize;
    int bottom = offs + lineSize + 1;
    int left = size;
    int right = size;
    
    if (insets == null) {
      insets = new Insets(top, left, bottom, right);
    } else {
      insets.left = left;
      insets.right = right;
      insets.top = top;
      insets.bottom = bottom;
    }
    return insets;
  }
  
  /**
   * Paints the rounded border using the parameters from AbstractBorder and the variables from this class.
   * 
   * @param c       the component for which this border is being painted
   * @param g       the paint graphics
   * @param x       the x position of the rounded border
   * @param y       the y position of the rounded border
   * @param width   the width of the rounded border
   * @param height  the height of the rounded border
   */
  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2d = (Graphics2D) g;
    //Stores the paint, stroke and rendering hint for the old graphics
    Paint oldPaint = g2d.getPaint();
    Stroke oldStroke = g2d.getStroke();
    Object oldAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    try {
      //Uses the new paint, stroke and rendering hint for drawing the border
      g2d.setPaint(fill != null ? fill : c.getForeground());
      g2d.setStroke(stroke);
      if (aaHint != null) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaHint);
      }
      int off = lineSize << 1;
      // Draws a rectangle with rounded corners, using the coordinates of
      // AbstractBorder, width of lines and the diameter of the corners
      g2d.drawRoundRect(x + off, y + off, width - off * 2 - 1 , height - off * 2 - 1, cornerSize, cornerSize);
    } finally {
      //Resets the graphics to the old paint, stroke and rendering hint
      g2d.setPaint(oldPaint);
      g2d.setStroke(oldStroke);
      if (aaHint != null) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
      }
    }
  }
  
}
