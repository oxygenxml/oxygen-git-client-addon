package com.oxygenxml.git.auth;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Utility class for authentication-related issues.
 */
public class AuthUtil {
  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthUtil.class);
  /**
   * Translator for i18n.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  /**
   * Part of exception message shown when no value was found in the configuration for remote.origin.url.
   */
  private static final String NO_VALUE_FOR_ORIGIN_URL_IN_CONFIG = 
      "no value for key remote.origin.url found in configuration";
  /**
   * Part of the exception message shown when origin is not found.
   */
  private static final String ORIGIN_NOT_FOUND = "origin: not found";
  /**
   * Part of the exception message shown when authentication fails.
   */
  public static final String AUTH_FAIL = "auth fail";
  /**
   * Part of the exception message shown when an operation is not permitted.
   */
  public static final String NOT_PERMITTED = "not permitted";
  /**
   * Part of the exception message shown when an operation is not authorized.
   */
  public static final String NOT_AUTHORIZED = "not authorized";
  /**
   * Part of the exception message shown when a specific authentication type is not supported.
   */
  public static final String AUTHENTICATION_NOT_SUPPORTED = "authentication not supported";
  
  public static final String GIT_UPLOAD_PACK = "git-upload-pack";
  
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
   * @param excMessPresenter  Exception message presenter.  
   * @param retryLoginHere    <code>true</code> to retry login here, in this method.
   * 
   * @return <code>true</code> if the authentication should be tried again.
   */
  public static boolean handleAuthException(
      GitAPIException ex,
      String hostName,
      AuthExceptionMessagePresenter excMessPresenter,
      boolean retryLoginHere) {
    
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Handle Auth Exception: ");
      LOGGER.debug(ex.getMessage(), ex);
    }
    
    Throwable cause = ex;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    
    CredentialsBase userCredentials = OptionsManager.getInstance().getGitCredentials(hostName);
    
    boolean tryAgainOutside = false;
    String lowercaseMsg = ex.getMessage().toLowerCase();
    if (lowercaseMsg.contains(NOT_AUTHORIZED) 
        || lowercaseMsg.contains(AUTHENTICATION_NOT_SUPPORTED)) {
      // Authorization problems.
      String loginMessage = TRANSLATOR.getTranslation(Tags.AUTHENTICATION_FAILED) + " ";
      if (userCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
        String username = ((UserAndPasswordCredentials) userCredentials).getUsername();
        loginMessage += username == null ? TRANSLATOR.getTranslation(Tags.NO_CREDENTIALS_FOUND)
            : TRANSLATOR.getTranslation(Tags.CHECK_CREDENTIALS);
      } else if (userCredentials.getType() == CredentialsType.PERSONAL_ACCESS_TOKEN) {
        loginMessage += TRANSLATOR.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS);
      }
      tryAgainOutside = shouldTryAgainOutside(hostName, retryLoginHere, loginMessage);
    } else if (lowercaseMsg.contains(NOT_PERMITTED)) {
      // The user doesn't have permissions.
      PluginWorkspaceProvider.getPluginWorkspace()
          .showErrorMessage(TRANSLATOR.getTranslation(Tags.NO_RIGHTS_TO_PUSH_MESSAGE));
      String loginMessage = TRANSLATOR.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS); 
      if (userCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
        loginMessage += ", " + ((UserAndPasswordCredentials) userCredentials).getUsername();
      }
      loginMessage += ".";
      tryAgainOutside = shouldTryAgainOutside(hostName, retryLoginHere, loginMessage);
    } else if (lowercaseMsg.contains(ORIGIN_NOT_FOUND)
        || lowercaseMsg.contains(NO_VALUE_FOR_ORIGIN_URL_IN_CONFIG)) {
      // No remote linked with the local.
      tryAgainOutside  = new AddRemoteDialog().linkRemote();
    } else if (lowercaseMsg.contains(AUTH_FAIL)
        || (cause instanceof SshException)
            && ((SshException) cause).getDisconnectCode() == SshConstants.SSH2_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE) {
      // This message is thrown for SSH.
      String passPhraseMessage = TRANSLATOR.getTranslation(Tags.ENTER_SSH_PASS_PHRASE);
      String passphrase = new PassphraseDialog(passPhraseMessage).getPassphrase();
      tryAgainOutside = passphrase != null;
    } else if (ex.getCause() instanceof NoRemoteRepositoryException
        || lowercaseMsg.contains("invalid advertisement of")) {
      if (excMessPresenter != null) {
        if (userCredentials.getType() == CredentialsType.USER_AND_PASSWORD) {
          excMessPresenter.presentMessage(TRANSLATOR.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY));
        } else if (userCredentials.getType() == CredentialsType.PERSONAL_ACCESS_TOKEN) {
          excMessPresenter.presentMessage(TRANSLATOR.getTranslation(Tags.CANNOT_REACH_HOST) + ". "
              + TRANSLATOR.getTranslation(Tags.CHECK_TOKEN_VALUE_AND_PERMISSIONS));
        }
      } else {
        LOGGER.error(ex.getMessage(), ex);
      }
    } else if (ex instanceof TransportException) {
      if (excMessPresenter != null) {
        excMessPresenter.presentMessage(TRANSLATOR.getTranslation(
            ex.getMessage().contains(GIT_UPLOAD_PACK) ? Tags.CANNOT_OPEN_GIT_UPLOAD_PACK : Tags.DOMAIN_NAME_DOES_NOT_EXIST)); 
      }
    } else {
      // "Unhandled" exception
      if (excMessPresenter != null) {
        excMessPresenter.presentMessage(ex instanceof RefNotAdvertisedException ? 
            TRANSLATOR.getTranslation(Tags.NO_REMOTE_EXCEPTION_MESSAGE) : 
          ex.getClass().getName() + ": " + ex.getMessage());
      } else {
        LOGGER.error(ex.getMessage(), ex);
      }
    }
    
    return tryAgainOutside;
  }

  /**
   * Should try again outside this class, in the caller class?
   * 
   * @param hostName        Host name.
   * @param retryLoginHere  True to retry login here in this class.
   * @param loginMessage    The login failure message to show.
   * 
   * @return <code>true</code> to try logging in outside of this class.
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
