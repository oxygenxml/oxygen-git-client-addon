package com.oxygenxml.git.view.dialog;

import com.oxygenxml.git.options.OptionsManager;

/**
 * Dialog for requesting a passphrase for the GPG keys.
 */
public class GPGPassphraseDialog extends PassphraseDialog {
  
  /**
   * Create the dialog.
   * 
   * @param message The message to be shown by the dialog.
   */
  public GPGPassphraseDialog(String message) {
    super("GPG Passphrase", message);
  }

  /**
   * Save passphrase.
   */
  @Override
  protected void savePassphrase(String passphrase) {
    OptionsManager.getInstance().saveGPGPassphare(passphrase);
  }
  
}
