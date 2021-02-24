package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.RepoUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * This class contains some utility functions for the branches.
 * 
 * @author Bogdan Draghici
 *
 */
public class BranchesUtil {

  /**
   * i18n.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = LogManager.getLogger(BranchesUtil.class.getName());
  
  /**
    * Constructor.
    *
    * @throws UnsupportedOperationException when invoked.
    */
  private BranchesUtil() {
    // Private to avoid instantiations
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }

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
  
  /**
   * Show a message saying why checking out a newly created branch failed.
   * 
   * @param ex Checkout conflict exception. 
   */
  public static void showCannotCheckoutNewBranchMessage(CheckoutConflictException ex) {
    RepositoryState state = null;
    try {
      state = GitAccess.getInstance().getRepository().getRepositoryState();
    } catch (NoRepositorySelected e2) {
      logger.debug(e2, e2);
    }

    if (state != null) {
      String messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH;
      switch (state) {
        case SAFE:
          messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH_BECAUSE_UNCOMMITTED_CHANGES;
          break;
        case MERGING:
          if (getCheckoutConflictAndPullConflictFilesIntersection(ex).isEmpty()) {
            messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH_BECAUSE_UNCOMMITTED_CHANGES;
          } else {
            messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH_WHEN_HAVING_CONFLICTS;
          }
          break;
        default:
          break;
      }
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(messageTag));
    }
  }

  /**
   * Get the intersection of the checkout conflict files and pull conflict files.
   * 
   * @param ex The checkout conflict exception.
   * 
   * @return the intersection set. Never <code>null</code>.
   */
  private static Set<String> getCheckoutConflictAndPullConflictFilesIntersection(CheckoutConflictException ex) {
    // The files that don't allow the checkout to be done
    List<String> checkoutConflictingPaths = ex.getConflictingPaths();
    // The conflicting files after a pull
    Set<String> repoConflictingFiles = GitAccess.getInstance().getConflictingFiles();
    // Only if the the same files are in pull conflict and checkout conflict show
    // the message to resolve the conflict. Otherwise show the one that says to
    // commit or discard the conflicting (talking here about checkout conflicts) changes
    return checkoutConflictingPaths.stream()
        .filter(repoConflictingFiles::contains)
        .collect(Collectors.toSet());
  }
  
  /**
   * Show error message when switching to another branch failed.
   * 
   * @param ex Checkout conflict exception. 
   */
  public static void showBranchSwitchErrorMessage(CheckoutConflictException ex) {
    RepositoryState repoState = null;
    try {
      repoState = GitAccess.getInstance().getRepository().getRepositoryState();
    } catch (NoRepositorySelected e1) {
      logger.error(e1, e1);
    }
    String msg = 
        RepoUtil.isRepoMergingOrRebasing(repoState) 
            && !getCheckoutConflictAndPullConflictFilesIntersection(ex).isEmpty()
          ? TRANSLATOR.getTranslation(Tags.BRANCH_SWITCH_WHEN_REPO_IN_CONFLICT_ERROR_MSG)
          : TRANSLATOR.getTranslation(Tags.BRANCH_SWITCH_CHECKOUT_CONFLICT_ERROR_MSG);
    PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(msg);
  }
  
  /**
   * Check if the given branch already exists.
   * 
   * @param branchName The branch name to check.
   * 
   * @return <code>true</code> if a locals branch with the given name already exists.
   * 
   * @throws NoRepositorySelected 
   */
  public static boolean doesBranchAlreadyExist(String branchName) throws NoRepositorySelected {
    return getLocalBranches().stream().anyMatch((String branch) -> branch.equalsIgnoreCase(branchName));
  }

}
