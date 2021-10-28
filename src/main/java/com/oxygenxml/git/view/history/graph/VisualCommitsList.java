package com.oxygenxml.git.view.history.graph;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;

/**
 * A list of commits to be represented visually.
 * 
 * @author alex_smarandache
 *
 */
public class VisualCommitsList extends PlotCommitList<VisualCommitsList.VisualLane> { 
	/**
	 * All commits colors.
	 */
	private final List<Color> allColors; 

	/**
	 * All available commits colors.
	 */
	private final LinkedList<Color> availableColors; 


	
	/**
	 * Constructor.
	 */
	public VisualCommitsList() { 
		this(false);
	} 

	
	/**
	 * Constructor.
	 * 
	 * @param <code>true</code> if is the dark theme.
	 */
	public VisualCommitsList(boolean isDarkTheme) {
		allColors = isDarkTheme ? GraphColorUtil.getEdgesColorsForDarkTheme() : 
			GraphColorUtil.getEdgesColorsForLightTheme(); 
		availableColors = new LinkedList<>(); 
		resetColors(); 
	} 
	
	
	/**
	 * Reset all colors.
	 */
	private void resetColors() { 
		availableColors.addAll(allColors); 
	} 

	
	@Override 
	protected VisualLane createLane() { 
		final VisualLane lane = new VisualLane(); 
		if (availableColors.isEmpty()) {
			resetColors(); 
		}
		lane.color = availableColors.removeFirst(); 
		return lane; 
	} 

	
	@Override 
	protected void recycleLane(final VisualLane lane) { 
		availableColors.add(lane.color); 
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
