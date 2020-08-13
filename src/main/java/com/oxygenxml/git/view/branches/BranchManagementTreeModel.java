package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.apache.log4j.Logger;

import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.NodeTreeComparator;

@SuppressWarnings("serial")
public class BranchManagementTreeModel extends DefaultTreeModel {

  /**
   * Logger for logging.
   */
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(BranchManagementTreeModel.class);

  /**
   * The branches in the model for the current repository.
   */
  private List<BranchType> branches = Collections.synchronizedList(new ArrayList<>());

  /**
   * Public Constructor
   * 
   * @param root               The root note of the Tree Model.
   * @param repositoryBranches The branches contained in the current repository.
   */
  public BranchManagementTreeModel(String root, List<BranchType> repositoryBranches) {
    super(new GitTreeNode(root != null ? root : ""));

    setBranches(repositoryBranches);
  }

  /**
   * Sets the branches in the model also resets the internal node structure and
   * creates a new one based on the given branches.
   * 
   * @param branchList The branches on which the node structure will be created.
   */
  private void setBranches(List<BranchType> branchList) {
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
  private void insertNodes(List<BranchType> branchesToBeUpdated) {

    for (BranchType branchTypeIterator : branchesToBeUpdated) {
      TreeFormatter.buildTreeFromString(this, branchTypeIterator.getName());
    }
    branches.addAll(branchesToBeUpdated);
    sortTree();
  }

  /**
   * Delete nodes from the tree based on the given branches.
   * 
   * @param branchesToBeUpdated The branches on which the nodes will be deleted.
   */
  private void deleteNodes(List<BranchType> branchesToBeUpdated) {
    for (BranchType branchType : branchesToBeUpdated) {
      GitTreeNode node = TreeFormatter.getTreeNodeFromString(this, branchType.getName());
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
    sortTree();
  }

  /**
   * Sorts the entire tree.
   */
  private void sortTree() {
    GitTreeNode root = (GitTreeNode) getRoot();
    Enumeration<?> e = root.depthFirstEnumeration();
    while (e.hasMoreElements()) {
      GitTreeNode node = (GitTreeNode) e.nextElement();
      if (!node.isLeaf()) {
        sort(node);
      }
    }
  }

  /**
   * Sorts the given node
   * 
   * @param parent The node to be sorted.
   */
  private void sort(GitTreeNode parent) {
    int n = parent.getChildCount();
    List<GitTreeNode> children = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      children.add((GitTreeNode) parent.getChildAt(i));
    }
    Collections.sort(children, new NodeTreeComparator());
    parent.removeAllChildren();
    for (MutableTreeNode node : children) {
      parent.add(node);
    }
  }
}
