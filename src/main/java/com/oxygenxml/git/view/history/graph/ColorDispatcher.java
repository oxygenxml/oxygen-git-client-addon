package com.oxygenxml.git.view.history.graph;

import java.awt.Color;

/**
 * Useful for managing colors.
 * 
 * @author alex_smarandache
 *
 */
public interface ColorDispatcher {
	
	/**
	 * Add a new color for available colors.
	 * 
	 * @param color
	 */
	public void aquireColor(Color color);
	
	
	/**
	 * Returns the next color that is available.
	 * 
	 * @return The color.
	 */
	public Color releaseColor();

}
