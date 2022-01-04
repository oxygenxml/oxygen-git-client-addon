package com.oxygenxml.git.view.refresh;

import java.util.List;


/**
 * Manage refreshables and notify them. 
 *
 * @see IRefreshable
 * 
 * @author Alex_Smarandache
 *
 */
public interface IRefresher {
	
	/**
	 * Adds the given refreshable to list.
	 * 
	 * @param refreshable The refreshable to add.
	 */
	public void addRefreshable(IRefreshable refreshable);
	
	/**
	 * Remove the given refreshable.
	 * 
	 * @param refreshable The refreshable to remove.
	 */
	public void removeRefreshable(IRefreshable refreshable);
	
	/**
	 * Remove the refreshable that has the given index.
	 * 
	 * @param indexToRemove Index of refreshable to remove.
	 */
	public void removeRefreshable(int indexToRemove);
	
	/**
	 * @return The list with refreshables associate with this refresher.
	 */
	public List<IRefreshable> getRefreshables();
	
	/**
	 * Update all refreshers.
	 */
	public void updateAll(); 
	
}
