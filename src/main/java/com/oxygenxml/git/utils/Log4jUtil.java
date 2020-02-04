package com.oxygenxml.git.utils;

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jgit.util.FS;

public class Log4jUtil {

  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(Log4jUtil.class);

  private Log4jUtil() {}
  
  /**
   * Redirect logging to the Log4J instance and install filters to ignore certain events.
   */
  public static void setupLog4JLogger() {
    // Redirect logging to log4j instance.
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
    
    addFiltersOnAppenders(Logger.getRootLogger().getAllAppenders());
  }

  /**
   * EXM-44131 Filter an exception that we know it appears quite often and has no repercussions.
   * 
   * @param allAppenders The Log4j active appenders.
   */
  private static void addFiltersOnAppenders(Enumeration allAppenders) {
    while (allAppenders.hasMoreElements()) {
      Appender appender = (Appender) allAppenders.nextElement();
      
      appender.addFilter(new Filter() {
        @Override
        public int decide(LoggingEvent event) {
          int toReturn = NEUTRAL;
          if(FS.class.getName().equals(event.getLoggerName()) && 
              event.getLevel() == Level.ERROR) {
            Pattern pattern = Pattern.compile("^java.security.AccessControlException:\\s+access denied\\s+\\(\"java\\.io\\.FilePermission\".*\\.probe");
            
            Matcher matcher = pattern.matcher(event.getRenderedMessage());
            
            if (matcher.find()) {
              toReturn = DENY;
            }
          }
          
          return toReturn;
        }
      });
    }
  }
}
