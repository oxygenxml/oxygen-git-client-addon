package com.oxygenxml.git.view.event;

/**
 * The entities that are being observed for new changes
 * 
 * @author intern2
 *
 */
public interface Subject<T> {
	// methods to register and unregister observers
	/**
	 * Adds a new observer
	 * 
	 * @param obj
	 *          - the observer
	 */
	public void addObserver(Observer<T> obj);

	/**
	 * Removes an observer
	 * 
	 * @param obj
	 *          - the observer
	 */
	public void removeObserver(Observer<T> obj);
}
