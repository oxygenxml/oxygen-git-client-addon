package com.oxygenxml.git.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.view.MyNode;

/**
 * An utility class for JTree.
 * 
 * @author Beniamin Savu
 *
 */
public class TreeFormatter {

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
		MyNode root = (MyNode) model.getRoot();

		// Split the string around the delimiter
		String[] strings = str.split("/");

		// Create a node object to use for traversing down the tree as it
		// is being created
		MyNode node = root;

		// Iterate of the string array
		for (String s : strings) {
			// Look for the index of a node at the current level that
			// has a value equal to the current string
			int index = childIndex(node, s);

			// Index less than 0, this is a new node not currently present on the tree
			if (index < 0) {
				// Add the new node
				MyNode newChild = new MyNode(s);
				node.insert(newChild, node.getChildCount());
				node = newChild;
			}
			// Else, existing node, skip to the next string
			else {
				node = (MyNode) node.getChildAt(index);
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
	public static int childIndex(final MyNode node, final String childValue) {
		Enumeration<MyNode> children = node.children();
		MyNode child = null;
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
	 * Finds the node in the tree from a given forward slash delimited string
	 * path.
	 * 
	 * @param model
	 *          - The tree model
	 * @param path
	 *          - The string to find the node from
	 * @return The node
	 */
	public static MyNode getTreeNodeFromString(DefaultTreeModel model, String path) {
		MyNode node = (MyNode) model.getRoot();
		String[] strings = path.split("/");
		for (String s : strings) {
			int index = childIndex(node, s);
			if (index != -1) {
				node = (MyNode) node.getChildAt(index);
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
		List<TreePath> commonAncestors = new ArrayList<TreePath>();
		if (selectedPaths != null) {
			commonAncestors.add(selectedPaths[0]);
			for (int i = 0; i < selectedPaths.length; i++) {
				boolean newPathToAdd = false;
				List<TreePath> pathsToRemove = new ArrayList<TreePath>();
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
				if (pathsToRemove.size() != 0) {
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
		String fullPath = "";
		Object[] pathNodes = treePath.getPath();
		for (int j = 1; j < pathNodes.length; j++) {
			if (j == pathNodes.length - 1) {
				fullPath += pathNodes[j];
			} else {
				fullPath += pathNodes[j] + "/";
			}

		}
		return fullPath;
	}

	
	public static List<String> getStringComonAncestor(JTree tree) {
		List<String> selectedFiles = new ArrayList<String>();
		TreePath[] selectedPaths = tree.getSelectionPaths();
		List<TreePath> commonAncestors = TreeFormatter.getTreeCommonAncestors(selectedPaths);
		String fullPath = "";
		for (TreePath treePath : commonAncestors) {
			fullPath = TreeFormatter.getStringPath(treePath);
			selectedFiles.add(new String(fullPath));
			fullPath = "";
		}
		return selectedFiles;
	}
	
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
}
