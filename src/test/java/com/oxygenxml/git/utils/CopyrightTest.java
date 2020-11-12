package com.oxygenxml.git.utils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.Year;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class CopyrightTest extends TestCase {

  /**
   * <p><b>Description:</b> test copyright year in README.</p> 
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testCopyrightYearInReadme() throws Exception {
    try (Reader reader = new FileReader(new File("README.md"))) {
      String readmeContent = IOUtils.readLines(reader).toString();
      assertTrue(readmeContent.contains("Copyright " + Year.now().getValue() + " Syncro Soft SRL"));
    }
  }
  
}
