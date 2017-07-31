package com.oxygenxml.git.view.event;

import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.git.service.GitAccess;

/**
 * Delegates the changing event to all other observers and makes sure that all
 * the observers are properly updated
 * 
 * @author Beniamin Savu
 *
 */
public class StageController implements Observer<ChangeEvent> {

	/**
	 * the git API
	 */
	private GitAccess gitAccess;
	
	/**
	 * List of all subjects registered
	 */
	private List<Subject<ChangeEvent>> subjects = new ArrayList<Subject<ChangeEvent>>();

	/**
	 * List of all observers registered being controlled by this controller(the controller will
	 * delegate some work to them)
	 */
	private List<Observer<ChangeEvent>> observers = new ArrayList<Observer<ChangeEvent>>();

	public StageController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	/**
	 * Register the given subject to this controller and adds this controller as
	 * the observer to it. A subject can only be observed for changes
	 * 
	 * @param subject
	 *          - the subject to be observed by this controller
	 */
	public void registerSubject(Subject<ChangeEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	/**
	 * Register the given observer to this controller
	 * 
	 * @param observer
	 *          - the observer to be registered
	 */
	public void registerObserver(Observer<ChangeEvent> observer) {
		observers.add(observer);
	}

	/**
	 * Removes the given subject from this controller and also remove from the
	 * given subject this controller
	 * 
	 * @param subject
	 *          - the subject to be unregistered
	 */
	public void unregisterSubject(Subject<ChangeEvent> subject) {
		subjects.remove(subject);

		subject.removeObserver(this);
	}

	/**
	 * Removes the given observer from this controller
	 * 
	 * @param observer
	 */
	public void unregisterObserver(Observer<ChangeEvent> observer) {
		observers.remove(observer);
	}

	/**
	 * Called when a file is changing its state(Defined in the StageState enum).
	 * Depending on the new state the file is being added to the staging area or
	 * removed from the staging area. After this all the other observers are
	 * notified with the new changes so they can update
	 * 
	 */
	public void stateChanged(ChangeEvent changeEvent) {
		if (changeEvent.getNewState() == StageState.STAGED) {
			gitAccess.addAll(changeEvent.getFileToBeUpdated());
		} else if (changeEvent.getNewState() == StageState.UNSTAGED) {
			gitAccess.removeAll(changeEvent.getFileToBeUpdated());
		}

		for (Observer<ChangeEvent> observer : observers) {
			observer.stateChanged(changeEvent);
		}
	}

}
