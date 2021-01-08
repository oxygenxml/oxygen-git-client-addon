package com.oxygenxml.git.utils;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.eclipse.jgit.lib.Constants;

import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.NodeTreeComparator;

/**
 * An utility class for JTree.
 * 
 * @author Beniamin Savu
 *
 */
public class TreeUtil {
  /**
   * Hidden constructor.
   */
  private TreeUtil() {
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
			} else {
			  // Existing node, skip to the next string
				node = (GitTreeNode) node.getChildAt(index);
			}
		}
	}

  /**
   * Builds a tree from a given forward slash delimited string and puts the full
   * path to the node in its user object.
   * 
   * @param model The tree model
   * @param str   The string to build the tree from
   */
  public static void buildTreeFromStringFullPath(final DefaultTreeModel model, final String str) {
    GitTreeNode root = (GitTreeNode) model.getRoot();
    String[] strings = str.split("/");

    // Create a node object to use for traversing down the tree as it
    // is being created
    GitTreeNode node = root;
    StringBuilder currentNodePath = new StringBuilder();
    for (int i = 0; i < strings.length; ++i) {
      currentNodePath.append(strings[i]);
      if (i < strings.length - 1) {
        currentNodePath.append("/");
      }
      // Make sure not to add the refs/ node in the tree.
      if (!currentNodePath.toString().equals(Constants.R_REFS) 
          && !currentNodePath.toString().equals(Constants.HEAD)) {
        // Look for the index of a node at the current level that
        // has a value equal to the current string
        int index = childIndex(node, currentNodePath.toString());

        // Index less than 0, this is a new node not currently present on the tree
        if (index < 0) {
          GitTreeNode newChild = new GitTreeNode(currentNodePath.toString());
          node.insert(newChild, node.getChildCount());
          node = newChild;
        } else {
          // Existing node, skip to the next string
          node = (GitTreeNode) node.getChildAt(index);
        }
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
		Enumeration<TreeNode> children = node.children();
		GitTreeNode child = null;
		int index = -1;

		while (children.hasMoreElements() && index < 0) {
			child = (GitTreeNode) children.nextElement();

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
		List<TreePath> commonAncestors = TreeUtil.getTreeCommonAncestors(selectedPaths);
		String fullPath = "";
		for (TreePath treePath : commonAncestors) {
			fullPath = TreeUtil.getStringPath(treePath);
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
				String currentStringPath = TreeUtil.getStringPath(currentPath);
				for (TreePath treePath : paths) {
					String stringTreePahr = TreeUtil.getStringPath(treePath);
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
	
	 /**
   * Returns all the current expanded paths for a tree.
   * 
   * @param tree The tree used to find the expanded paths.
   * @return The current expanded paths.
   */
  public static Enumeration<TreePath> getLastExpandedPaths(JTree tree) {
    TreeModel treeModel = tree.getModel();
    GitTreeNode rootNode = (GitTreeNode) treeModel.getRoot();
    Enumeration<TreePath> expandedPaths = null;
    if (rootNode != null) {
      TreePath rootTreePath = new TreePath(rootNode);
      expandedPaths = tree.getExpandedDescendants(rootTreePath);
    }
    return expandedPaths;
  }
  
  /**
   * Adds an expand new functionality to the tree: When the user expands a node, the node
   * will expand as long as it has only one child.
   * 
   * @param tree The tree for which to add the listener.
   * @param event The event used to get the tree path.
   */
  public static void expandSingleChildPath(JTree tree, TreeExpansionEvent event) {
    TreePath path = event.getPath();
    TreeModel treeModel = tree.getModel();
    GitTreeNode node = (GitTreeNode) path.getLastPathComponent();
    if (!treeModel.isLeaf(node)) {
      int children = node.getChildCount();
      if (children == 1) {
        GitTreeNode child = (GitTreeNode) node.getChildAt(0);
        TreePath childPath = new TreePath(child.getPath());
        tree.expandPath(childPath);
      }
    }
  }
  
  /**
   * Sorts the entire tree.
   * 
   * @param treeModel The tree model.
   */
  public static void sortGitTree(DefaultTreeModel treeModel) {
    GitTreeNode root = (GitTreeNode) treeModel.getRoot();
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
   * @param node The node to be sorted.
   */
  private static void sort(GitTreeNode node) {
    int childCount = node.getChildCount();
    List<GitTreeNode> children = new ArrayList<>(childCount);
    for (int i = 0; i < childCount; i++) {
      children.add((GitTreeNode) node.getChildAt(i));
    }
    Collections.sort(children, new NodeTreeComparator());
    node.removeAllChildren();
    children.forEach(node::add);
  }
  
  /**
   * Trims the text to fit the given width.
   *  
   * @param initialString Text to trim.
   * @param fontMetrics The font metrics that will be used to render the string.
   * @param maxWidth Maximum available width.
   * 
   * @return The trimmed version.
   */
  public static String getWordToFitInWidth(String initialString, FontMetrics fontMetrics, int maxWidth) {
    int singleRowStringWidth = 0;
    if (fontMetrics != null &&
        initialString != null && 
        fontMetrics.stringWidth(initialString) > maxWidth) {
      char[] charArray = initialString.toCharArray();
      int stringWidth = fontMetrics.stringWidth("...");
      
      for (int i = 0; i < charArray.length; i++) {
        char character = charArray[i];
        // Is Chinese/Japanese/Korean character?
          singleRowStringWidth += fontMetrics.charWidth(character);
          
          // Make sure the three dots have enough space (if we actually need them).
          if (singleRowStringWidth + stringWidth > maxWidth) {
            // There is not enough space for the current word.
            initialString = initialString.substring(0, i) + "...";
            break;
          }
        }
    }
    return initialString;
  }
}
