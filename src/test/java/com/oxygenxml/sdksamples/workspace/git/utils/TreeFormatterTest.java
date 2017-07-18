package com.oxygenxml.sdksamples.workspace.git.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.sdksamples.workspace.git.view.FileTreeModel;

public class TreeFormatterTest {
	
	List<String> paths = new ArrayList<String>();
	
	@Before
	public void init() {
		paths.add("src/add/poc.txt");
		paths.add("src/add/hello.txt");
		paths.add("src/add/java/info.txt");
	}

	@Test
	public void testGetNodeFromString() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test");
		DefaultTreeModel model = new FileTreeModel(root, false);

		for (String string : paths) {
			TreeFormatter.buildTreeFromString(model, string);
		}
		
		DefaultMutableTreeNode node = TreeFormatter.getTreeNodeFromString(model, "src/add/java/info.txt");
		String actual = (String) node.getUserObject();
		String expected = "info.txt";
		
		System.out.println(node.getUserObject());
		
		assertEquals(actual, expected);
	

	}
}
