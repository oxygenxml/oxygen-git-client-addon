package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.LoginDialog;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * A {@link UsernamePasswordCredentialsProvider} that has an implementation for the "reset()" method.
 * This implementation shows the login dialog.
 */
public class ResetableUserCredentialsProvider extends UsernamePasswordCredentialsProvider {
  /**
   *  Logger for logging.
   */
  private static Logger logger = Logger.getLogger(ResetableUserCredentialsProvider.class); 
  /**
   * Translator for i18n.
   */
  protected static Translator translator = Translator.getInstance();
  /**
   * <code>true</code> to cancel the login (i.e. to stop showing the login dialog).
   */
  private boolean shouldCancelLogin;
  /**
   * User name.
   */
  private String username;
  /**
   * Password.
   */
  private String password;
  /**
   * Host name.
   */
  private String host;
  /**
   * <code>true</code> if the "reset()" method was called
   * for the current login try.
   */
  private boolean wasReset;
  /**
   * Flag to keep track if the credentials were previously created.
   */
  private boolean userCredentialsRequested = false; 

  /**
   * Constructor.
   * 
   * @param username User name.
   * @param password Password.
   * @param host     Host name.
   */
  public ResetableUserCredentialsProvider(String username, String password, String host) {
    super(username, password);
    this.username = username;
    this.password = password;
    this.host = host;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @see org.eclipse.jgit.transport.CredentialsProvider.reset(URIish)
   */
  @Override
  public void reset(URIish uri) {
    wasReset = true;
    if (logger.isDebugEnabled()) {
      logger.debug("Reset credentials provider for: " + uri.toString());
    }
    if (
        // The credentials were actually previously requested. 
        userCredentialsRequested 
        // The user hasn't already canceled a login session.
        && !shouldCancelLogin) {
      LoginDialog loginDialog = new LoginDialog(
          host,
          username == null ? translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE)
              : translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE) + " " + username);
      if (loginDialog.getResult() == OKCancelDialog.RESULT_OK) {
        UserCredentials userCredentials = loginDialog.getUserCredentials();
        username = userCredentials.getUsername();
        password = userCredentials.getPassword();
      } else {
        shouldCancelLogin = true;
      }
    }
    super.reset(uri);
  }
  
  /**
   * @see org.eclipse.jgit.transport.CredentialsProvider.get(URIish, CredentialItem...)
   */
  @Override
  public boolean get(URIish uri, CredentialItem... items) {
    wasReset = false;
    if (logger.isDebugEnabled()) {
      logger.debug("Get credential items for: " + uri.toString());
    } 
    if (!shouldCancelLogin) {
      for (CredentialItem credentialItem : items) { // NOSONAR
        if (credentialItem instanceof CredentialItem.Username) {
          ((CredentialItem.Username) credentialItem).setValue(username);
          userCredentialsRequested = true;
          continue;
        }
        if (credentialItem instanceof CredentialItem.Password) {
          ((CredentialItem.Password) credentialItem).setValue(password.toCharArray());
          userCredentialsRequested = true;
          continue;
        }
        if (credentialItem instanceof CredentialItem.StringType
            //$NON-NLS-1$
            && credentialItem.getPromptText().equals("Password: ")) {
          ((CredentialItem.StringType) credentialItem).setValue(password);
          userCredentialsRequested = true;
          continue;
        }
        throw new UnsupportedCredentialItem(uri, credentialItem.getClass().getName()
            + ":" + credentialItem.getPromptText()); //$NON-NLS-1$
      }
      return true;
    } else {
      return super.get(uri, items);
    }
  }
  
  /**
   * @return <code>true</code> if the "reset()" method was called
   * for the current login try.
   */
  public boolean wasResetCalled() {
    return wasReset;
  }

  /**
   * @return <code>true</code> if the login re-trying should be canceled.
   */
  public boolean shouldCancelLogin() {
    return shouldCancelLogin;
  }
}
