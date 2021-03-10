package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import com.oxygenxml.git.options.CredentialsBase;
import com.oxygenxml.git.options.CredentialsBase.CredentialsType;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.PersonalAccessTokenInfo;
import com.oxygenxml.git.options.UserAndPasswordCredentials;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.AddRemoteDialog;
import com.oxygenxml.git.view.dialog.LoginDialog;
import com.oxygenxml.git.view.dialog.PassphraseDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Utility class for authentication-related issues.
 */
public class AuthUtil {
  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(AuthUtil.class);
  /**
   * Translator for i18n.
   */
  private static Translator translator = Translator.getInstance();
  
  /**
   * Hidden constructor.
   */
  private AuthUtil() {
    // Nothing
  }
  
  /**
   * Get the credentials provider for the given host.
   * 
   * @param host The host.
   * 
   * @return The credentials provider.
   */
  public static SSHCapableUserCredentialsProvider getCredentialsProvider(String host) {
    CredentialsBase credentials = OptionsManager.getInstance().getGitCredentials(host);
    CredentialsType credentialsType = credentials.getType();
    return new SSHCapableUserCredentialsProvider(
        credentialsType == CredentialsType.USER_AND_PASSWORD 
          ? ((UserAndPasswordCredentials) credentials).getUsername() 
            : ((PersonalAccessTokenInfo) credentials).getTokenValue(),
        credentialsType == CredentialsType.USER_AND_PASSWORD 
          ? ((UserAndPasswordCredentials) credentials).getPassword()
            : ((PersonalAccessTokenInfo) credentials).getTokenValue(),
        OptionsManager.getInstance().getSshPassphrase(),
        credentials.getHost());
  }
  
  /**
   * Handle authentication exception.
   * 
   * @param ex                The exception to handle.
   * @param hostName          The host name.
   * @param userCredentials   The user credentials.
   * @param excMessPresenter  Exception message presenter.  
   * @param retryLoginHere    <code>true</code> to retry login here, in this method.
   * 
   * @return <code>true</code> if the authentication should be tried again.
   */
  public static boolean handleAuthException(
      GitAPIException ex,
      String hostName,
      CredentialsBase userCredentials,
      AuthExceptionMessagePresenter excMessPresenter,
      boolean retryLoginHere) {
    
    if (logger.isDebugEnabled()) {
      logger.debug("Handle Auth Exception: ");
      logger.debug(ex, ex);
    }
    
    Throwable cause = ex;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    
    boolean tryAgainOutside = false;
    String lowercaseMsg = ex.getMessage().toLowerCase();
    if (lowercaseMsg.contains("not authorized") || lowercaseMsg.contains("authentication not supported")) {
      // Authorization problems.
      String loginMessage = "";
      if (userCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
        String username = ((UserAndPasswordCredentials) userCredentials).getUsername();
        if (username == null) {
          // No credentials were used but they are required.
          loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE);
        } else {
          // Invalid credentials.
          loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
              + " " + username;
        }
      } else {
        // TODO:
        loginMessage = "BAD TOKEN OR SOMETHING";
      }
      tryAgainOutside = shouldTryAgainOutside(hostName, retryLoginHere, loginMessage);
    } else if (lowercaseMsg.contains("not permitted")) {
      // The user doesn't have permissions.
      ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .showWarningMessage(translator.getTranslation(Tags.NO_RIGHTS_TO_PUSH_MESSAGE));
      String loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS); 
      if (userCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
        loginMessage += ", " + ((UserAndPasswordCredentials) userCredentials).getUsername();
      }
      loginMessage += ".";
      tryAgainOutside = shouldTryAgainOutside(hostName, retryLoginHere, loginMessage);
    } else if (lowercaseMsg.contains("origin: not found")
        || lowercaseMsg.contains("no value for key remote.origin.url found in configuration")) {
      // No remote linked with the local.
      tryAgainOutside  = new AddRemoteDialog().linkRemote();
    } else if (lowercaseMsg.contains("auth fail")
        || (cause instanceof SshException)
            && ((SshException) cause).getDisconnectCode() == SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE) {
      // This message is thrown for SSH.
      String passPhraseMessage = translator.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      String passphrase = new PassphraseDialog(passPhraseMessage).getPassphrase();
      tryAgainOutside = passphrase != null;
    } else if (ex.getCause() instanceof NoRemoteRepositoryException
        || lowercaseMsg.contains("invalid advertisement of")) {
      if (excMessPresenter != null) {
        excMessPresenter.presentMessage(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
      } else {
        logger.error(ex, ex);
      }
    } else {
      // "Unhandled" exception
      if (excMessPresenter != null) {
        excMessPresenter.presentMessage(ex.getClass().getName() + ": " + ex.getMessage());
      } else {
        logger.error(ex, ex);
      }
    }
    
    return tryAgainOutside;
  }

  /**
   * 
   * 
   * @param hostName
   * @param retryLoginHere
   * @param loginMessage
   * 
   * @return
   */
  private static boolean shouldTryAgainOutside(String hostName, boolean retryLoginHere, String loginMessage) {
    boolean tryAgainOutside = false;
    if (retryLoginHere) {
      // Request new credentials.
      LoginDialog loginDlg = new LoginDialog(hostName, loginMessage);
      tryAgainOutside = loginDlg.getCredentials() != null;
    } else {
      tryAgainOutside = true;
    }
    return tryAgainOutside;
  }
  
}
