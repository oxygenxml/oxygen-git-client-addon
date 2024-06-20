package com.oxygenxml.git.view.dialog;

import com.oxygenxml.git.options.OptionsManager;

/**
 * Dialog for requesting a passphrase for the SSH keys.
 */
public class SSHPassphraseDialog extends PassphraseDialog {
  
  /**
   * Create the dialog.
   * 
   * @param message The message to be shown by the dialog.
   */
  public SSHPassphraseDialog(String message) {
    super("SSH Passphrase", message);
  }

  /**
   * Save passphrase.
   */
  @Override
  protected void savePassphrase(String passphrase) {
    OptionsManager.getInstance().saveSshPassphare(passphrase);
  }
  
}
