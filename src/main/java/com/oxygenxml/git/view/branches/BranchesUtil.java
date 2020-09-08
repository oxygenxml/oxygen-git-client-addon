package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;

/**
 * This class contains some utility functions for the branches.
 * 
 * @author Bogdan Draghici
 *
 */
public class BranchesUtil {

  /**
   * Creates a list with local branches short names for the current repository.
   * 
   * @return The list of local branches.
   * 
   * @throws NoRepositorySelected
   */
  public static List<String> getLocalBranches() throws NoRepositorySelected {
    List<String> branchList = new ArrayList<>();
    Repository repository = GitAccess.getInstance().getRepository();
    if (repository != null) {
      List<Ref> branches = new ArrayList<>();
      branches.addAll(GitAccess.getInstance().getLocalBranchList());
      branchList = branches.stream()
          .map(t -> createBranchPath(t.getName(), BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL))
          .collect(Collectors.toList());
    }
    return branchList;
  }

  /**
   * Creates the path to a branch without having its type node, starting from the
   * full path of the node that contains the branch.
   * 
   * @param nodePath                The path of the node that contains the branch.
   * @param startingIndexBranchType The position from which to start to add to the
   *                                branch path, depending on type of the branch.
   *                                This parameter can only be
   *                                {@link com.oxygenxml.git.view.branches.BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL}
   *                                or
   *                                {@link com.oxygenxml.git.view.branches.BranchManagementConstants.REMOTE_BRANCH_NODE_TREE_LEVEL}
   * 
   * @return The branch path in string format.
   */
  public static String createBranchPath(String nodePath, int startingIndexBranchType) {
    StringBuilder branchPath = new StringBuilder();
    String[] split = nodePath.split("/");
    for (int i = startingIndexBranchType; i < split.length; i++) {
      branchPath.append(split[i]);
      if (i < split.length - 1) {
        branchPath.append("/");
      }
    }
    return branchPath.toString();
  }

  /**
   * Creates a list with all branches, local and remote, for the current repository.
   * 
   * @return The list of all branches. Never <code>null</code>.
   * 
   * @throws NoRepositorySelected
   */
  public static List<String> getAllBranches() throws NoRepositorySelected {
    List<String> branchList = new ArrayList<>();
    Repository repository = GitAccess.getInstance().getRepository();
    if (repository != null) {
      List<Ref> branches = new ArrayList<>();
      branches.addAll(GitAccess.getInstance().getLocalBranchList());
      branches.addAll(GitAccess.getInstance().getRemoteBrachListForCurrentRepo());
      branchList = branches.stream().map(Ref::getName).collect(Collectors.toList());
    }
    return branchList;
  }

}
