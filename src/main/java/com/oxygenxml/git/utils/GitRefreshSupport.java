package com.oxygenxml.git.utils;

import com.oxygenxml.git.view.StagingPanel;

/**
 * Synchronizes the staging view with the GIT repository state. 
 *  
 * @author alex_jitianu
 */
public interface GitRefreshSupport {
	
	public void call();

	public void setPanel(StagingPanel stagingPanel);
}
