package com.oxygenxml.git.view.actions.internal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;


/**
 * Action to show Git Staging.
 * 
 * @author Alex_Smarandache
 *
 */
public class ShowStagingAction extends AlwaysEnabledAction {
	
	/**
	 * The translator for translations.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();
	
	
	/**
	 * Constructor.
	 */
	public ShowStagingAction() {
		super(TRANSLATOR.getTranslation(Tags.SHOW_STAGING));
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		((StandalonePluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace())
		    .showView(com.oxygenxml.git.OxygenGitPluginExtension.GIT_STAGING_VIEW, true);
	}

}
