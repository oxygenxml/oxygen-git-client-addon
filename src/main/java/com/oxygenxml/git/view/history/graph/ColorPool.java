package com.oxygenxml.git.view.history.graph;

import java.awt.Color;

/**
 * Useful for managing colors.
 * 
 * @author alex_smarandache
 *
 */
public interface ColorPool {
	
	/**
	 * A color that was used is returned to the pool. A subsequent call to
	 *  {@link #aquireColor()} might return this color.
	 * 
	 * @param color A color to add to the pool.
	 */
	public void releaseColor(Color color);
	
	
	/**
	 * Pops a color from the pull and returns it. This color will not be used until
	 * it is returned to the pool by a {@link #releaseColor(Color)} call.
	 * 
	 * @return The color.
	 */
	public Color aquireColor();

}
