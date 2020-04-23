package com.oxygenxml.git.utils;

import java.lang.reflect.Method;
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
    
    if (isLog4jVersion2_OrLater()) {
      addFiltersOnAppenders();
    } else {
      addFiltersOnAppenders_Log4j1_2();
    }
  }
  
  /**
   * @return <code>true</code> if logger 2 or later is present in classpath.
   */
  private static boolean isLog4jVersion2_OrLater() {
    boolean isVersion2OrLater = false;
    try {
      Class.forName("org.apache.logging.log4j.core.LoggerContext");
      isVersion2OrLater = true;
    } catch(Throwable t) {
      logger.debug(t, t);
    }
    
    return isVersion2OrLater;
  }
  
  /**
   * EXM-44131 Filter an exception that we know it appears quite often and has no repercussions.
   */
  private static void addFiltersOnAppenders() {
    // We want to be compatible with versions of Oxygen prior to 2.13.0
    // Because of that we use reflection for invoking what's in the commented lines below.
    
//    LoggerContext logContext = LoggerContext.getContext(false);
//    LoggerConfig rootLoggerConfig = logContext.getConfiguration().getRootLogger();
//    rootLoggerConfig.addFilter(new FilterAdapter(createSecurityExceptionFilter));
    
    try {
      Class<?> loggerContextClass = Class.forName("org.apache.logging.log4j.core.LoggerContext");
      Method loggerContextMethod = loggerContextClass.getMethod("getContext", boolean.class);

      Object logContext = loggerContextMethod.invoke(null, false);
      Object configurationObject = logContext.getClass().getMethod("getConfiguration").invoke(logContext);

      Object rootLoggerConfig = configurationObject.getClass().getMethod("getRootLogger").invoke(configurationObject);

      Class<?> filterClass = Class.forName("org.apache.logging.log4j.core.Filter");
      Method addFilterMethod = rootLoggerConfig.getClass().getMethod("addFilter", filterClass);

      Filter filter = createSecurityExceptionFilter();

      Class<?> filterAdapterClass = Class.forName("org.apache.log4j.bridge.FilterAdapter");
      Object filterAdapterObject = filterAdapterClass.getConstructor(Filter.class).newInstance(filter);

      addFilterMethod.invoke(rootLoggerConfig, filterAdapterObject);
    } catch(Throwable e) {
      logger.error(e, e);
    }
  }

  /**
   * EXM-44131 Filter an exception that we know it appears quite often and has no repercussions.
   */
  private static void addFiltersOnAppenders_Log4j1_2() {
    Enumeration allAppenders = Logger.getRootLogger().getAllAppenders();
    while (allAppenders.hasMoreElements()) {
      Appender appender = (Appender) allAppenders.nextElement();
      
      appender.addFilter(createSecurityExceptionFilter());
    }
  }

  /***
   * @return A filter that ignores a security relater exception that appears quite often and has no repercussions.
   */
  private static Filter createSecurityExceptionFilter() {
    return new Filter() {
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
    };
  }
}
