package com.oxygenxml.git.service;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.oxygenxml.git.auth.SSHCapableUserCredentialsProvider;

/**
 * Test the user credentials provider.
 * 
 * @author alex_smarandache
 */
public class UserCredentialsProviderTest {
  
  /**
   * <p><b>Description:</b> Test if no NPE is obtained when the password is <code>null</code>.</p>
   * 
   * <p><b>Bug ID:</b> EXM-52468</p>
   *
   * @author alex_smarandache 
   */ 
  @Test
  public void testNullPasswordOnUserCredentialsProvider() {
    SSHCapableUserCredentialsProvider credProvider = new SSHCapableUserCredentialsProvider("alex", null, "pass", "host");
    assertNotNull(credProvider);
  }

}
