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

}
