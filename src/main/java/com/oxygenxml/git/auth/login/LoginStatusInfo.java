package com.oxygenxml.git.auth.login;

import org.eclipse.jgit.annotations.Nullable;

import com.oxygenxml.git.options.CredentialsBase;

/**
 * Concrete implementation for a @ILoginStatusInfo.
 *
 * @author alex_smarandache
 */
public class LoginStatusInfo implements ILoginStatusInfo {
  
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

  @Override
  @Nullable
  public CredentialsBase getCredentials() {
    return credentials;
  }

  @Override
  public boolean isCanceled() {
    return isCanceled;
  }

}
