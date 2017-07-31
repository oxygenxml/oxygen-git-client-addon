package com.oxygenxml.git.service.entities;

/**
 * Used in File Status to mark the file in which of the following states is
 * 
 * @author Beniamin Savu
 *
 */
public enum GitChangeType {
	/** Add a new file to the project */
	ADD,

	/** Modify an existing file in the project (content and/or mode) */
	MODIFY,

	/** Delete an existing file from the project */
	DELETE,

	/** File is in conflict */
	CONFLICT;
}
