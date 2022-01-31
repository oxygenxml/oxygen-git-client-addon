package com.oxygenxml.git.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtil {

  /**
   * Logger for logging.
   */
  private static Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

  /**
   * Private constructor.
   * 
   * @throws UnsupportedOperationException when invoked.
   */
  private LoggingUtil() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }
  
  /**
   * Redirect JSch logging to our instance.
   */
  public static void setupLogger() {
    // Redirect JSch logging to our logging instance.
    com.jcraft.jsch.JSch.setLogger(new com.jcraft.jsch.Logger() {
      @Override
      public void log(int level, String message) {
        if (logger.isDebugEnabled()) {
          logger.debug(message);
        }
      }
      @Override
      public boolean isEnabled(int level) {
        return logger.isDebugEnabled();
      }
    });
  }
}
