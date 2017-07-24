package com.oxygenxml.git.view;

import java.net.URL;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class DiffHandler {

	private StandalonePluginWorkspace pluginWorkspaceAccess;

	public DiffHandler(StandalonePluginWorkspace pluginWorkspaceAccess) {
		this.pluginWorkspaceAccess = pluginWorkspaceAccess;
	}

	public void makeDiff(URL left, URL right){
		pluginWorkspaceAccess.openDiffFilesApplication(left, right);
	}


}
