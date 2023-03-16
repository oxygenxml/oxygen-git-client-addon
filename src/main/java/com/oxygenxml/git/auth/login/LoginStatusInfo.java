package com.oxygenxml.git.auth.login;

import org.eclipse.jgit.annotations.Nullable;

import com.oxygenxml.git.options.CredentialsBase;

/**
 * Contains informations about login status.
 *
 * @author alex_smarandache
 */
public class LoginStatusInfo {
  
  /**
   * The credentials obtained after login process.
   */
  private final CredentialsBase credentials;
  
  /**
   * <code>true</code> if the login was canceled.
   */
  private final boolean isCanceled;
  
  /**
   * Constructor. 
   * 
   * @param credentials The credentials obtained after login process.
   * @param isCanceled  <code>true</code> if the login was canceled.
   */
  public LoginStatusInfo(@Nullable final CredentialsBase credentials, 
      final boolean isCanceled) {
    this.credentials = credentials;
    this.isCanceled = isCanceled;
  }

  /**
   * @return The credentials obtained after login process.
   */
  @Nullable
  public CredentialsBase getCredentials() {
    return credentials;
  }

  /**
   * @return <code>true</code> if the login was canceled.
   */
  public boolean isCanceled() {
    return isCanceled;
  }

}
