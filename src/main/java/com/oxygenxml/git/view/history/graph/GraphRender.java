package com.oxygenxml.git.view.history.graph;
 
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revplot.AbstractPlotRenderer;
import org.eclipse.jgit.revplot.PlotCommit;

import com.oxygenxml.git.view.history.graph.VisualCommitsList.VisualLane;

/**
 * 
 * Render for commits graph in GitHistory.
 * <br>
 * Is responsible for painting the graph for a commit.
 *  
 * @author alex_smarandache
 * 
 */
public class GraphRender extends AbstractPlotRenderer<VisualCommitsList.VisualLane, Color> { 
 
/**
 * Graphics for paint.
 */
 private Graphics2D g; 
 
 /**
  * The commit dot color.
  */
 private final Color commitDotColor; 
 
 
 
 /**
  * Constructor.
  */
 public GraphRender() {
	 this(Color.WHITE);
 }
 
 
 /**
  * Constructor.
  */
 public GraphRender(Color commitDotColor) {
	 this.commitDotColor = commitDotColor;
 }
 
 
/**
  * Paints the part of the graph specific to a commit.
  * 
  * @param commit  The commit to paint. Must not be null.
  * @param height  Total height (in pixels) of this cell.   
  * @param g       The graphics.
  */
 public void paint(PlotCommit<VisualCommitsList.VisualLane> commit, int height, Graphics2D g) {
	 this.g = g;
	 paintCommit(commit, height);
 }
 
 
 protected void drawLine(final Color color, final int x1, final int y1, 
   final int x2, final int y2, final int width) { 
  g.setColor(color);
  g.setStroke(new BasicStroke(width));
  g.drawLine(x1, y1, x2, y2);
 } 
 
 
 protected void drawCommitDot(final int x, final int y, final int w, 
   final int h) { 
	 g.setColor(commitDotColor);
	 g.fillOval(x, y, w, h); 
	 g.setStroke(new BasicStroke(2)); 
	 g.drawOval(x + 1, y + 1, w - 2, h - 2); 
	 g.setStroke(new BasicStroke(1));
	 g.drawOval(x, y, w, h); 
 } 
 
 
 protected void drawBoundaryDot(final int x, final int y, final int w, 
   final int h) { 
	// not needed
 } 
 
 
 @Override
 protected void drawText(final String msg, final int x, final int y) { 
	 // not needed
 } 
 
 
 @Override 
 protected int drawLabel(int x, int y, Ref ref) { 
	 //not needed
  return 0;
 } 
 
 
 protected Color laneColor(final VisualLane myLane) {
  return myLane != null ? myLane.color : Color.RED; 
 }
       
}


