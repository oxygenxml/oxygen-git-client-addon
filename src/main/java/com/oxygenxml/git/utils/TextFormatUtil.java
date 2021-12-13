package com.oxygenxml.git.utils;

/**
 * Contains utils methods to format the text.
 * 
 * @author Alex_Smarandache
 *
 */
public class TextFormatUtil {

  private TextFormatUtil() {
    // nothing
  }
  
  /**
   * Convert the text to HTML text. <br><br>
   * 
   * @param content The content text
   * 
   * @return The HTML text;
   */
  public static String toHTML(String content) {
    return content.startsWith("<html>") ? content : "<html>" + content + "</html>";
  }
  
  /**
   * Shorten text.
   * <br><br>
   * For example, for ("hamburger", 2, 3, "...") -> ha...ger.
   * 
   * @param text                       Text to shorten.
   * @param firstCharactersToKeep      Index for the first characters to keep in shorten text.
   * @param lastCharactersToKeep       Index for the last characters to keep in shorten text.
   * @param addBetweenCharacters       A String that can be inserted between first and last characters. 
   * 
   * @return The computed text.
   */
  public static String shortenText(final String text, int firstCharactersToKeep, int lastCharactersToKeep, final String addBetweenCharacters) {
	  String toReturn = text;
	  if(text.length() > (firstCharactersToKeep + lastCharactersToKeep)) {
		  toReturn = text.substring(0, firstCharactersToKeep) + addBetweenCharacters + text.substring(text.length() - lastCharactersToKeep, text.length());
	  }
	  return toReturn;
  }

}
