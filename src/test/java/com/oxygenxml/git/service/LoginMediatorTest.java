package com.oxygenxml.git.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

      LoginStatusInfo loginInfo = LoginMediator.getInstance().requestLogin(host, loginMessage).orElse(null);
      assertNotNull(loginInfo);
      assertTrue(loginInfo.isCanceled());
      assertNull(loginInfo.getCredentials());

      loginInfo = LoginMediator.getInstance().requestLogin(host, loginMessage).orElse(null);
      assertNull(loginInfo);
    } finally {
      LoginMediator.getInstance().reset();
    } 
  }
  
}
