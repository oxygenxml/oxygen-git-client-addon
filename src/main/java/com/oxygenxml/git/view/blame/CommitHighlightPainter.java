package com.oxygenxml.git.view.blame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.apache.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.utils.Equaler;

import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

/**
 * Simple highlight painter that fills a highlighted area with
 * a solid color.
 */
public class CommitHighlightPainter extends LayeredHighlighter.LayerPainter {

  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = Logger.getLogger(CommitHighlightPainter.class);
  /**
   * Text page on which the painter is installed.
   */
  private WSTextEditorPage textpage;
  /**
   * Mapping between lines and revisions in which that line was last changed.
   */
  private Map<Integer, RevCommit> l2r;
  /**
   * The active commit is the commit under the caret.
   */
  private Supplier<RevCommit> activeCommit;
  /**
   * Color used by the painter.
   */
  private Color color;

  /**
   * Constructs a new highlight painter. If <code>c</code> is null,
   * the JTextComponent will be queried for its selection color.
   *
   * @param c the color for the highlight
   * @param l2r 
   * @param textpage 
   */
  public CommitHighlightPainter(Color c, WSTextEditorPage textpage, Map<Integer, RevCommit> l2r, Supplier<RevCommit> activeCommit) {
    color = c;
    this.textpage = textpage;
    this.l2r = l2r;
    this.activeCommit = activeCommit;
  }


  /**
   * Returns the color of the highlight.
   *
   * @return the color
   */
  private Color getColor() {
    return color;
  }

  // --- HighlightPainter methods ---------------------------------------

  /**
   * Paints a highlight.
   *
   * @param g the graphics context
   * @param offs0 the starting model offset &gt;= 0
   * @param offs1 the ending model offset &gt;= offs1
   * @param bounds the bounding box for the highlight
   * @param c the editor
   */
  public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
    Rectangle alloc = bounds.getBounds();
    try {
      // --- determine locations ---
      TextUI mapper = c.getUI();
      Rectangle p0 = mapper.modelToView(c, offs0);
      Rectangle p1 = mapper.modelToView(c, offs1);

      // --- render ---
      Color color = getColor();

      if (color == null) {
        g.setColor(c.getSelectionColor());
      }
      else {
        g.setColor(color);
      }
      if (p0.y == p1.y) {
        // same line, render a rectangle
        Rectangle r = p0.union(p1);
        g.fillRect(r.x, r.y + 1, r.width, r.height - 2);
      } else {
        // different lines
        int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
        g.fillRect(p0.x, p0.y + 1, p0ToMarginWidth, p0.height - 2);
        if ((p0.y + p0.height) != p1.y) {
          g.fillRect(alloc.x, p0.y + p0.height + 1, alloc.width,
              p1.y - (p0.y + p0.height) - 2);
        }
        g.fillRect(alloc.x, p1.y + 1, (p1.x - alloc.x), p1.height - 2);
      }
    } catch (BadLocationException e) {
      // can't render
    }
  }

  // --- LayerPainter methods ----------------------------
  /**
   * Paints a portion of a highlight.
   *
   * @param g the graphics context
   * @param offs0 the starting model offset &gt;= 0
   * @param offs1 the ending model offset &gt;= offs1
   * @param bounds the bounding box of the view, which is not
   *        necessarily the region to paint.
   * @param c the editor
   * @param view View painting for
   * @return region drawing occurred in
   */
  public Shape paintLayer(Graphics g, int offs0, int offs1,
      Shape bounds, JTextComponent c, View view) {
    Color color = getColor();

    if (color == null) {
      g.setColor(c.getSelectionColor());
    }
    else {
      g.setColor(color);
    }

    Rectangle r;

    if (offs0 == view.getStartOffset() &&
        offs1 == view.getEndOffset()) {
      // Contained in view, can just use bounds.
      if (bounds instanceof Rectangle) {
        r = (Rectangle) bounds;
      }
      else {
        r = bounds.getBounds();
      }
    }
    else {
      // Should only render part of View.
      try {
        // --- determine locations ---
        Shape shape = view.modelToView(offs0, Position.Bias.Forward,
            offs1,Position.Bias.Backward,
            bounds);
        r = (shape instanceof Rectangle) ?
            (Rectangle)shape : shape.getBounds();
      } catch (BadLocationException e) {
        // can't render
        r = null;
      }
    }

    if (r != null) {
      // If we are asked to highlight, we should draw something even
      // if the model-to-view projection is of zero width (6340106).
      r.width = Math.max(r.width, 1);

      int delta = getYDelta(offs0, g);

      g.fillRect(r.x, r.y + delta, r.width, r.height - delta);
    }

    return r;
  }

  /**
   * A small correction on the Y axis. If the line that contains this offset has a different revision than the previous line then we 
   * leave a gap between them.
   * 
   * @param offset Offset to check.
   * @param g Graphics.
   * 
   * @return A correction for the Y axis.
   */
  private int getYDelta(int offset, Graphics g) {
    int delta = 0;

    try {
      int lineOfOffset = textpage.getLineOfOffset(offset);

      int key = lineOfOffset - 1;
      RevCommit revCommit = l2r.get(key);
      if (lineOfOffset > 1) {
        // Not the first line.
        RevCommit prevRevCommit = l2r.get(key - 1);

        boolean same = Equaler.verifyEquals(revCommit, prevRevCommit);
        if (!same) {
          delta = 1;
        }
      }

      RevCommit currentRevCommit = activeCommit.get();
      if (!Equaler.verifyEquals(currentRevCommit, revCommit)) {
        // Bleach it a bit.
        Color color2 = getColor();
        Color bleached = new Color((float) color2.getRed() / 255, (float) color2.getGreen() / 255, (float) color2.getBlue() / 255, (float )0.1);
        g.setColor(bleached);
      }
    } catch (BadLocationException e) {
      LOGGER.error(e, e);
    }

    return delta;
  }
}