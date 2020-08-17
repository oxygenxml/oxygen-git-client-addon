package com.oxygenxml.git.view.branches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;

import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.NodeTreeComparator;

/**
 * The tree model for the branches.
 * 
 * @author Bogdan Draghici
 *
 */
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
  private List<String> branches = Collections.synchronizedList(new ArrayList<>());

  /**
   * Public Constructor
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

    for (String branchName : branchesToBeUpdated) {
      TreeFormatter.buildTreeFromString(this, branchName);
    }
    branches.addAll(branchesToBeUpdated);
    sortTree();
  }

  /**
   * Delete nodes from the tree based on the given branches.
   * 
   * @param branchesToBeUpdated The branches on which the nodes will be deleted.
   */
  private void deleteNodes(List<String> branchesToBeUpdated) {
    for (String branchName : branchesToBeUpdated) {
      GitTreeNode node = TreeFormatter.getTreeNodeFromString(this, branchName);
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
   * Searches in a tree for the branches that contains a given text and updates it
   * with the branches that contains the text.
   * 
   * @param branchesTree    The tree to be filtered.
   * @param text            The text used to filter the tree.
   * @param removedBranches A list with the branches that have been removed
   *                        because of the filtering.
   */
  public void filterForTree(JTree branchesTree, String text, List<String>removedBranches) {
    addFromRemovedBranches(text, removedBranches);
    removeFromCurrentBranches(branchesTree, text, removedBranches );
    sortTree();
    fireTreeStructureChanged(this, null, null, null);
    TreeFormatter.expandAllNodes(branchesTree, 0, branchesTree.getRowCount());
  }

  /**
   * Remove the branches that do not contain in their name a text given and add
   * those branches to a list of removed branches.
   * 
   * @param branchesTree    The tree to be modified.
   * 
   * @param text            The text used to filter which branches remain and
   *                        which are removed.
   * @param removedBranches A list with the branches that have been removed
   *                        because of the filtering.
   */
  private void removeFromCurrentBranches(JTree branchesTree, String text, List<String> removedBranches ) {
    //Create a new list in which to add the branches that are removed now.
    List<String> branchesToBeRemoved = new ArrayList<>();
    GitTreeNode root = (GitTreeNode) this.getRoot();
    Enumeration<?> depthFirstEnumeration = root.depthFirstEnumeration();
    while (depthFirstEnumeration.hasMoreElements()) {
      GitTreeNode node = (GitTreeNode) depthFirstEnumeration.nextElement();
      //Can remove a node only if it is a branch(leaf).
      if (node.isLeaf()) {
        //Get the name of the branch.
        String userObject = (String) node.getUserObject();
        if (!userObject.contains(text)) {
          TreeNode[] path = node.getPath();
          StringBuilder branchPath = new StringBuilder();
          //Create the path of the node without the root element.
          for (int i = 1; i < path.length; ++i) {
            branchPath.append(path[i].toString());
            if (i + 1 < path.length) {
              branchPath.append("/");
            }
          }
          //Add to the list of the branches to be removed only if it is not the root node
          if (path.length > 1) {
            branchesToBeRemoved.add(branchPath.toString());
          }
        }
      }
    }
    //Add the branches to be removed to the ones that were also removed.
    removedBranches.addAll(branchesToBeRemoved);
    deleteNodes(branchesToBeRemoved);
  }
  
  /**
   * Inspects the branches that have been removed if they contain a given text so that they are eligible to be added back to the tree.
   * @param text The text used to see if any of the removed branches contains it.
   * @param removedBranches The list of branches that have been removed from the tree.
   */
  private void addFromRemovedBranches(String text, List<String>removedBranches) {
    // Create a new list in which to add the branches that contain a given text so
    // that they are eligible to be added to the tree.
    List<String> branchesToBeAdded = new ArrayList<>();
    Iterator<String> iterator = removedBranches.iterator();
    while (iterator.hasNext()) {
      String node = iterator.next();
      if (node.contains(text)) {
        branchesToBeAdded.add(node);
        iterator.remove();
      }
    }
    insertNodes(branchesToBeAdded);
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
