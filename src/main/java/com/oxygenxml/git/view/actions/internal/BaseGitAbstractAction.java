package com.oxygenxml.git.view.actions.internal;

import java.util.Optional;

import javax.swing.AbstractAction;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Base abstract action for Git. 
 * <br>
 * Add the action to the shortcuts menu. 
 * 
 * @author alex_smarandache
 *
 */
public abstract class BaseGitAbstractAction extends AbstractAction {

	/**
	 * Constructor.
	 * 
	 * @param actionName
	 */
	public BaseGitAbstractAction(final String actionName) {
		super(actionName);
		StandalonePluginWorkspace pluginWorkspace = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
		Optional.ofNullable(pluginWorkspace).ifPresent(pws -> pws.getActionsProvider().registerAction("git." + actionName, this, null));
	}
	
}
