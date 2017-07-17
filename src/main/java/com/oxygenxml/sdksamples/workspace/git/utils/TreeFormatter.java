package com.oxygenxml.sdksamples.workspace.git.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class TreeFormatter {

	private String prefixPath;

	public TreeFormatter() {
		this.prefixPath = "";
	}

	public List<String> search(String path) {
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		String tempPrefixPath = prefixPath;
		prefixPath += rootFolder.getName() + "/";

		List<String> fileNames = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				fileNames.addAll(search(listOfFiles[i].getAbsolutePath()));
			} else if (listOfFiles[i].isFile()) {
				fileNames.add(prefixPath + listOfFiles[i].getName());
			}
		}
		prefixPath = tempPrefixPath;
		return fileNames;
	}

	public DefaultMutableTreeNode generateTreeScruture(String path) {
		File rootFolder = new File(path);
		File[] listOfFiles = rootFolder.listFiles();

		String tempPrefixPath = prefixPath;
		prefixPath += rootFolder.getName() + "/";
		DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(rootFolder.getName());

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory()) {
				DefaultMutableTreeNode directory = generateTreeScruture(listOfFiles[i].getAbsolutePath());
				localRoot.add(directory);
			}
		}

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				DefaultMutableTreeNode file = new DefaultMutableTreeNode(listOfFiles[i].getName());
				localRoot.add(file);
			}
		}

		prefixPath = tempPrefixPath;

		return localRoot;
	}

	 /**
   * Builds a tree from a given forward slash delimited string.
   * 
   * @param model The tree model
   * @param str The string to build the tree from
   */
	public static void buildTreeFromString(final DefaultTreeModel model, final String str) {
		// Fetch the root node
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

		// Split the string around the delimiter
		String[] strings = str.split("/");

		// Create a node object to use for traversing down the tree as it
		// is being created
		DefaultMutableTreeNode node = root;

		// Iterate of the string array
		for (String s : strings) {
			// Look for the index of a node at the current level that
			// has a value equal to the current string
			int index = childIndex(node, s);

			// Index less than 0, this is a new node not currently present on the tree
			if (index < 0) {
				// Add the new node
				DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(s);
				node.insert(newChild, node.getChildCount());
				node = newChild;
			}
			// Else, existing node, skip to the next string
			else {
				node = (DefaultMutableTreeNode) node.getChildAt(index);
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
	public static int childIndex(final DefaultMutableTreeNode node, final String childValue) {
		Enumeration<DefaultMutableTreeNode> children = node.children();
		DefaultMutableTreeNode child = null;
		int index = -1;

		while (children.hasMoreElements() && index < 0) {
			child = children.nextElement();

			if (child.getUserObject() != null && childValue.equals(child.getUserObject())) {
				index = node.getIndex(child);
			}
		}

		return index;
	}

}
