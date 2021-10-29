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
			new Color(231,76,60), 
			new Color(46,204,113),
			new Color(52, 152, 219), 
			new Color(193, 54, 193),
			new Color(241, 196, 15),
			new Color(44, 214, 204)
	};
	
	/**
	 * Lines commits colors for dark theme.
	 */
	private static final Color[] DARK_THEME_EDGES_COLORS = {
			new Color(188, 79, 68), 
			new Color(58, 169, 105),
			new Color(62, 133, 180), 
			new Color(161, 64, 161),
			new Color(195,164,36),
			new Color(57, 176, 169)
	};
	
	public static Color BACKGROUND = Color.BLACK;
	
	/**
	 * Commit dot color on light theme.
	 */
	public static final Color COMMIT_DOT_COLOR_LIGHT_THEME = new Color(30,144,255);
	
	/**
	 * Commit dot color on dark theme.
	 */
	public static final Color COMMIT_DOT__COLOR_DARK_THEME = Color.WHITE;
	
	/**
	 * Commit dot default color.
	 */
	public static final Color COMMIT_DOT_DEFAULT_COLOR = Color.GRAY;
	
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

}
