package com.oxygenxml.git.utils;

/**
 * Synchronizes the staging view with the GIT repository state. 
 *  
 * @author alex_jitianu
 */
public interface GitRefreshSupport {
	
	public void call();
}
