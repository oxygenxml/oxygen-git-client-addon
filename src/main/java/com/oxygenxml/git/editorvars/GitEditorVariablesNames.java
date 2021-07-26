package com.oxygenxml.git.editorvars;

/**
 * The names of all the Git editor variables.
 */
public class GitEditorVariablesNames {
  
  private GitEditorVariablesNames() {}
  
  /**
   * The URL of the working copy directory.
   */
  public static final String WORKING_COPY_URL_EDITOR_VAR = "${git(working_copy_url)}";
  
  /**
   * The path to the working copy directory.
   */
  public static final String WORKING_COPY_PATH_EDITOR_VAR = "${git(working_copy_path)}";

  /**
   * The name of the working copy directory.
   */
  public static final String WORKING_COPY_NAME_EDITOR_VAR = "${git(working_copy_name)}";

  /**
   * The full name of the current branch (e.g. "refs/heads/dev").
   */
  public static final String FULL_BRANCH_NAME_EDITOR_VAR = "${git(full_branch_name)}";

  /**
   * The short name of the current branch (e.g. "dev").
   */
  public static final String SHORT_BRANCH_NAME_EDITOR_VAR = "${git(short_branch_name)}";
}
