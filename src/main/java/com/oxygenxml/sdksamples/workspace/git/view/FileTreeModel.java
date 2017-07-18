package com.oxygenxml.sdksamples.workspace.git.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.utils.TreeFormatter;
import com.oxygenxml.sdksamples.workspace.git.view.event.ChangeEvent;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

public class FileTreeModel extends DefaultTreeModel implements Subject, Observer {

	List<FileStatus> fileStatus = new ArrayList<FileStatus>();

	private boolean forStaging;
	private Observer observer;

	public FileTreeModel(TreeNode root, boolean forStaging) {
		super(root);
		this.forStaging = forStaging;
	}

	@Override
	public void stateChanged(ChangeEvent changeEvent) {
		List<FileStatus> fileToBeUpdated = changeEvent.getFileToBeUpdated();
		if (changeEvent.getNewState() == StageState.STAGED) {
			if (forStaging) {
				insertNodes(fileToBeUpdated);
			} else {
				deleteNodes(fileToBeUpdated);
			}
		} else {
			if (forStaging) {
				deleteNodes(fileToBeUpdated);
			} else {
				insertNodes(fileToBeUpdated);
			}
		}

		fireTreeStructureChanged(this, null, null, null);

	}

	private void insertNodes(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			TreeFormatter.buildTreeFromString(this, fileStatus.getFileLocation());
		}
	}

	private void deleteNodes(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			DefaultMutableTreeNode node = TreeFormatter.getTreeNodeFromString(this, fileStatus.getFileLocation());
			while (node.getParent() != null) {
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
				if (node.getSiblingCount() != 1) {
					parentNode.remove(node);
					break;
				} else {
					parentNode.remove(node);
				}
				node = parentNode;
			}
		}
	}

	@Override
	public void addObserver(Observer observer) {
		if (observer == null)
			throw new NullPointerException("Null Observer");

		this.observer = observer;
	}

	@Override
	public void removeObserver(Observer obj) {
		observer = null;
	}

	public void removeUnstageFiles(List<FileStatus> selectedFiles) {

		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, selectedFiles, this);
		notifyObservers(changeEvent);
	}

	private void notifyObservers(ChangeEvent changeEvent) {
		observer.stateChanged(changeEvent);
	}
}
