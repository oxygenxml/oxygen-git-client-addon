package com.oxygenxml.sdksamples.workspace.git.view.event;

import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;

/**
 * Delegates the changing event to all other observers and makes sure that all
 * the observers are properly updated
 * 
 * @author intern2
 *
 */
public class StageController implements Observer<ChangeEvent> {

	private GitAccess gitAccess;

	private List<Subject<ChangeEvent>> subjects = new ArrayList<Subject<ChangeEvent>>();
	private List<Observer<ChangeEvent>> observers = new ArrayList<Observer<ChangeEvent>>();

	public StageController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public void registerSubject(Subject<ChangeEvent> subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	public void registerObserver(Observer<ChangeEvent> subject) {
		observers.add(subject);
	}

	public void unregisterSubject(Subject<ChangeEvent> subject) {
		subjects.remove(subject);

		subject.removeObserver(this);
	}

	public void unregisterObserver(Observer<ChangeEvent> subject) {
		observers.remove(subject);
	}

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
