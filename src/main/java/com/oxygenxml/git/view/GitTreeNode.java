package com.oxygenxml.git.view;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class GitTreeNode extends DefaultMutableTreeNode {
	
	
	public GitTreeNode(String rootFolder) {
		super(rootFolder);
	}
	
	public GitTreeNode(TreePath path){
		super(path);
	}

	@Override
	public boolean equals(Object obj) {
		
		boolean equals = false;
		
		if(obj instanceof GitTreeNode){
			String thisUserObgject = (String) this.getUserObject();
			String objUserObject = (String) ((GitTreeNode) obj).getUserObject();
			equals = thisUserObgject.equals(objUserObject);
			return equals;
		} else {
			equals = super.equals(obj);
		}
		
		return equals;
	}
	
	@Override
	public int hashCode() {
		String thisUserObgject = (String) this.getUserObject();
		
		return thisUserObgject.hashCode();
	}
}
