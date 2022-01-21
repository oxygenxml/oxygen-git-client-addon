package com.oxygenxml.git.auth;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.CredentialsBase;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.PersonalAccessTokenInfo;
import com.oxygenxml.git.options.UserAndPasswordCredentials;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(ResetableUserCredentialsProvider.class); 
  /**
   * Translator for i18n.
   */
  protected static Translator translator = Translator.getInstance();
  /**
   * <code>true</code> to cancel the login (i.e. to stop showing the login dialog).
   */
  private boolean isUserCancelledLogin;
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
  private boolean isCredentialsPreviouslyRequested = false; 

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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Reset credentials provider for: " + uri.toString());
    }
    if (isCredentialsPreviouslyRequested && !isUserCancelledLogin) {
      LoginDialog loginDialog = new LoginDialog(host, getLoginFailureMessage());
      if (loginDialog.getResult() == OKCancelDialog.RESULT_OK) {
        updateUsernameAndPassword(loginDialog.getCredentials());
      } else {
        isUserCancelledLogin = true;
      }
    }
    super.reset(uri);
  }

  /**
   * @return the login failure message to show in the dialog.
   */
  private String getLoginFailureMessage() {
    String loginMessage = translator.getTranslation(Tags.AUTHENTICATION_FAILED) + " ";
    CredentialsBase creds = OptionsManager.getInstance().getGitCredentials(host);
    if (creds.getType() == CredentialsType.USER_AND_PASSWORD) {
      loginMessage += username == null ? translator.getTranslation(Tags.NO_CREDENTIALS_FOUND)
          : translator.getTranslation(Tags.CHECK_CREDENTIALS);
    } else if (creds.getType() == CredentialsType.PERSONAL_ACCESS_TOKEN) {
      loginMessage += translator.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS);
    }
    return loginMessage;
  }

  /**
   * Update username and password, based on the credentials type.
   * 
   * @param credentials New credentials.
   */
  private void updateUsernameAndPassword(CredentialsBase credentials) {
    if (credentials != null) {
      if (credentials.getType() == CredentialsType.USER_AND_PASSWORD) {
        username = ((UserAndPasswordCredentials) credentials).getUsername();
        password = ((UserAndPasswordCredentials) credentials).getPassword();
      } else if (credentials.getType() == CredentialsType.PERSONAL_ACCESS_TOKEN) {
        // GitHub uses the username as token value, GitLab uses the password
        username = ((PersonalAccessTokenInfo) credentials).getTokenValue();
        password = ((PersonalAccessTokenInfo) credentials).getTokenValue();
      }
    }
  }
  
  /**
   * @see org.eclipse.jgit.transport.CredentialsProvider.get(URIish, CredentialItem...)
   */
  @Override
  public boolean get(URIish uri, CredentialItem... items) {
    wasReset = false;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Get credential items for: " + uri.toString());
    } 
    if (!isUserCancelledLogin) {
      for (CredentialItem credentialItem : items) { // NOSONAR
        if (credentialItem instanceof CredentialItem.Username) {
          ((CredentialItem.Username) credentialItem).setValue(username);
          isCredentialsPreviouslyRequested = true;
          continue;
        }
        if (credentialItem instanceof CredentialItem.Password) {
          ((CredentialItem.Password) credentialItem).setValue(password.toCharArray());
          isCredentialsPreviouslyRequested = true;
          continue;
        }
        if (credentialItem instanceof CredentialItem.StringType
            //$NON-NLS-1$
            && credentialItem.getPromptText().startsWith("Password:")) {
          ((CredentialItem.StringType) credentialItem).setValue(password);
          isCredentialsPreviouslyRequested = true;
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
    return isUserCancelledLogin;
  }
}
