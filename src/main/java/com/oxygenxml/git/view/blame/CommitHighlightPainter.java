package com.oxygenxml.git.view.blame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.Position.Bias;
import javax.swing.text.View;

import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.utils.Equaler;

import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

/**
 * Simple highlight painter that fills a highlighted area with
 * a solid color.
 */
public class CommitHighlightPainter extends LayeredHighlighter.LayerPainter {
  /**
   * Maximum value for a color.
   */
  private static final int MAX_COLOR_VALUE_255 = 255;
  /**
   * Alpha value for highlight color.
   */
  private static final float HIGHLIGHT_COLOR_ALPHA = (float) 0.1;
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CommitHighlightPainter.class);
  /**
   * Text page on which the painter is installed.
   */
  private WSTextEditorPage textpage;
  /**
   * Mapping between lines and revisions in which that line was last changed.
   */
  private Map<Integer, RevCommit> lineToRevCommitMap;
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
   * @param color               The color for the highlight
   * @param lineToRevCommitMap  Mapping between lines and revisions in which that line was last changed.
   * @param textpage            The text page.
   * @param activeCommit        The commit under the caret.
   */
  public CommitHighlightPainter(Color color, WSTextEditorPage textpage,
      Map<Integer, RevCommit> lineToRevCommitMap , Supplier<RevCommit> activeCommit) {
    this.color = color;
    this.textpage = textpage;
    this.lineToRevCommitMap = lineToRevCommitMap;
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

  /**
   * Paints a highlight.
   *
   * @param g the graphics context
   * @param startOffset the starting model offset &gt;= 0
   * @param endOffset the ending model offset &gt;= offs1
   * @param bounds the bounding box for the highlight
   * @param textComp the editor
   */
  @Override
  public void paint(Graphics g, int startOffset, int endOffset, Shape bounds, JTextComponent textComp) {
    try {
      Graphics2D g2 = (Graphics2D) g;
      
      Rectangle alloc = bounds.getBounds();
      
      // --- determine locations ---
      TextUI mapper = textComp.getUI();
      Rectangle2D p0 = mapper.modelToView2D(textComp, startOffset, Bias.Forward);
      Rectangle2D p1 = mapper.modelToView2D(textComp, endOffset, Bias.Forward);

      // --- render ---
      Color paintColor = getColor();
      if (paintColor == null) {
        g2.setColor(textComp.getSelectionColor());
      } else {
        g2.setColor(paintColor);
      }
      
      Rectangle2D rectToFill;
      if (p0.getY() == p1.getY()) {
        // same line, render a rectangle
        Rectangle2D r = new Rectangle2D.Double();
        Rectangle2D.union(p0, p1, r);
        rectToFill = new Rectangle2D.Double(r.getX(), r.getY() + 1, r.getWidth(), r.getHeight() - 2);
        g2.fill(rectToFill);
      } else {
        // different lines
        double p0ToMarginWidth = alloc.x + alloc.width - p0.getX();
        rectToFill = new Rectangle2D.Double(p0.getX(), p0.getY() + 1, p0ToMarginWidth, p0.getHeight() - 2);
        g2.fill(rectToFill);
        if ((p0.getY() + p0.getHeight()) != p1.getY()) {
          rectToFill = new Rectangle2D.Double(
              alloc.x,
              p0.getY() + p0.getHeight() + 1,
              alloc.width,
              p1.getY() - (p0.getY() + p0.getHeight()) - 2);
          g2.fill(rectToFill);
        }
        rectToFill = new Rectangle2D.Double(
            alloc.x,
            p1.getY() + 1,
            (p1.getX() - alloc.x),
            p1.getHeight() - 2);
        g2.fill(rectToFill);
      }
    } catch (BadLocationException e) {
      // can't render
    }
  }

  /**
   * Paints a portion of a highlight.
   *
   * @param g the graphics context
   * @param stOffs the starting model offset &gt;= 0
   * @param endOffs the ending model offset &gt;= offs1
   * @param bounds the bounding box of the view, which is not
   *        necessarily the region to paint.
   * @param textComp the editor
   * @param view View painting for
   * @return region drawing occurred in
   */
  @Override
  public Shape paintLayer(Graphics g, int stOffs, int endOffs,
      Shape bounds, JTextComponent textComp, View view) {
    Color paintColor = getColor();
    if (paintColor == null) {
      g.setColor(textComp.getSelectionColor());
    } else {
      g.setColor(paintColor);
    }

    Rectangle rect;

    if (stOffs == view.getStartOffset() && endOffs == view.getEndOffset()) {
      // Contained in view, can just use bounds.
      if (bounds instanceof Rectangle) {
        rect = (Rectangle) bounds;
      } else {
        rect = bounds.getBounds();
      }
    } else {
      // Should only render part of View.
      try {
        // --- determine locations ---
        Shape shape = view.modelToView(stOffs, Position.Bias.Forward,
            endOffs,Position.Bias.Backward,
            bounds);
        rect = (shape instanceof Rectangle) ?
            (Rectangle)shape : shape.getBounds();
      } catch (BadLocationException e) {
        // can't render
        rect = null;
      }
    }

    if (rect != null) {
      // If we are asked to highlight, we should draw something even
      // if the model-to-view projection is of zero width (6340106).
      rect.width = Math.max(rect.width, 1);

      int delta = getYDelta(stOffs, g);

      g.fillRect(rect.x, rect.y + delta, rect.width, rect.height - delta);
    }

    return rect;
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
      RevCommit revCommit = lineToRevCommitMap.get(key);
      if (lineOfOffset > 1) {
        // Not the first line.
        RevCommit prevRevCommit = lineToRevCommitMap.get(key - 1);

        boolean same = Equaler.verifyEquals(revCommit, prevRevCommit);
        if (!same) {
          delta = 1;
        }
      }

      setCommitColor(g, revCommit);
    } catch (BadLocationException e) {
      LOGGER.error(e.getMessage(), e);
    }

    return delta;
  }


  /**
   * Set the commit color.
   * 
   * @param g          Graphics.
   * @param revCommit  Commit.
   */
  private void setCommitColor(Graphics g, RevCommit revCommit) {
    RevCommit currentRevCommit = activeCommit.get();
    if (!Equaler.verifyEquals(currentRevCommit, revCommit)) {
      // Bleach it a bit.
      Color c = getColor();
      Color bleached = new Color(
          (float) c.getRed() / MAX_COLOR_VALUE_255,
          (float) c.getGreen() / MAX_COLOR_VALUE_255,
          (float) c.getBlue() / MAX_COLOR_VALUE_255,
          HIGHLIGHT_COLOR_ALPHA);
      g.setColor(bleached);
    }
  }
}