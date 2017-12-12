package com.oxygenxml.git.view.event;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.GitTreeNode;

/**
 * 
 * Executes Git commands. A higher level wrapper over the GitAccess.
 *  
 * @author Beniamin Savu
 */
public class StageController {

	/**
	 * the git API
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	private List<JTree> trees = new ArrayList<JTree>();

	/**
	 * Executes the given action on the given files.
	 * 
	 * @param filesStatus The files to be processed. 
	 * @param action
	 */
	public void doGitCommand(List<FileStatus> filesStatus, GitCommand action) {
	  // TODO This is something that the tree can do for itself.
	   List<Enumeration<TreePath>> treePathsToRestore = new ArrayList<Enumeration<TreePath>>();
	    for (JTree tree : trees) {
	      GitTreeNode rootNode = (GitTreeNode) tree.getModel().getRoot();
	      TreePath rootTreePath = new TreePath(rootNode);
	      treePathsToRestore.add(tree.getExpandedDescendants(rootTreePath));
	    }

	  if (action == GitCommand.STAGE) {
	    gitAccess.addAll(filesStatus);
	  } else if (action == GitCommand.UNSTAGE) {
	    // Remove from index.
	    gitAccess.resetAll(filesStatus);
	  } else if (action == GitCommand.DISCARD || action == GitCommand.RESOLVE_USING_MINE) {
      gitAccess.resetAll(filesStatus);
      for (FileStatus file : filesStatus) {
        // TODO Test the DISCARD on SUBMODULES.
        if (file.getChangeType() != GitChangeType.SUBMODULE) {
          gitAccess.restoreLastCommitFile(file);
        }
      }
      
      if (action == GitCommand.RESOLVE_USING_MINE) {
        gitAccess.addAll(filesStatus);
      }
      
	  } else if (action == GitCommand.RESOLVE_USING_THEIRS) {
	    for (FileStatus file : filesStatus) {
        gitAccess.reset(file);
        gitAccess.updateWithRemoteFile(file.getFileLocation());
      }
	    
	    gitAccess.addAll(filesStatus);
	  }
	    
	  for (int i = 0; i < trees.size(); i++) {
	    TreeFormatter.restoreLastExpandedPaths(treePathsToRestore.get(i), trees.get(i));
	  }
	}
	

	public void addTree(JTree tree) {
		trees.add(tree);
	}

}
