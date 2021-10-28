package com.oxygenxml.git.view.history.graph;
 
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

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
  * The 'X' cell coordinate. 
  */
 private int cellX; 
 
 /**
  * The 'Y' cell coordinate. 
  */
 private int cellY; 
 
 /**
  * Cell foreground. 
  */
 private Color cellFG; 
 
 /**
  * Cell background.
  */
 private Color cellBG; 
 
 /**
  * The line level.
  */
 int lineLevel;
 
 /**
  * A map with colors for points levels.
  */
 private static final Map<Integer, Color> colors = new HashMap<>();
 static {
  colors.put(0, Color.CYAN);
  colors.put(1,  Color.RED);
  colors.put(2,  Color.LIGHT_GRAY);
  colors.put(3,  Color.GREEN);
  colors.put(4,  Color.BLUE);
 }
 
 
 
 /**
  * Constructor.
  * 
  * @param cellX   The 'X' cell coordinate. 
  * @param cellY   The 'Y' cell coordinate. 
  * @param cellFG  Cell foreground. 
  * @param cellBG  Cell background. 
  */
 public GraphRender(int cellX, int cellY, Color cellFG, Color cellBG) {
	super();
	this.cellX = cellX;
	this.cellY = cellY;
	this.cellFG = cellFG;
	this.cellBG = cellBG;
}

 
 /**
  * Constructor.
  */
 public GraphRender() {
	 
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
	 this.lineLevel = commit.getLane().getPosition();
	 paintCommit(commit, height);
 }
 
 
 protected void drawLine(final Color color, final int x1, final int y1, 
   final int x2, final int y2, final int width) { 
  g.setColor(Color.BLACK);
  Stroke stroke = g.getStroke();
  g.setStroke(new BasicStroke(width));
  g.drawLine(x1, y1, x2, y2);
  g.setStroke(stroke);
 } 
 
 
 protected void drawCommitDot(final int x, final int y, final int w, 
   final int h) { 
	 g.setColor(Color.BLACK);
	 g.drawOval(x, y, w, h);
	 g.setColor(colors.containsKey(lineLevel) ? colors.get(lineLevel) : Color.GRAY);
	 g.fillOval(x, y, w, h);
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


