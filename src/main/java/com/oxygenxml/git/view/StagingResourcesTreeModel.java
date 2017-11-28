package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.TreeFormatter;
import com.oxygenxml.git.view.event.ChangeEvent;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.FileState;
import com.oxygenxml.git.view.event.Subject;

/**
 * Custom tree model
 * 
 * @author Beniamin Savu
 *
 */
public class StagingResourcesTreeModel extends DefaultTreeModel implements Subject<ChangeEvent>, Observer<ChangeEvent> {

	/**
	 * The files in the model
	 */
	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	/**
	 * <code>true</code> if this model presents un-staged resources that will be
	 * staged. <code>false</code> if this model presents staged resources that
	 * will be unstaged.
	 */
	private boolean forStaging;

	/**
	 * Observer to delegate the event
	 */
	private Observer<ChangeEvent> observer;

	public StagingResourcesTreeModel(TreeNode root, boolean forStaging, List<FileStatus> filesStatus) {
		super(root);
		this.forStaging = forStaging;
		this.filesStatus = filesStatus;
	}

	public void stateChanged(ChangeEvent changeEvent) {
		List<FileStatus> fileToBeUpdated = changeEvent.getFileToBeUpdated();
		if (changeEvent.getNewState() == FileState.STAGED) {
			if (forStaging) {
				insertNodes(fileToBeUpdated);
			} else {
				deleteNodes(fileToBeUpdated);
			}
		} else if (changeEvent.getNewState() == FileState.UNSTAGED) {
			if (forStaging) {
				deleteNodes(fileToBeUpdated);
			} else {
				insertNodes(fileToBeUpdated);
			}
		} else if (changeEvent.getNewState() == FileState.COMMITED) {
			if (forStaging) {
				deleteNodes(filesStatus);
				filesStatus.clear();
			}
		} else if (changeEvent.getNewState() == FileState.DISCARD) {
			deleteNodes(fileToBeUpdated);
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
			MyNode node = TreeFormatter.getTreeNodeFromString(this, fileStatus.getFileLocation());
			while (node.getParent() != null) {
				MyNode parentNode = (MyNode) node.getParent();
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

	public void addObserver(Observer<ChangeEvent> observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	public void removeObserver(Observer<ChangeEvent> obj) {
		observer = null;
	}

	/**
	 * Change the given files stage state from unstaged to staged or from staged
	 * to unstaged
	 * 
	 * @param selectedFiles
	 *          - the files to change their stage state
	 */
	public void switchFilesStageState(List<String> selectedFiles) {

		List<FileStatus> filesToRemove = new ArrayList<FileStatus>();
		for (String string : selectedFiles) {
			for (FileStatus fileStatus : filesStatus) {
				if (fileStatus.getFileLocation().contains(string) && fileStatus.getChangeType() != GitChangeType.CONFLICT) {
					filesToRemove.add(new FileStatus(fileStatus));
				}
			}
		}

		FileState newSTate = FileState.UNSTAGED;
		FileState oldState = FileState.STAGED;
		if (!forStaging) {
			newSTate = FileState.STAGED;
			oldState = FileState.UNSTAGED;
		}

		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, filesToRemove);
		notifyObservers(changeEvent);
	}

	/**
	 * Delegate the given event to the observer
	 * 
	 * @param changeEvent
	 *          - the event to delegate
	 */
	private void notifyObservers(ChangeEvent changeEvent) {
		observer.stateChanged(changeEvent);
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
		MyNode root = (MyNode) getRoot();
		Enumeration e = root.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			MyNode node = (MyNode) e.nextElement();
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
	private void sort(MyNode parent) {
		int n = parent.getChildCount();
		List<MyNode> children = new ArrayList<MyNode>(n);
		for (int i = 0; i < n; i++) {
			children.add((MyNode) parent.getChildAt(i));
		}
		Collections.sort(children, new NodeTreeComparator());
		parent.removeAllChildren();
		for (MutableTreeNode node : children) {
			parent.add(node);
		}
	}

}
