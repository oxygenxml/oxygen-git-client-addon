package com.oxygenxml.git.service;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.view.dialog.PassphraseDialog;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class CustomUserCredentials extends UsernamePasswordCredentialsProvider {

	private boolean firstTry = true;

	public static boolean passphraseChecked = false;
	/**
	 * The pass phase to be used for SSH connections.
	 */
	private String passphrase;
	/**
	 * <code>true</code> if the pass phase was requested (for SSH).
	 */
	private boolean passphaseRequested = false;
	
	/**
	 * Constructor.
	 * 
	 * @param username User name.
	 * @param password Password.
	 * @param passphrase SSH pass phase.
	 */
	public CustomUserCredentials(String username, String password, String passphrase) {
		super(username, password);
		this.passphrase = passphrase;
	}
	
	

	@Override
	public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
		for (CredentialItem i : items) {
			if (i instanceof CredentialItem.StringType) {
				if (i.getPromptText().startsWith("Passphrase")) {
				  // A not so great method to check that the pass phrase is requested.
				  passphaseRequested = true;
				  
					((CredentialItem.StringType) i).setValue(new String(passphrase));
					return true;
				}
			}
			if (i instanceof CredentialItem.YesNoType) {
				if (GitAccess.getInstance().isSshChecked()) {
					return true;
				}
				GitAccess.getInstance().setSshChecked(true);
				String[] options = new String[] { "   Yes   ", "   No   " };
				int[] optonsId = new int[] { 0, 1 };
				int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace())
						.showConfirmDialog("Connection", i.getPromptText(), options, optonsId);
				if (response == 0) {
				  // Apparently JGIT also checks the value from the given item.
				  // org.eclipse.jgit.transport.CredentialsProviderUserInfo.promptYesNo(String)
				  ((CredentialItem.YesNoType) i).setValue(true);
					return true;
				} else {
					return false;
				}
			}
		}
		return super.get(uri, items);
	}
	
	/**
	 * @return <code>true</code> if the pass phase was requested (for SSH).
	 */
	public boolean isPassphaseRequested() {
    return passphaseRequested;
  }
	

	private boolean passphraseIsValid(CredentialItem i) {
		String sshPassphrase = OptionsManager.getInstance().getSshPassphrase();
		String messageDialog = "";
		if ("".equals(sshPassphrase) || sshPassphrase == null) {
			messageDialog = "No passphrase found. Please enter your SSH passphrase";
		} else {
			if (firstTry) {
				firstTry = false;
				((CredentialItem.StringType) i).setValue(new String(sshPassphrase));
				return true;
			} else {
				messageDialog = "The previous passphrase is invalid. Please enter your SSH passphrase";
			}
		}
		sshPassphrase = new PassphraseDialog(messageDialog).getPassphrase();
		if (sshPassphrase == null) {
			passphraseChecked = true;
			return false;
		} else {
			((CredentialItem.StringType) i).setValue(new String(sshPassphrase));
			return true;
		}
	}
}
