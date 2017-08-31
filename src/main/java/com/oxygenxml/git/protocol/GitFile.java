package com.oxygenxml.git.protocol;

/**
 * Constants for the location of a git file content. Informs where to find the
 * file content
 * 
 * @author Beniamin Savu
 *
 */
public class GitFile {
	/**
	 * The file having the LOCAL constant shows that this file content is located
	 * in the user repository
	 */
	public static final String LOCAL = "Local";

	/**
	 * The file having the REMOTE constant shows that this file content is located
	 * in the remote reposiory
	 */
	public static final String REMOTE = "Remote";

	/**
	 * The file having the BASE constant shows that this file content is from the
	 * base
	 */
	public static final String BASE = "Base";

	/**
	 * The file having the LAST_COMMIT constant shows that the file content is
	 * from the last commit
	 */
	public static final String LAST_COMMIT = "LastCommit";

	public static final String CURRENT_SUBMODULE = "CurrentSubmodule";

	public static final String PREVIOUSLY_SUBMODULE = "PreviousSubmodule";
}
