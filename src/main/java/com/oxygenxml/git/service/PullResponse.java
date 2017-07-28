package com.oxygenxml.git.service;

import java.util.HashSet;
import java.util.Set;

public class PullResponse {
	private Set<String> conflictingFiles;
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
