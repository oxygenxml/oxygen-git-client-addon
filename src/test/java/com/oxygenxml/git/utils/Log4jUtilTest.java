package com.oxygenxml.git.utils;

import java.io.ByteArrayOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.jgit.util.FS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Logging setup tests. 
 */
public class Log4jUtilTest {
  
  private WriterAppender newAppender;
  private ByteArrayOutputStream baos;

  @Before
  public void setUp() throws Exception {
    baos = new ByteArrayOutputStream();
    Configurator.setRootLevel(Level.ERROR);
    newAppender = WriterAppender.newBuilder()
        .setName("writeLogger")
        .setTarget(new OutputStreamWriter(baos))
        .setLayout(PatternLayout.newBuilder().withPattern("%level - %m%n").build())
        .build();
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    newAppender.start();
    config.addAppender(newAppender);
    config.getRootLogger().addAppender(newAppender, null, null);
    
    Log4jUtil.setupLog4JLogger();
  }
  
  @After
  public void tearDown() throws Exception {
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    config.getRootLogger().removeAppender(newAppender.getName());
  }

  /**
   * EXM-44131 Tests that we filter a specific AccessControlException error.
   * 
   * @throws Exception
   */
  @Test
  public void testLogFilter() throws Exception {

    //=====================
    // An exception is not an AccessControlException. It should pass.
    //=====================
    
    Exception ex = new IOException("A test");
    Logger logger = Logger.getLogger(FS.class);
    logger.error(ex,  ex);
    
    String message = baos.toString("UTF8");
    Assert.assertTrue("The log must pass: " + message, message.toString().startsWith("ERROR - java.io.IOException: A test"));

    //=====================
    // An AccessControlException issued through a specific class logger. It should be filtered.
    //=====================
    baos.reset();
    FilePermission perm = new FilePermission(".probe-64fe0316-10fa-4fa1-b163-d79366318e4b", "write");
    ex = new AccessControlException("access denied "+ perm, perm);
    Logger.getLogger(FS.class).error(ex,  ex);
    
    message = baos.toString("UTF8");
    Assert.assertEquals("This exception should be filtered from the logger: " + message, "", message.toString());
    

    //=====================
    // An AccessControlException issued through another class logger. It should pass.
    //=====================
    baos.reset();
    perm = new FilePermission(".probe-64fe0316-10fa-4fa1-b163-d79366318e4b", "write");
    ex = new AccessControlException("access denied "+ perm, perm);
    Logger.getLogger(Log4jUtilTest.class).error(ex,  ex);
    
    message = baos.toString("UTF8");
    Assert.assertTrue("The log must pass: " + message, message.startsWith("ERROR - java.security.AccessControlException: access denied (\"java.io.FilePermission\" \".probe-64fe0316-10fa-4fa1-b163-d79366318e4b\" \"write\")"));
  }
}
