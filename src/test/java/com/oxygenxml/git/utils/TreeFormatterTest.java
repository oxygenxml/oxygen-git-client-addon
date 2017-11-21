package com.oxygenxml.git.utils;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.junit.Test;

import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.MyNode;

public class TreeFormatterTest {

	@Test
	public void testGetNodeFromString() {
		URL resource = getClass().getClassLoader().getResource(com.oxygenxml.git.constants.ImageConstants.GIT_PUSH_ICON);

		List<String> paths = new ArrayList<String>();
		paths.add("src/add/poc.txt");
		paths.add("src/add/hello.txt");
		paths.add("src/add/java/info.txt");
		paths.add("src/main/java/test.java");
		paths.add("src/main/java/package/file.java");
		paths.add("resources/java/find.txt");
		MyNode root = new MyNode("Test");
		DefaultTreeModel model = new DefaultTreeModel(root);

		for (String string : paths) {
			TreeFormatter.buildTreeFromString(model, string);
		}

		MyNode node = TreeFormatter.getTreeNodeFromString(model, "src/add/java/info.txt");
		String actual = (String) node.getUserObject();
		String expected = "info.txt";

		assertEquals(actual, expected);
	}

	@Test
	public void testGetTreeCommonAncestors() {
		List<Object[]> p = new ArrayList<Object[]>();
		p.add(new Object[] { "src", "add", "poc.txt" });
		p.add(new Object[] { "src", "add", "poc1.txt" });
		p.add(new Object[] { "src", "add", "poc2.txt" });
		p.add(new Object[] { "src", "add", "poc3.txt" });
		p.add(new Object[] { "src", "add", "poc4.txt" });
		p.add(new Object[] { "src", "add", "poc5.txt" });
		p.add(new Object[] { "src", "add" });
		p.add(new Object[] { "resources", "java" });
		p.add(new Object[] { "resources", "java", "fisier1.xml" });
		p.add(new Object[] { "resources", "java", "fisier2.xml" });
		p.add(new Object[] { "resources", "java", "fisier3.xml" });
		p.add(new Object[] { "resources", "java", "fisier4.xml" });
		p.add(new Object[] { "src" });
		p.add(new Object[] { "test", "remove", "file.txt" });
		p.add(new Object[] { "test", "remove", "all", "main.smt" });
		p.add(new Object[] { "test", "remove", "all", "main2.smt" });
		p.add(new Object[] { "test", "remove", "all", "main3.smt" });
		TreePath[] trePaths = new TreePath[p.size()];
		for (int i = 0; i < p.size(); i++) {
			trePaths[i] = new TreePath(p.get(i));
		}

		List<TreePath> actual = TreeFormatter.getTreeCommonAncestors(trePaths);
		List<TreePath> expected = new ArrayList<TreePath>();
		expected.add(new TreePath(new Object[] { "resources", "java" }));
		expected.add(new TreePath(new Object[] { "src" }));
		expected.add(new TreePath(new Object[] { "test", "remove", "file.txt" }));
		expected.add(new TreePath(new Object[] { "test", "remove", "all", "main.smt" }));
		expected.add(new TreePath(new Object[] { "test", "remove", "all", "main2.smt" }));
		expected.add(new TreePath(new Object[] { "test", "remove", "all", "main3.smt" }));

		assertEquals(actual, expected);

	}


}
