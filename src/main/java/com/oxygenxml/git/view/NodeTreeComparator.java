package com.oxygenxml.git.view;

import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;

public class NodeTreeComparator implements Comparator<DefaultMutableTreeNode> {

	public int compare(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {
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
