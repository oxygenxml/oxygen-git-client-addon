package com.oxygenxml.git.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import junit.extensions.jfcunit.JFCTestCase;
import ro.sync.annotations.api.API;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
   * Delimiter between the imported class and the class that imports it.
   */
  private static final String DELIMITER = " ~IS IMPORTED BY~ ";
  
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
   * JOptionPane exceptions.
   */
  private static final ImmutableSet<String> J_OPTION_PANE_EXCEPTIONS =
      ImmutableSet.of("HistoryViewContextualMenuPresenter.java");

  
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
    Set<String> importedOxyClasses = new HashSet<>();
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
            importedOxyClasses.add(classQName + DELIMITER + file);
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
    for (String importedClass : importedOxyClasses) {
      String importedClassQName = importedClass.substring(0, importedClass.indexOf(DELIMITER));
      Class<?> clazz = classLoader.loadClass(importedClassQName);
      API[] annotationsByType = clazz.getAnnotationsByType(API.class);
      if (annotationsByType.length == 0) {
        classesToReport.add(importedClass);
      } else {
        for (API api : annotationsByType) {
          if (api.toString().contains("INTERNAL")) {
            if (!importedClass.contains("ro.sync.exml.workspace.api.standalone.ui.Button")) {
              // The Button API was erroneously annotated as private, but this was fixed in 22.0 API
              classesToReport.add(importedClass);
            }
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
  
  /**
   * <p><b>Description:</b> use the confirmation, information, warning and error messages
   * from {@link StandalonePluginWorkspace} instead of {@link JOptionPane}.</p>
   * <p><b>Bug ID:</b> EXM-44205</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testDontUseJOptionPane() throws Exception {
    Set<String> classesToReport = new HashSet<>();

    // Get all the "java.swing.JOptionPane" imports
    BufferedReader br = null;
    while (srcFilesIterator.hasNext()) {
      File file = srcFilesIterator.next();
      if (!J_OPTION_PANE_EXCEPTIONS.contains(file.getName())) {
        try {
          br = new BufferedReader(new FileReader(file));
          String line = "";
          while ((line = br.readLine()) != null && !shouldStopSearchingForImports(line)) {
            if (line.startsWith("import javax.swing.JOptionPane")) {
              String classQName = line.substring(
                  line.indexOf("import ") + "import ".length(),
                  line.indexOf(';'));
              classesToReport.add(classQName + DELIMITER + file);
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
    }

    StringBuilder sb = new StringBuilder();
    for (String classToReport : classesToReport) {
      sb.append(classToReport + "\n");
    }

    assertEquals("Use the confirmation, information, warning and error messages from "
        + "StandalonePluginWorkspace instead of JOptionPane. JOptionPane usages have been found in: ",
        "",
        sb.toString());
  }
  
}
