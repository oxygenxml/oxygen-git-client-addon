package com.oxygenxml.git.service.entities;

public enum GitChangeType {
	/** Add a new file to the project */
	ADD,

	/** Modify an existing file in the project (content and/or mode) */
	MODIFY,

	/** Delete an existing file from the project */
	DELETE,
	
	CONFLICT;
}
