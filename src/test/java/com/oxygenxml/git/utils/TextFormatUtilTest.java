package com.oxygenxml.git.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Contains tests for TextFormatUtil class.
 * 
 * @author Alex_Smarandache
 *
 */
public class TextFormatUtilTest {

  
 /**
  * Tests method for convert text to HTML.
  */
 @Test
 public void testToHtml() {
   assertEquals("<html>test</html>", TextFormatUtil.toHTML("test"));
   assertEquals("<html>test</html>", TextFormatUtil.toHTML("<html>test</html>"));
 }
 
 
 /**
  * Tests method for shorten text.
  * 
  * @author Alex_Smarandache
  */
 @Test
 public void testShortenText() {
   assertEquals("123yes9", TextFormatUtil.shortenText("123456789", 3, 1, "yes"));
   assertEquals("1239", TextFormatUtil.shortenText("123456789", 3, 1, ""));
   assertEquals("123456789", TextFormatUtil.shortenText("123456789", 10, 1, "yes"));
   assertEquals("yes23456789", TextFormatUtil.shortenText("123456789", 0, 8, "yes"));
   assertNull(TextFormatUtil.shortenText(null, 0, 8, "yes"));
 }

}
