package com.oxygenxml.git.view.history.graph;

import java.awt.Color;

import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;

/**
 * A list of commits that is being rendered visualy using different colors for each commit lane.
 * 
 * @author alex_smarandache
 *
 */
public class VisualCommitsList extends PlotCommitList<VisualCommitsList.VisualLane> {  

	/**
	 * Manages colors.
	 */
	private final ColorDispatcher colorDispatcher; 


	
	/**
	 * Constructor.
	 * 
	 * @param <code>true</code> if is the dark theme.
	 */
	public VisualCommitsList(ColorDispatcher colorDispatcher) {
		 this.colorDispatcher = colorDispatcher;
	} 
	
	
	@Override 
	protected VisualLane createLane() { 
		final VisualLane lane = new VisualLane(); 
		lane.color = colorDispatcher.releaseColor(); 
		return lane; 
	} 

	
	@Override 
	protected void recycleLane(final VisualLane lane) { 
		colorDispatcher.aquireColor(lane.color);
	} 


	/**
	 * 
	 * A visual colored line.
	 * 
	 * @author alex_smarandache
	 *
	 */
	@SuppressWarnings("serial")
	public static class VisualLane extends PlotLane { 
		transient Color color;

		@Override
		public String toString() {
			return "VisualLane [color=" + color + "]";
		}
	} 
	
}
