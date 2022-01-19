package com.oxygenxml.git.view.actions.internal;

import javax.swing.AbstractAction;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.actions.ActionsProvider;

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
		final StandalonePluginWorkspace pluginWorkspace = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
		if(pluginWorkspace != null) {
		  final ActionsProvider actionsProvider = pluginWorkspace.getActionsProvider();
		  if(actionsProvider != null) {
		    actionsProvider.registerAction("git." + actionName, this, null);
		  }  
		}
	}
	
}
