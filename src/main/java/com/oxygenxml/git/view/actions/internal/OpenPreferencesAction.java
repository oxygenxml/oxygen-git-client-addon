package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;


/**
 * Action to open preferences page for Git Plugin.
 * 
 * @author Alex_Smarandache
 */
public class OpenPreferencesAction extends AlwaysEnabledAction {

	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	public OpenPreferencesAction() {
		super(TRANSLATOR.getTranslation(Tags.PREFERENCES));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		PluginWorkspaceProvider.getPluginWorkspace().showPreferencesPages(
				new String[] {OxygenGitOptionPagePluginExtension.KEY},
				OxygenGitOptionPagePluginExtension.KEY,
				true);
	}

}
