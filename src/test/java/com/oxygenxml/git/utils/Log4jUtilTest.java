package com.oxygenxml.git.utils;

import java.io.FilePermission;
import java.io.IOException;
import java.io.StringWriter;
import java.security.AccessControlException;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.eclipse.jgit.util.FS;
import org.junit.Assert;

import junit.framework.TestCase;

/**
 * Logging setup tests. 
 */
public class Log4jUtilTest extends TestCase {
  
  private WriterAppender newAppender;
  private StringWriter writer;

  protected void setUp() throws Exception {
    writer = new StringWriter();
    
    newAppender = org.apache.logging.log4j.core.appender.WriterAppender.createAppender(null, null, writer, "My.appender", false, true);
    LoggerContext.getContext().getConfiguration().getRootLogger().addAppender(
        newAppender, org.apache.logging.log4j.Level.ERROR, null);
    
    Log4jUtil.setupLog4JLogger();
  }
  
  protected void tearDown() throws Exception {
    LoggerContext.getContext().getRootLogger().removeAppender(newAppender);
  }

  /**
   * EXM-44131 Tests that we filter a specific AccessControlException error.
   * 
   * @throws Exception
   */
  public void testLogFilter() throws Exception {

    //=====================
    // An exception is not an AccessControlException. It should pass.
    //=====================
    
    Exception ex = new IOException("A test");
    LoggerContext.getContext().getLogger(FS.class.toString()).error(ex,  ex);

    Assert.assertTrue("The log must pass: " + writer.toString(), writer.toString().contains("java.io.IOException: A test"));

    Assert.assertFalse("The log must pass: " + writer.toString(), writer.toString().contains("java.security.AccessControlException"));
    
    //=====================
    // An AccessControlException issued through a specific class logger. It should be filtered.
    //=====================
    writer.getBuffer().setLength(0);
    FilePermission perm = new FilePermission(".probe-64fe0316-10fa-4fa1-b163-d79366318e4b", "write");
    ex = new AccessControlException("access denied "+ perm, perm);
    Logger.getLogger(FS.class).error(ex,  ex);
    
    Assert.assertEquals("This exception should be filtered from the logger: " + writer.toString(), "", writer.toString());
    

    //=====================
    // An AccessControlException issued through another class logger. It should pass.
    //=====================
    writer.getBuffer().setLength(0);
    perm = new FilePermission(".probe-64fe0316-10fa-4fa1-b163-d79366318e4b", "write");
    ex = new AccessControlException("access denied "+ perm, perm);
    LoggerContext.getContext().getLogger(Log4jUtilTest.class.toString()).error(ex,  ex);
    
    Assert.assertTrue("The log must pass: " + writer.toString(), writer.toString().startsWith("java.security.AccessControlException: access denied (\"java.io.FilePermission\" \".probe-64fe0316-10fa-4fa1-b163-d79366318e4b\" \"write\")"));
  }
}
