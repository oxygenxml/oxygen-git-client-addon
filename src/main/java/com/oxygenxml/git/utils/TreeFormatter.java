package com.oxygenxml.git.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.view.GitTreeNode;

/**
 * An utility class for JTree.
 * 
 * @author Beniamin Savu
 *
 */
public class TreeFormatter {
  /**
   * Hidden constructor.
   */
  private TreeFormatter() {
    // Nothing
  }

	/**
	 * Builds a tree from a given forward slash delimited string.
	 * 
	 * @param model
	 *          The tree model
	 * @param str
	 *          The string to build the tree from
	 */
	public static void buildTreeFromString(final DefaultTreeModel model, final String str) {
		// Fetch the root node
		GitTreeNode root = (GitTreeNode) model.getRoot();

		// Split the string around the delimiter
		String[] strings = str.split("/");

		// Create a node object to use for traversing down the tree as it
		// is being created
		GitTreeNode node = root;

		// Iterate of the string array
		for (String s : strings) {
			// Look for the index of a node at the current level that
			// has a value equal to the current string
			int index = childIndex(node, s);

			// Index less than 0, this is a new node not currently present on the tree
			if (index < 0) {
				// Add the new node
				GitTreeNode newChild = new GitTreeNode(s);
				node.insert(newChild, node.getChildCount());
				node = newChild;
			}
			// Else, existing node, skip to the next string
			else {
				node = (GitTreeNode) node.getChildAt(index);
			}
		}
	}

	/**
	 * Returns the index of a child of a given node, provided its string value.
	 * 
	 * @param node
	 *          The node to search its children
	 * @param childValue
	 *          The value of the child to compare with
	 * @return The index
	 */
	public static int childIndex(final GitTreeNode node, final String childValue) {
		Enumeration<GitTreeNode> children = node.children();
		GitTreeNode child = null;
		int index = -1;

		while (children.hasMoreElements() && index < 0) {
			child = children.nextElement();

			if (child.getUserObject() != null && childValue.equals(child.getUserObject())) {
				index = node.getIndex(child);
			}
		}

		return index;
	}

	/**
	 * Finds the node in the tree from a given forward slash delimited string path.
	 * 
	 * @param model
	 *          - The tree model
	 * @param path
	 *          - The string to find the node from
	 * @return The node or <code>null</code> if the file is not present in the tree.
	 */
	public static GitTreeNode getTreeNodeFromString(DefaultTreeModel model, String path) {
		GitTreeNode node = (GitTreeNode) model.getRoot();
		if (node != null && path != null && !path.isEmpty()) {
		  String[] strings = path.split("/");
		  for (String s : strings) {
		    int index = childIndex(node, s);
		    if (index != -1) {
		      node = (GitTreeNode) node.getChildAt(index);
		    } else {
		      node = null;
		      break;
		    }
		  }
		}
		return node;
	}

	/**
	 * Finds the common ancestors from the given selected paths
	 * 
	 * @param selectedPaths
	 *          - The paths selected
	 * @return A List containing the common ancestors
	 */
	public static List<TreePath> getTreeCommonAncestors(TreePath[] selectedPaths) {
		List<TreePath> commonAncestors = new ArrayList<>();
		if (selectedPaths != null) {
			commonAncestors.add(selectedPaths[0]);
			for (int i = 0; i < selectedPaths.length; i++) {
				boolean newPathToAdd = false;
				List<TreePath> pathsToRemove = new ArrayList<>();
				for (TreePath treePath : commonAncestors) {
					if (treePath.isDescendant(selectedPaths[i])) {
						newPathToAdd = false;
						break;
					} else if (selectedPaths[i].isDescendant(treePath)) {
						pathsToRemove.add(treePath);
						newPathToAdd = false;
					} else {
						newPathToAdd = true;
					}
				}
				if (!pathsToRemove.isEmpty()) {
					commonAncestors.removeAll(pathsToRemove);
					commonAncestors.add(selectedPaths[i]);
				} else if (newPathToAdd) {
					commonAncestors.add(selectedPaths[i]);
				}
			}
		}
		return commonAncestors;
	}

	/**
	 * Expands all the nodes from the tree
	 * 
	 * @param tree
	 *          - tree rows to expand
	 * @param startingIndex
	 *          - starting row index
	 * @param rowCount
	 *          - end row index
	 */
	public static void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
		for (int i = startingIndex; i < rowCount; ++i) {
			tree.expandRow(i);
		}

		if (tree.getRowCount() != rowCount) {
			expandAllNodes(tree, rowCount, tree.getRowCount());
		}
	}

	/**
	 * Generates an equivalent String path from a given TreePath
	 * 
	 * @param treePath
	 *          - The path that will be converted to String
	 * @return The String path
	 */
	public static String getStringPath(TreePath treePath) {
		StringBuilder fullPath = new StringBuilder();
		Object[] pathNodes = treePath.getPath();
		for (int j = 1; j < pathNodes.length; j++) {
			if (j == pathNodes.length - 1) {
				fullPath.append(pathNodes[j]);
			} else {
				fullPath.append(pathNodes[j]).append("/");
			}

		}
		return fullPath.toString();
	}

	
	public static List<String> getStringComonAncestor(JTree tree) {
		List<String> selectedFiles = new ArrayList<>();
		TreePath[] selectedPaths = tree.getSelectionPaths();
		List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(selectedPaths);
		String fullPath = "";
		for (TreePath treePath : commonAncestors) {
			fullPath = TreeFormatter.getStringPath(treePath);
			selectedFiles.add(fullPath);
		}
		return selectedFiles;
	}

	/**
	 * Expand all the path from the given expandedPaths and the given tree
	 * 
	 * @param expandedPaths
	 *          - the paths to expand
	 * @param tree
	 *          - the tree on which the paths has to be expanded
	 */
	public static void restoreLastExpandedPaths(Enumeration<TreePath> expandedPaths, JTree tree) {
		if (expandedPaths != null) {
			List<TreePath> paths = Collections.list(expandedPaths);
			for (int i = 0; i < tree.getRowCount(); i++) {
				TreePath currentPath = tree.getPathForRow(i);
				String currentStringPath = TreeFormatter.getStringPath(currentPath);
				for (TreePath treePath : paths) {
					String stringTreePahr = TreeFormatter.getStringPath(treePath);
					if (currentStringPath.equals(stringTreePahr)) {
						tree.expandRow(i);
					}
				}
			}
		}
	}
	
	/**
	 * Converts a file path to the TreePath that indentifies the file in the tree model.
	 * 
	 * @param model  The tree model.
	 * @param path   File path from the working copy.
	 * 
	 * @return A file path to the TreePath.
	 */
	public static TreePath getTreePath(TreeModel model, String path) {
	  String[] strings = path.split("/");
	  Object[] parts = new Object[1 + strings.length];
	  parts[0] = model.getRoot();
	  for (int i = 0; i < strings.length; i++) {
      parts[i + 1] = new GitTreeNode(strings[i]);
    }
	  
	  return new TreePath(parts);
  }
}
