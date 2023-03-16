package com.oxygenxml.git.service;

import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.oxygenxml.git.auth.login.LoginMediator;
import com.oxygenxml.git.auth.login.LoginStatusInfo;
import com.oxygenxml.git.view.dialog.LoginDialog;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Tests the @LoginMediator.
 * 
 * @author alex_smarandache
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LoginMediator.class)
public class LoginMediatorTest {

  /**
   * <p><b>Description:</b> The login dialog should appear only once if it was closed by the user.</p>
   * 
   * <p><b>Bug ID:</b> EXM-52468</p>
   *
   * @author Alex_Smarandache
   * 
   * @throws Exception 
   */ 
  @Test
  public void testTheLoginDialogApparences() throws Exception {
    try {
      final String host = "github.com";
      final String loginMessage = "Please login";
      final LoginDialog mockLoginDialog = Mockito.mock(LoginDialog.class);
      Mockito.when(mockLoginDialog.getResult()).thenReturn(OKCancelDialog.RESULT_CANCEL);
      PowerMockito.whenNew(LoginDialog.class).withArguments(host, loginMessage).thenReturn(mockLoginDialog);
      LoginMediator.getInstance().reset();
      
      final LoginStatusInfo[] loginInfo = new LoginStatusInfo[1];
      SwingUtilities.invokeLater(() -> {
        loginInfo[0] = LoginMediator.getInstance().requestLogin(host, loginMessage).orElse(null);
      });

      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> loginInfo[0] != null
          && loginInfo[0].isCanceled() && loginInfo[0].getCredentials() == null);
      
      SwingUtilities.invokeLater(() -> {
        loginInfo[0] = LoginMediator.getInstance().requestLogin(host, loginMessage).orElse(null);
      });
     
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> loginInfo[0] == null);
    } finally {
      LoginMediator.getInstance().reset();
    } 
    
  }
  
}
