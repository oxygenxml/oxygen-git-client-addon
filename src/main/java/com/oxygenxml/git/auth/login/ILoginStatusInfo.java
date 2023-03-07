package com.oxygenxml.git.auth.login;

import com.oxygenxml.git.options.CredentialsBase;

/**
 * An interface with informations about login status.
 * 
 * @author alex_smarandache
 */
public interface ILoginStatusInfo {
  
  /**
   * @return The credentials obtained after login process.
   */
  CredentialsBase getCredentials();
  
  /**
   * @return <code>true</code> if the login was canceled.
   */
  boolean isCanceled();
}
