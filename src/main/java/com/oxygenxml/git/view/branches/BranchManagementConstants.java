package com.oxygenxml.git.view.branches;

/**
 * Contains the constants for the branch management.
 * 
 * @author Bogdan Draghici
 */
public final class BranchManagementConstants {

  /**
   * Hidden constructor.
   */
  private BranchManagementConstants() {}
  
  /**
   * "Local".
   */
  public static final String LOCAL = "Local";
  
  /**
   * "Remote".
   */
  public static final String REMOTE = "Remote";
  
  /**
   * "Remote/origin".
   */
  public static final String REMOTE_ORIGIN = "Remote/origin";
  
  /**
   * The level from which to start adding local branches to the branch path in the branches tree.
   */
  public static final int LOCAL_BRANCH_NODE_TREE_LEVEL = 2;
  
  /**
   * The level from which to start adding remote branches to the branch path in the branches tree.
   */
  public static final int REMOTE_BRANCH_NODE_TREE_LEVEL = 3;
}
