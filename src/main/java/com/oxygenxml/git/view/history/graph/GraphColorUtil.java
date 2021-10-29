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
			Color.RED, 
			Color.MAGENTA, 
			Color.PINK, 
			Color.DARK_GRAY, 
			Color.ORANGE 
	};
	
	/**
	 * Lines commits colors for dark theme.
	 */
	private static final Color[] DARK_THEME_EDGES_COLORS = {
			Color.RED,
			Color.BLUE, 
			Color.YELLOW, 
			Color.GREEN, 
			Color.LIGHT_GRAY, 
			Color.ORANGE 
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
