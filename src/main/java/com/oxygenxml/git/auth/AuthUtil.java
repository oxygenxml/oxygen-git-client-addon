package com.oxygenxml.git.auth;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import com.oxygenxml.git.options.UserCredentials;
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
   * Handle authentication exception.
   * 
   * @param ex                The exception to handle.
   * @param hostName          The host name.
   * @param userCredentials   The user credentials.
   * @param excMessPresenter  Exception message preenter.  
   * @param retryLoginHere    <code>true</code> to retry login here, in this method.
   * 
   * @return <code>true</code> if the authentication should be tried again.
   */
  public static boolean handleAuthException(
      GitAPIException ex,
      String hostName,
      UserCredentials userCredentials,
      AuthExceptionMessagePresenter excMessPresenter,
      boolean retryLoginHere) {
    
    if (logger.isDebugEnabled()) {
      logger.debug("Handle Auth Exception: ");
      logger.debug(ex, ex);
    }
    
    boolean tryAgainOutside = false;
    String lowercaseMsg = ex.getMessage().toLowerCase();
    if (lowercaseMsg.contains("not authorized") || lowercaseMsg.contains("authentication not supported")) {
      // Authorization problems.
      String loginMessage = "";
      if (userCredentials.getUsername() == null) {
        // No credentials were used but they are required.
        loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_NOT_FOUND_MESSAGE);
      } else {
        // Invalid credentials.
        loginMessage = translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_INVALID_MESSAGE)
            + " " + userCredentials.getUsername();
      }
      if (retryLoginHere) {
        // Request new credentials.
        UserCredentials loadNewCredentials = requestNewCredentials(hostName, loginMessage);
        tryAgainOutside = loadNewCredentials != null;
      } else {
        tryAgainOutside = true;
      }
    } else if (lowercaseMsg.contains("not permitted")) {
      // The user doesn't have permissions.
      ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
          .showWarningMessage(translator.getTranslation(Tags.NO_RIGHTS_TO_PUSH_MESSAGE));
      if (retryLoginHere) {
        // Request new credentials.
        UserCredentials loadNewCredentials = requestNewCredentials(
            hostName,
            translator.getTranslation(Tags.LOGIN_DIALOG_CREDENTIALS_DOESNT_HAVE_RIGHTS) + " "
                + userCredentials.getUsername());
        tryAgainOutside = loadNewCredentials != null;
      } else {
        tryAgainOutside = true;
      }
    } else if (lowercaseMsg.contains("origin: not found")
        || lowercaseMsg.contains("no value for key remote.origin.url found in configuration")) {
      // No remote linked with the local.
      tryAgainOutside  = new AddRemoteDialog().linkRemote();
    } else if (lowercaseMsg.contains("auth fail")) {
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
   * Opens a login dialog to update the credentials
   * 
   * @param loginMessage
   * 
   * @return the new credentials or <code>null</code> if the user canceled.
   */
  private static UserCredentials requestNewCredentials(String hostName, String loginMessage) {
    return new LoginDialog(hostName, loginMessage).getUserCredentials();
  }
}
