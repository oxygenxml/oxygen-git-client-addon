package com.oxygenxml.git.view.branches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;

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
  private static final Logger LOGGER = LogManager.getLogger(BranchesUtil.class.getName());
  
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
      List<Ref> branches = new ArrayList<>(GitAccess.getInstance().getLocalBranchList());
      branchList = branches.stream()
          .map(t -> createBranchPath(t.getName(), BranchManagementConstants.LOCAL_BRANCH_NODE_TREE_LEVEL))
          .collect(Collectors.toList());
    }
    return branchList;
  }

  
  /**
   * Creates a list with remote branches path for the current repository.
   * 
   * @return The list of remote branches.
   * 
   * @throws NoRepositorySelected when no repo is selected.
   */
  public static List<String> getRemoteBranches() throws NoRepositorySelected {
    List<String> branchList = new ArrayList<>();
    Repository repository = GitAccess.getInstance().getRepository();
    if (repository != null) {
      List<Ref> branches = new ArrayList<>(GitAccess.getInstance().getRemoteBrachListForCurrentRepo());
      branchList = branches.stream().map(Ref::getName).collect(Collectors.toList());
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
   */
  public static void showCannotCheckoutNewBranchMessage() {
    RepositoryState state = null;
    try {
      state = GitAccess.getInstance().getRepository().getRepositoryState();
    } catch (NoRepositorySelected e2) {
      LOGGER.debug(e2, e2);
    }

    if (state != null) {
      String messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH;
      switch (state) {
        case SAFE:
          messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH_BECAUSE_UNCOMMITTED_CHANGES;
          break;
        case MERGING:
        case REVERTING:
          messageTag = Tags.CANNOT_CHECKOUT_NEW_BRANCH_WHEN_HAVING_CONFLICTS;
          break;
        default:
          break;
      }
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(TRANSLATOR.getTranslation(messageTag));
    }
  }

  /**
   * Show error message when switching to another branch failed.
   */
  public static void showBranchSwitchErrorMessage() {
    RepositoryState repoState = null;
    try {
      repoState = GitAccess.getInstance().getRepository().getRepositoryState();
    } catch (NoRepositorySelected e1) {
      LOGGER.error(e1, e1);
    }
    String msg = 
        RepoUtil.isUnfinishedConflictState(repoState) 
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

  /**
   * Rewrites a +refs/heads/hot:refs/remotes/origin/hot to a wildcard variant:
   * +refs/heads/*:refs/remotes/origin/*
   * 
   * @param refSpecString A ref spec.
   * 
   * @return A wildcard variant of the given refspec, if the the refspec does not contain wildcards.
   */
  public static Optional<String> fixupFetch(String refSpecString) {
    if (refSpecString != null) {
      RefSpec refSpec = new RefSpec(refSpecString);

      if (refSpec.getSource() != null &&
          refSpec.getDestination() != null &&
          !refSpec.isWildcard()) {
        int lastIndexOf = refSpec.getSource().lastIndexOf('/');
        String newSource = refSpec.getSource().substring(0, lastIndexOf) + "/*";
        lastIndexOf = refSpec.getDestination().lastIndexOf('/');
        String newDestination = refSpec.getDestination().substring(0, lastIndexOf) + "/*";


        final StringBuilder r = new StringBuilder();
        if (refSpec.isForceUpdate()) {
          r.append('+');
        }
        r.append(newSource);
        r.append(':');
        r.append(newDestination);

        return Optional.of(r.toString());
      }
    }

    return Optional.empty();
  }
  
  /**
   * Rewrites a +refs/heads/hot:refs/remotes/origin/hot to a wildcard variant:
   * +refs/heads/*:refs/remotes/origin/*
   * 
   * @param config A git configuration.
   * 
   * @throws IOException Unable to save the new fetch value.
   */
  public static void fixupFetchInConfig(StoredConfig config) throws IOException {
    String value = config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, Constants.DEFAULT_REMOTE_NAME, ConfigConstants.CONFIG_FETCH_SECTION);
    
    Optional<String> fixupFetch = fixupFetch(value);
    if (fixupFetch.isPresent()) {
      config.setString(
          ConfigConstants.CONFIG_REMOTE_SECTION, 
          Constants.DEFAULT_REMOTE_NAME, 
          ConfigConstants.CONFIG_FETCH_SECTION, 
          fixupFetch.get());
      
      try {
        config.save();
      } catch (IOException ex) {
        throw new IOException("Failed to update fetch configuration to use wildcards. "
            + "Changes in the remote branch will not be visible. "
            + "To fix this, please edit the .git/config file and replace " + value + " with " + fixupFetch.get(), ex);
      }
    }
  }
  
  
}
