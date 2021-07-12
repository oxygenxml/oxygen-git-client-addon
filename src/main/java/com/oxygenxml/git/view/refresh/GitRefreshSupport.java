package com.oxygenxml.git.view.refresh;

/**
 * Synchronizes the staging view with the GIT repository state. 
 *  
 * @author alex_jitianu
 */
public interface GitRefreshSupport {
	/**
	 * Call the refresh support (i.e. perform the actual refresh).
	 */
	public void call();
}
