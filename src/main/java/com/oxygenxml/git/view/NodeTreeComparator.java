package com.oxygenxml.git.view;

import java.util.Comparator;

/**
 * Comparator for the tree node
 * 
 * @author Beniamin Savu
 *
 */
public class NodeTreeComparator implements Comparator<GitTreeNode> {

  /**
   * @see java.util.Comparator.compare(T, T)
   */
  @Override
	public int compare(GitTreeNode a, GitTreeNode b) {
    int toReturn = 0;
		if (a.isLeaf() && !b.isLeaf()) {
		  toReturn = 1;
		} else if (!a.isLeaf() && b.isLeaf()) {
		  toReturn = -1;
		} else {
			String sa = a.getUserObject().toString();
			String sb = b.getUserObject().toString();
			toReturn = sa.compareToIgnoreCase(sb);
		}
		return toReturn;
	}

}
