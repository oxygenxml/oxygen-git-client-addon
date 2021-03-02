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
   * Alpha value for highlight color.
   */
  private static final float HIGHLIGHT_COLOR_ALPHA = (float) 0.1;
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
    Rectangle alloc = bounds.getBounds();
    try {
      // --- determine locations ---
      TextUI mapper = textComp.getUI();
      Rectangle p0 = mapper.modelToView(textComp, startOffset);
      Rectangle p1 = mapper.modelToView(textComp, endOffset);

      // --- render ---
      Color paintColor = getColor();
      if (paintColor == null) {
        g.setColor(textComp.getSelectionColor());
      } else {
        g.setColor(paintColor);
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
      LOGGER.error(e, e);
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
          (float) c.getRed() / 255,
          (float) c.getGreen() / 255,
          (float) c.getBlue() / 255,
          HIGHLIGHT_COLOR_ALPHA);
      g.setColor(bleached);
    }
  }
}