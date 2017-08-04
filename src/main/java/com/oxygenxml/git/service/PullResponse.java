package com.oxygenxml.git.service;

import java.util.HashSet;
import java.util.Set;

/**
 * After a pull is completed this is the response it will generate. The response
 * will have a status and the conflicting files (if any)
 * 
 * @author Beniamin Savu
 *
 */
public class PullResponse {
	/**
	 * The files relative location with the selected repository
	 */
	private Set<String> conflictingFiles;

	/**
	 * The status of the completed pull
	 */
	private PullStatus status;

	public PullResponse(PullStatus status, HashSet<String> conflictingFiles) {
		this.status = status;
		this.conflictingFiles = conflictingFiles;

	}

	public Set<String> getConflictingFiles() {
		return conflictingFiles;
	}

	public void setConflictingFiles(Set<String> conflictingFiles) {
		this.conflictingFiles = conflictingFiles;
	}

	public PullStatus getStatus() {
		return status;
	}

	public void setStatus(PullStatus status) {
		this.status = status;
	}

}
