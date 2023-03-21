package com.oxygenxml.git.view.history.graph;

import java.awt.Color;

import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;

/**
 * A list of commits that is being rendered visually using different colors for each commit lane.
 * 
 * @author alex_smarandache
 *
 */
@SuppressWarnings("java:S2160")
public class VisualCommitsList extends PlotCommitList<VisualCommitsList.VisualLane> {  

	/**
	 * Manages colors.
	 */
	private final ColorPool colorDispatcher; 


	
	/**
	 * Constructor.
	 * 
	 * @param <code>true</code> if is the dark theme.
	 */
	public VisualCommitsList(ColorPool colorDispatcher) {
		 this.colorDispatcher = colorDispatcher;
	} 
	
	
	@Override 
	protected VisualLane createLane() { 
		final VisualLane lane = new VisualLane(); 
		lane.color = colorDispatcher.aquireColor(); 
		return lane; 
	} 

	
	@Override 
	protected void recycleLane(final VisualLane lane) { 
		colorDispatcher.releaseColor(lane.color);
	} 


	/**
	 * 
	 * A visual colored line.
	 * 
	 * @author alex_smarandache
	 *
	 */
	public static class VisualLane extends PlotLane { 
		transient Color color;

		@Override
		public String toString() {
			return "VisualLane [color=" + color + "]";
		}
	} 
	
}
