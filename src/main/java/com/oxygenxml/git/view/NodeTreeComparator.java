package com.oxygenxml.git.view;

import java.util.Comparator;

/**
 * Comparator for the tree node
 * 
 * @author Beniamin Savu
 *
 */
public class NodeTreeComparator implements Comparator<GitTreeNode> {

	public int compare(GitTreeNode a, GitTreeNode b) {
		if (a.isLeaf() && !b.isLeaf()) {
			return 1;
		} else if (!a.isLeaf() && b.isLeaf()) {
			return -1;
		} else {
			String sa = a.getUserObject().toString();
			String sb = b.getUserObject().toString();
			return sa.compareToIgnoreCase(sb);
		}
	}

}
