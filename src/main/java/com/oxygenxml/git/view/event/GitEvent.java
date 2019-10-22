package com.oxygenxml.git.view.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * Git event.
 */
public class GitEvent {
	
	/**
	 * The Git command.
	 */
	private GitCommand gitCmd;
	
	/**
	 * The state of the Git command that triggered the event:
	 * started, ended, failed, etc.
	 */
	private	GitCommandState gitCmdState;
	
	/**
	 * The files affected by the Git event.
	 */
	private Collection<String> affectedFiles = Collections.<String> emptyList();
	
	 /**
   * Object representing a state change.
   * 
   * @param gitCmd         The Git command.
   * @param gitCmdState    The state of the git command: started, ended, failed, etc.
   */
  public GitEvent(
      GitCommand gitCmd, 
      GitCommandState gitCmdState) {
    this.gitCmd = gitCmd;
    this.gitCmdState = gitCmdState;
  }

	/**
	 * Object representing a state change.
	 * 
	 * @param gitCmd         The Git command.
	 * @param gitCmdState    The state of the git command: started, ended, failed, etc.
	 * @param affectedFiles  The files that are changing their state.
	 */
	public GitEvent(
	    GitCommand gitCmd, 
	    GitCommandState gitCmdState,
	    Collection<String> affectedFiles) {
		this.gitCmd = gitCmd;
    this.gitCmdState = gitCmdState;
		this.affectedFiles = affectedFiles;
	}

	public GitCommand getGitCommand() {
		return gitCmd;
	}

	public GitCommandState getGitComandState() {
	  return gitCmdState;
	}

	public Collection<String> getAffectedFiles() {
	  return affectedFiles;
	}

	public List<FileStatus> getOldAffectedFiles() {
	  List<FileStatus> fss = new LinkedList<>();
	  for (Iterator<String> iterator = affectedFiles.iterator(); iterator.hasNext();) {
      String path = iterator.next();
      fss.add(new FileStatus(GitChangeType.UNKNOWN, path));
    }
	  return fss;
  }
	
	@Override
	public String toString() {
	  return "Command: " + gitCmd 
	      + ", Command state: " + gitCmdState
	      + ", Affected files: " + affectedFiles;
	}
	
}