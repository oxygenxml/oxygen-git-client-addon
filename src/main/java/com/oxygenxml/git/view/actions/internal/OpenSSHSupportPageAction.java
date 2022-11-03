package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.SSHSupportOptionPage;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;


/**
 * Action to open preferences page for Git Plugin.
 * 
 * @author Alex_Smarandache
 */
public class OpenSSHSupportPageAction extends AlwaysEnabledAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	public OpenSSHSupportPageAction() {
		super(TRANSLATOR.getTranslation(Tags.OPEN_SSH_SUPPORT_PAGE));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		PluginWorkspaceProvider.getPluginWorkspace().showPreferencesPages(
				new String[] {SSHSupportOptionPage.KEY},
				SSHSupportOptionPage.KEY,
				true);
	}

}
