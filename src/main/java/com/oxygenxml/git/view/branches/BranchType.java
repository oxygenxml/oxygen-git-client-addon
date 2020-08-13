package com.oxygenxml.git.view.branches;

/**
 * This class is used to store the path to a branch and and its type(local or
 * remote) in a repository.
 * 
 * @author Bogdan Draghici
 *
 */
public class BranchType {

  /**
   * This is the enumeration of the possible types of branches.
   * 
   * @author Bogdan Draghici
   *
   */
  public enum BranchLocation {
    LOCAL, REMOTE
  }

  /**
   * Stores the path to a branch.
   */
  private String name;

  /**
   * The type of the branch.
   */
  private BranchLocation type;

  /**
   * Public Constructor
   * 
   * @param name The path to the branch.
   * @param type The type of the Branch.
   */
  public BranchType(String name, BranchLocation type) {
    this.name = name;
    this.type = type;
  }

  public BranchLocation getType() {
    return type;
  }

  public String getName() {
    return name;
  }
}
