package com.oxygenxml.git.protocol;

/**
 * Constants for the location of a git file content.
 * Informs where to find the file content.
 * 
 * @author Beniamin Savu
 */
public class VersionIdentifier {
  /**
   * Hidden constructor.
   */
  private VersionIdentifier() {
    // Nothing
  }
  
	/**
	 * Used when a conflict occurs. My own version of the file.
	 * 
	 * The file having the LOCAL constant shows that this file content is located
	 * in the user repository.
	 */
	public static final String MINE = "Mine";
	
	/**
   * Used when a trying to rebase. The URL that appears on the left.
   * 
   * Rebase works by doing a series of cherry-picks, into an anonymous branch (detached HEAD mode). The target branch is 
   * the anonymous branch and the merge-from branch is your original (pre-rebase) branch: so "--ours" means the anonymous 
   * one rebase is building while "--theirs" means "our branch being rebased".
   * 
   * This version represents the version of the file in which the rebase is resolved.
   */
  public static final String MINE_RESOLVED = "MineResolved";
  
	/**
   * Used when trying to rebase. The URL that appears on the right.
   * 
   * Rebase works by doing a series of cherry-picks, into an anonymous branch (detached HEAD mode). The target branch is 
   * the anonymous branch and the merge-from branch is your original (pre-rebase) branch: so "--ours" means the anonymous 
   * one rebase is building while "--theirs" means "our branch being rebased".
   * 
   * This version represents the original version of the file that was in the local branch.
   */
  public static final String MINE_ORIGINAL = "MineOriginal";
  
	/**
	 * Used when a conflict occurs. The remote version of the file (from the remote server).
	 * 
	 * The file having the REMOTE constant shows that this file content is located
	 * in the remote repository.
	 */
	public static final String THEIRS = "Theirs";

	/**
	 * Used when a conflict occurs. The common base, when the two versions forked (MINE, THEIRS).
	 * 
	 * The file having the BASE constant shows that this file content is from the
	 * base
	 */
	public static final String BASE = "Base";

	/**
	 * The last commit in the local repository. HEAD.
	 * 
	 * The file having the LAST_COMMIT constant shows that the file content is
	 * from the last commit in the local repository.
	 */
	public static final String LAST_COMMIT = "LastCommit";
	
	/**
	 * Will first search for the file in the index and, if not found,
	 * will search in the last commit.
	 */
	public static final String INDEX_OR_LAST_COMMIT = "IndexOrLastCommit";

	/**
	 * Current submodule.
	 */
	public static final String CURRENT_SUBMODULE = "CurrentSubmodule";

	/**
	 * Previous submodule.
	 */
	public static final String PREVIOUSLY_SUBMODULE = "PreviousSubmodule";
}
