package com.oxygenxml.git.view.event;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Event created when a file is changing its state, from staged to unstaged or
 * from unstaged to staged
 * 
 * @author Beniamin Savu
 *
 */
public class ChangeEvent {
	
	/**
	 * The state of the command that generated the change.
	 */
	private GitCommandEvent gitCmdState;
	
	/**
	 * The files that are changing their state
	 */
	private Collection<String> changedFiles;

	/**
	 * Object representing a state change.
	 * 
	 * @param gitCmdState        The state of the command that generated the change.
	 * @param filesToBeUpdated   The files that are changing their state.
	 */
	public ChangeEvent(GitCommandEvent gitCmdState, Collection<String> affectedFiles) {
		this.gitCmdState = gitCmdState;
		this.changedFiles = affectedFiles;
	}

	public GitCommandEvent getGitCommandState() {
		return gitCmdState;
	}

	public List<FileStatus> getOldStates() {
	  List<FileStatus> fss = new LinkedList<>();
	  for (Iterator<String> iterator = changedFiles.iterator(); iterator.hasNext();) {
      String path = iterator.next();
      fss.add(new FileStatus(GitChangeType.UNKNOWN, path));
    }
	  
	  return fss;
  }
	
	@Override
	public String toString() {
	  return " new state: " + gitCmdState + " files: " + changedFiles;
	}
	
	public Collection<String> getChangedFiles() {
    return changedFiles;
  }

}