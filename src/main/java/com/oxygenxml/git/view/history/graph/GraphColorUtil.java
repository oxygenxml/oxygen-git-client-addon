package com.oxygenxml.git.view.history.graph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages colors for Commits Graph.
 * 
 * @author alex_smarandache
 *
 */
public class GraphColorUtil {
	
	/**
	 * Hidden constructor.
	 */
	private GraphColorUtil() {
		// nothing
	}
	
	
	/**
	 * Commit dot default color.
	 */
	public static final Color COMMIT_LINE_DEFAULT_COLOR = Color.LIGHT_GRAY;
	
	
	/**
	 * Factory for ColorDispatcher.
	 * 
	 * @param isDarkTheme <code>true</code> if is a color dispatcher for dark theme. 
	 * 
	 * @return The created color dispatcher.
	 */
	public static ColorPool createColorDispatcher(boolean isDarkTheme) {
		// A list with all colors of graph.
		final List<Color> allColors = new ArrayList<>();
		
		if(isDarkTheme) {
			Color[] darkThemeEdgesColors = {
					new Color(197, 92, 81),    // RED
					new Color(59, 187, 114),   // GREEN
					new Color(79, 154, 205),   // BLUE
					new Color(186, 94, 186),   // MAUVE
					new Color(199, 171, 59),   // ORANGE
					new Color(57, 176, 169)    // TURQUOISE
			};
			allColors.addAll(Arrays.asList(darkThemeEdgesColors));
		} else {
			Color[] lightThemeEdgesColors = {
					new Color(231,76,60),      // RED
					new Color(46,204,113),     // GREEN
					new Color(52, 152, 219),   // BLUE
					new Color(193, 54, 193),   // MAUVE
					new Color(241, 196, 15),   // ORANGE
					new Color(44, 214, 204)    // TURQUOISE
			};
			allColors.addAll(Arrays.asList(lightThemeEdgesColors));

		}
		
		// A list containing colors that have not yet been used or can be reused.
		final LinkedList<Color> availableColors = new LinkedList<>(allColors); 
		
		
		return new ColorPool() {	
			
			/**
			 * Reset the list to have all colors available.
			 */
			private void resetColors() {
				availableColors.addAll(allColors); 
			}
			
			@Override
			public Color aquireColor() {
				if (availableColors.isEmpty()) {
					resetColors(); 
				}
				return availableColors.removeFirst();
			}
			
			@Override
			public void releaseColor(Color color) {
				availableColors.add(color);
			}
		};
	}

}
