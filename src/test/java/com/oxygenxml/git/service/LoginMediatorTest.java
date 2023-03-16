package com.oxygenxml.git.service;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Test;

import com.oxygenxml.git.auth.login.LoginMediator;
import com.oxygenxml.git.auth.login.LoginStatusInfo;
import com.oxygenxml.git.translator.Tags;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Tests the @LoginMediator.
 * 
 * @author alex_smarandache
 */
public class LoginMediatorTest extends GitTestBase {

  /**
   * <p><b>Description:</b> The login dialog should appear only once if it was closed by the user.</p>
   * 
   * <p><b>Bug ID:</b> EXM-52468</p>
   *
   * @author Alex_Smarandache
   */ 
  @Test
  public void testTheLoginDialogApparences() {
    LoginMediator.getInstance().reset();
    final LoginStatusInfo[] loginInfo = new LoginStatusInfo[1];
    SwingUtilities.invokeLater(() -> {
      loginInfo[0] = LoginMediator.getInstance().requestLogin("host", "loginMessage").orElse(null);
    });

    final OKCancelDialog loginDialog = (OKCancelDialog) findDialog(Tags.LOGIN_DIALOG_TITLE);
    assertNotNull(loginDialog);
    final JButton cancelButton = findFirstButton(loginDialog, Tags.CANCEL);
    assertNotNull(cancelButton);
    cancelButton.doClick();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> loginInfo[0] != null
        && loginInfo[0].isCanceled() && loginInfo[0].getCredentials() == null);
    SwingUtilities.invokeLater(() -> loginInfo[0] = 
        LoginMediator.getInstance().requestLogin("host", "loginMessage").orElse(null));
    assertNull(findDialog(Tags.LOGIN_DIALOG_TITLE));
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> loginInfo[0] == null);
    LoginMediator.getInstance().reset();
  }

}
