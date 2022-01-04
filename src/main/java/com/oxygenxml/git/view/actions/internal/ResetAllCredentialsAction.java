package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.refresh.GitRefreshSupport;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;


/**
 * Action to reset all credentials.
 * 
 * @author alex_smarandache
 *
 */
public class ResetAllCredentialsAction extends AlwaysEnabledAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Needed to perform a refresh after resetting the credentials.
	 */
	private final GitRefreshSupport refreshSupport;

	/**
	 * Constructor.
	 * 
	 * @param refreshSupport Needed to perform a refresh after resetting the credentials.
	 */
	public ResetAllCredentialsAction(final GitRefreshSupport refreshSupport) {
		super(TRANSLATOR.getTranslation(Tags.RESET_ALL_CREDENTIALS));
		this.refreshSupport = refreshSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int result = PluginWorkspaceProvider.getPluginWorkspace().showConfirmDialog(
				TRANSLATOR.getTranslation(Tags.RESET_ALL_CREDENTIALS),
				TRANSLATOR.getTranslation(Tags.RESET_CREDENTIALS_CONFIRM_MESAGE),
				new String[] {
						"   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
						"   " + TRANSLATOR.getTranslation(Tags.NO) + "   " },
				new int[] {1, 0});
		if (result == 1) {
			OptionsManager optManager = OptionsManager.getInstance();
			optManager.saveSshPassphare(null);
			optManager.saveGitCredentials(null);

			refreshSupport.call();
		}
	}

}
