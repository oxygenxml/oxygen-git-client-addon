package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.GitCommand;

/**
 * Custom tree model
 * 
 * @author Beniamin Savu
 *
 */
public class StagingResourcesTreeModel extends DefaultTreeModel {

	/**
	 * The files in the model
	 */
	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

  /**
   * <code>true</code> if this model presents the resources inside the index.
   * <code>false</code> if it presents the modified resources that can be put in the index.
   */
	private boolean inIndex;

	/**
	 * 
	 * @param root
	 * @param forStaging
	 * @param filesStatus
	 */
	public StagingResourcesTreeModel(TreeNode root, boolean forStaging, List<FileStatus> filesStatus) {
		super(root);
		this.inIndex = forStaging;
		this.filesStatus = filesStatus;
	}

	public void stateChanged(ChangeEvent changeEvent) {
		List<FileStatus> oldStates = changeEvent.getOldStates();
    List<FileStatus> newStates = 
        inIndex ? 
            GitAccess.getInstance().getStagedFile(changeEvent.getChangedFiles()) :
            GitAccess.getInstance().getUnstagedFiles(changeEvent.getChangedFiles()); 
    
    if (changeEvent.getCommand() == GitCommand.STAGE) {
			if (inIndex) {
				insertNodes(newStates);
			} else {
				deleteNodes(oldStates);
			}
		} else if (changeEvent.getCommand() == GitCommand.UNSTAGE) {
			if (inIndex) {
				deleteNodes(oldStates);
			} else {
	       // Things were taken out of the INDEX. 
        // The same resource might be present in the UnStaged and INDEX. Remove old states.
			  deleteNodes(oldStates);
				insertNodes(newStates);
			}
		} else if (changeEvent.getCommand() == GitCommand.COMMIT) {
			if (inIndex) {
				deleteNodes(filesStatus);
				filesStatus.clear();
			}
		} else if (changeEvent.getCommand() == GitCommand.DISCARD) {
			deleteNodes(oldStates);
		} else if (changeEvent.getCommand() == GitCommand.MERGE_RESTART) {
      filesStatus.clear();
      List<FileStatus> fileStatuses = inIndex ? GitAccess.getInstance().getStagedFile() :
        GitAccess.getInstance().getUnstagedFiles();
      insertNodes(fileStatuses);
    }

		fireTreeStructureChanged(this, null, null, null);
	}

	/**
	 * Insert nodes to the tree based on the given files
	 * 
	 * @param fileToBeUpdated
	 *          - the files on which the nodes will be created
	 */
	private void insertNodes(List<FileStatus> fileToBeUpdated) {

		for (FileStatus fileStatus : fileToBeUpdated) {
			TreeFormatter.buildTreeFromString(this, fileStatus.getFileLocation());
		}
		filesStatus.addAll(fileToBeUpdated);
		sortTree();
	}

	/**
	 * Delete nodes from the tree based on the given files
	 * 
	 * @param fileToBeUpdated
	 *          - the files on which the nodes will be deleted
	 */
	private void deleteNodes(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			GitTreeNode node = TreeFormatter.getTreeNodeFromString(this, fileStatus.getFileLocation());
			while (node.getParent() != null) {
				GitTreeNode parentNode = (GitTreeNode) node.getParent();
				if (node.getSiblingCount() != 1) {
					parentNode.remove(node);
					break;
				} else {
					parentNode.remove(node);
				}
				node = parentNode;
			}
		}
		filesStatus.removeAll(fileToBeUpdated);
		sortTree();
	}

	/**
	 * Return the file from the given path
	 * 
	 * @param path
	 *          - the path
	 * @return the file
	 */
	public FileStatus getFileByPath(String path) {
		for (FileStatus fileStatus : filesStatus) {
			if (path.equals(fileStatus.getFileLocation())) {
				return fileStatus;
			}
		}
		return null;
	}

	/**
	 * Return the files from the given paths
	 * 
	 * @param selectedPaths
	 *          - the paths
	 * @return a list containing the files from the path
	 */
	public List<FileStatus> getFilesByPaths(List<String> selectedPaths) {
		List<FileStatus> containingPaths = new ArrayList<FileStatus>();
		for (String path : selectedPaths) {
			for (FileStatus fileStatus : filesStatus) {
				if (fileStatus.getFileLocation().startsWith(path)) {
					containingPaths.add(new FileStatus(fileStatus));
				}
			}
		}
		return containingPaths;
	}
	
	/**
   * Return the files corresponding to leaves from the given paths.
   * 
   * @param selectedPaths The selected paths.
   * 
   * @return a list containing the files from the path.
   */
  public List<FileStatus> getFileLeavesByPaths(List<String> selectedPaths) {
    List<FileStatus> containingPaths = new ArrayList<FileStatus>();
    for (String path : selectedPaths) {
      for (FileStatus fileStatus : filesStatus) {
        if (fileStatus.getFileLocation().equals(path)) {
          containingPaths.add(new FileStatus(fileStatus));
        }
      }
    }
    return containingPaths;
  }

	/**
	 * Sets the files in the model also resets the internal node structure and
	 * creates a new one based on the given files
	 * 
	 * @param filesStatus
	 *          - the files on which the node structure will be created
	 */
	public void setFilesStatus(List<FileStatus> filesStatus) {
		deleteNodes(this.filesStatus);
		this.filesStatus.clear();
		insertNodes(filesStatus);
		fireTreeStructureChanged(this, null, null, null);
	}

	/**
	 * Sorts the entire tree
	 */
	private void sortTree() {
		GitTreeNode root = (GitTreeNode) getRoot();
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
	 * @param parent
	 *          - the node to be sorted
	 */
	private void sort(GitTreeNode parent) {
		int n = parent.getChildCount();
		List<GitTreeNode> children = new ArrayList<GitTreeNode>(n);
		for (int i = 0; i < n; i++) {
			children.add((GitTreeNode) parent.getChildAt(i));
		}
		Collections.sort(children, new NodeTreeComparator());
		parent.removeAllChildren();
		for (MutableTreeNode node : children) {
			parent.add(node);
		}
	}

	/**
	 * @return The files in the model.
	 */
	public List<FileStatus> getFilesStatus() {
    return filesStatus;
  }
}
