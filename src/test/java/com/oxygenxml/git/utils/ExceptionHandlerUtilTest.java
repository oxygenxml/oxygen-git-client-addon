package com.oxygenxml.git.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.junit.Test;

import com.oxygenxml.git.view.util.ExceptionHandlerUtil;

/**
 * Test class for the exception handler.
 * 
 * @author alex_smarandache
 */
public class ExceptionHandlerUtilTest {
  
  /**
   * <p><b>Description:</b> Checks if the exception cause is detected properly.</p>
   * 
   * <p><b>Bug ID:</b> EXM-54376</p>
   *
   * @author alex_smarandache
   *
   */ 
  @Test
  public void testIdentifyExceptionCause() {
    CanceledException canceledException = new CanceledException("Exception");
    IOException ioException = new IOException(canceledException);
    JGitInternalException jgitException = new JGitInternalException("Here is my exception", ioException);
    
    assertTrue(ExceptionHandlerUtil.hasCauseOfType(jgitException, CanceledException.class));
    assertFalse(ExceptionHandlerUtil.hasCauseOfType(jgitException, MalformedURLException.class));
  }

}
