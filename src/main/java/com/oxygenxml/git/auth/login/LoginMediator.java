package com.oxygenxml.git.auth.login;

import java.util.Optional;

import org.eclipse.jgit.annotations.NonNull;

import com.oxygenxml.git.view.dialog.LoginDialog;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * This class is responsible to control the login process.
 * 
 * @author alex_smarandache
 */
public class LoginMediator {
  
  /**
   * <code>true</code> if the last dialog was canceled.
   */
  private boolean lastDialogWasCanceled = false;
 
  /**
   * This inner class is used to be sure that a single instance will be created lazy.
   * 
   * @author alex_smarandache
   */
  private static final class SingletonHelper {
    /**
     * The unique instance.
     */
    static final LoginMediator INSTANCE;
    static {
      INSTANCE = new LoginMediator();
    }
  }
  
  /**
   * Hidden constructor.
   */
  private LoginMediator() {
    // not needed
  }
  
  /**
   * @return The unique instance.
   */
  public static LoginMediator getInstance() {
    return SingletonHelper.INSTANCE;
  }
  
  /**
   * This method will show a dialog to the user to request him to enter the credentials to login on the given host.
   * <br>
   * If the previous dialog was canceled by the user, the method will do nothing.
   * <br>
   * You must call reset() method to reset the flags and be sure this method will try to receive connection informations from the user.
   * 
   * @param host         The host for which to provide the credentials.
   * @param loginMessage The login message.
   * 
   * @return An optional containing a @ILoginStatusInfo if login dialog was shown, or an empty optional otherwise.
   */
  public synchronized Optional<ILoginStatusInfo> requestLogin(
      @NonNull final String host, @NonNull final String loginMessage) {
    Optional<ILoginStatusInfo> toReturn = Optional.empty();
    if(!lastDialogWasCanceled) {
      final LoginDialog loginDialog = new LoginDialog(host, loginMessage);
      lastDialogWasCanceled = loginDialog.getResult() == OKCancelDialog.RESULT_CANCEL;
      toReturn = Optional.of(new LoginStatusInfo(loginDialog.getCredentials(), lastDialogWasCanceled));
    }
    
    return toReturn;
  }
  
  /**
   * This method will reset all attributes to the initial default value.
   */
  public void reset() {
    lastDialogWasCanceled = false;
  }

}
