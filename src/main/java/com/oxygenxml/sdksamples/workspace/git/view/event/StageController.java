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
public class StageController implements Observer {

	private GitAccess gitAccess;

	private List<Subject> subjects = new ArrayList<Subject>();
	private List<Observer> observers = new ArrayList<Observer>();

	public StageController(GitAccess gitAccess) {
		this.gitAccess = gitAccess;
	}

	public void registerSubject(Subject subject) {
		subjects.add(subject);

		subject.addObserver(this);
	}

	public void registerObserver(Observer subject) {
		observers.add(subject);
	}

	public void unregisterSubject(Subject subject) {
		subjects.remove(subject);

		subject.removeObserver(this);
	}

	public void unregisterObserver(Observer subject) {
		observers.remove(subject);
	}

	@Override
	public void stateChanged(ChangeEvent changeEvent) {
		if (changeEvent.getNewState() == StageState.STAGED) {
			gitAccess.addAll(changeEvent.getFileToBeUpdated());
		} else if (changeEvent.getNewState() == StageState.UNSTAGED) {
			gitAccess.removeAll(changeEvent.getFileToBeUpdated());
		}

		for (Observer observer : observers) {
			observer.stateChanged(changeEvent);
		}
	}

}
