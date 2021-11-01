package com.oxygenxml.git.view.history.graph;

import java.awt.Color;
import java.util.Arrays;
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
	 * Lines commits colors for light theme.
	 */
	private static final Color[] LIGHT_THEME_EDGES_COLORS = {
			new Color(231,76,60),      // RED
			new Color(46,204,113),     // GREEN
			new Color(52, 152, 219),   // BLUE
			new Color(193, 54, 193),   // MAUVE
			new Color(241, 196, 15),   // ORANGE
			new Color(44, 214, 204)    // TURQUOISE
	};
	
	/**
	 * Lines commits colors for dark theme.
	 */
	private static final Color[] DARK_THEME_EDGES_COLORS = {
			new Color(197, 92, 81),    // RED
			new Color(59, 187, 114),   // GREEN
			new Color(79, 154, 205),   // BLUE
			new Color(186, 94, 186),   // MAUVE
			new Color(199, 171, 59),   // ORANGE
			new Color(57, 176, 169)    // TURQUOISE
	};
	
	/**
	 * Backgound cell color.
	 */
	private static Color background = Color.BLACK;
	
	/**
	 * Commit dot default color.
	 */
	public static final Color COMMIT_LINE_DEFAULT_COLOR = Color.LIGHT_GRAY;
	
	/**
	 * @return A list with commits lines colors for light theme.
	 */
	public static List<Color> getEdgesColorsForLightTheme() {
		return Arrays.asList(LIGHT_THEME_EDGES_COLORS);
	}
	
	/**
	 * @return A list with commits lines colors for dark theme.
	 */
	public static List<Color> getEdgesColorsForDarkTheme() {
		return Arrays.asList(DARK_THEME_EDGES_COLORS);
	}

	/**
	 * Get background color.
	 * 
	 * @return The background color.
	 */
	public static Color getBackground() {
		return background;
	}

	/**
	 * Setter for background color.
	 * 
	 * @param background The new color for background.
	 */
	public static void setBackground(Color background) {
		GraphColorUtil.background = background;
	}

	
}
