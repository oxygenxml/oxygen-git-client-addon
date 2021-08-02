package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Logger;

import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.util.TreeUtil;

/**
 * The tree model for the branches.
 * 
 * @author Bogdan Draghici
 *
 */
public class BranchManagementTreeModel extends DefaultTreeModel {

  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(BranchManagementTreeModel.class);

  /**
   * The branches in the model for the current repository.
   */
  private final List<String> branches = Collections.synchronizedList(new ArrayList<>());

  /**
   * Constructor.
   * 
   * @param root               The root note of the Tree Model.
   * @param repositoryBranches The branches contained in the current repository.
   */
  public BranchManagementTreeModel(String root, List<String> repositoryBranches) {
    super(new GitTreeNode(root != null ? root : ""));

    setBranches(repositoryBranches);
  }

  /**
   * Sets the branches in the model also resets the internal node structure and
   * creates a new one based on the given branches.
   * 
   * @param branchList The branches on which the node structure will be created.
   */
  private void setBranches(List<String> branchList) {
    if (branchList == null) {
      branchList = Collections.emptyList();
    }

    deleteNodes(branches);
    insertNodes(branchList);

    fireTreeStructureChanged(this, null, null, null);
  }

  /**
   * Insert nodes to the tree based on the given branches.
   * 
   * @param branchesToBeUpdated The branches on which the nodes will be created.
   */
  private void insertNodes(List<String> branchesToBeUpdated) {
    branchesToBeUpdated.forEach((String branchName) -> TreeUtil.buildTreeFromStringFullPath(this, branchName));
    branches.addAll(branchesToBeUpdated);
    TreeUtil.sortGitTree(this);
  }

  /**
   * Delete nodes from the tree based on the given branches.
   * 
   * @param branchesToBeUpdated The branches on which the nodes will be deleted.
   */
  private void deleteNodes(List<String> branchesToBeUpdated) {
    for (String branchName : branchesToBeUpdated) {
      GitTreeNode node = TreeUtil.getTreeNodeFromString(this, branchName);
      while (node != null && node.getParent() != null) {
        GitTreeNode parentNode = (GitTreeNode) node.getParent();
        if (node.getSiblingCount() != 1) {
          parentNode.remove(node);
          break;
        } else {
          parentNode.remove(node);
        }
        node = parentNode;
      }
    }
    branches.removeAll(branchesToBeUpdated);
    TreeUtil.sortGitTree(this);
  }

  /**
   * Return the branch from the given path
   * 
   * @param path The path
   * @return The branch
   */
  public String getBranchByPath(String path) {
    String toReturn = null;
    synchronized (branches) {
      for (String fileStatus : branches) {
        if (path.equals(fileStatus)) {
          toReturn = fileStatus;
          break;
        }
      }
    }
    return toReturn;
  }
}
