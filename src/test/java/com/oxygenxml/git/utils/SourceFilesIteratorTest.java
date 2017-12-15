package com.oxygenxml.git.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.annotations.api.API;

public class SourceFilesIteratorTest extends JFCTestCase {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(SourceFilesIteratorTest.class.getName());
  
  /**
   * Source code folder.
   */
  public static final File SRC_DIR = new File("src");
  
  /**
   * The iterator over the java source files.
   */
  private JavaFilesIterator srcFilesIterator = new JavaFilesIterator(SRC_DIR);
  
  /**
   * @return <code>true</code> if should stop looking for imports in a file.
   */
  private boolean shouldStopSearchingForImports(String line) {
    return line.contains("class ") 
        || line.contains("enum ") 
        || line.contains("interface ");
  }
  
  /**
   * <p><b>Description:</b> test that the oXygen Git plug-in doesn't use
   * not obfuscated, but also non-API classes. It should only use API classes.</p>
   * <p><b>Bug ID:</b> EXM-40653</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testDontUseNonAPIClasses() throws Exception {
    Set<String> classesToReport = new HashSet<>();
    
    // Get all the "ro.sync" imports
    BufferedReader br = null;
    Set<String> importedOxyClasses = new HashSet<String>();
    while (srcFilesIterator.hasNext()) {
      File file = srcFilesIterator.next();
      try {
        br = new BufferedReader(new FileReader(file));
        String line = "";
        while ((line = br.readLine()) != null && !shouldStopSearchingForImports(line)) {
          if (line.startsWith("import ro.sync.")) {
            String classQName = line.substring(
                line.indexOf("import ") + "import ".length(),
                line.indexOf(';'));
            importedOxyClasses.add(classQName);
          }
        }
      } catch (FileNotFoundException e) {
        logger.error(e, e);
      } finally {
        try {
          br.close();
        } catch (IOException e) {
          logger.error(e, e);
        }
      }
    }
    
    // Check whether the imported classes are public API or not
    ClassLoader classLoader = SourceFilesIteratorTest.class.getClassLoader();
    for (String classQName : importedOxyClasses) {
      Class<?> clazz = classLoader.loadClass(classQName);
      API[] annotationsByType = clazz.getAnnotationsByType(API.class);
      if (annotationsByType.length == 0) {
        classesToReport.add(classQName);
      } else {
        for (API api : annotationsByType) {
          if (api.toString().contains("INTERNAL")) {
            classesToReport.add(classQName);
            break;
          }
        }
      }
    }
    
    StringBuilder sb = new StringBuilder();
    for (String classToReport : classesToReport) {
      sb.append(classToReport + "\n");
    }
    
    assertEquals("Only public API should be used, but the following inadequte classes were also imported ",
        "",
        sb.toString());
  }
  
}
