package com.oxygenxml.git.view.dialog;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.options.OptionTags;
import com.oxygenxml.git.service.WSOptionsStorageTestAdapter;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Test related to the dialogs that save the SSH and GPG passphrase.
 */
public class PassphraseDialogsTest {

  /**
   * Do before each test.
   */
  @Before
  public void setUp() {
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
    
    UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(pluginWSMock.getUtilAccess()).thenReturn(utilAccessMock);
    Mockito.when(utilAccessMock.encrypt(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
  }
  
  /**
   * Do after each test.
   */
  @After
  public void tearDown() {
    PluginWorkspaceProvider.setPluginWorkspace(null);
  }
  
  /**
   * <p><b>Description:</b> save each passphrase using its corresponding option tag.</p>
   * <p><b>Bug ID:</b> EXM-52129</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  @Test
  public void testSavePassphrases() throws Exception {
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    assertEquals(
        "",
        optionsStorage.getOption(OptionTags.SSH_PASSPHRASE, ""));
    assertEquals(
        "",
        optionsStorage.getOption(OptionTags.GPG_PASSPHRASE, ""));
    
    // ======================== SSH keys passphrase ==============================
    SSHPassphraseDialog sshDlg = Mockito.mock(SSHPassphraseDialog.class);
    Mockito.doCallRealMethod().when(sshDlg).savePassphrase(Mockito.anyString());
    sshDlg.savePassphrase("ssh_pass");
    
    assertEquals(
        "ssh_pass",
        optionsStorage.getOption(OptionTags.SSH_PASSPHRASE, ""));
    assertEquals(
        "",
        optionsStorage.getOption(OptionTags.GPG_PASSPHRASE, ""));
    
    // ======================== GPG keys passphrase ==============================
    GPGPassphraseDialog gpgDlg = Mockito.mock(GPGPassphraseDialog.class);
    Mockito.doCallRealMethod().when(gpgDlg).savePassphrase(Mockito.anyString());
    gpgDlg.savePassphrase("gpg_pass");
    
    assertEquals(
        "ssh_pass",
        optionsStorage.getOption(OptionTags.SSH_PASSPHRASE, ""));
    assertEquals(
        "gpg_pass",
        optionsStorage.getOption(OptionTags.GPG_PASSPHRASE, ""));
  }
  
}
