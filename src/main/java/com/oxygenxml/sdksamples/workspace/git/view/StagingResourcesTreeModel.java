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
import com.oxygenxml.sdksamples.workspace.git.view.event.StageState;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

/**
 *
 * TODO How about we call this Staging(Resources)TreeModel? That's pretty much
 * what it does... switches the staging state.
 *
 */
public class StagingResourcesTreeModel extends DefaultTreeModel implements Subject, Observer {

	private List<FileStatus> filesStatus = new ArrayList<FileStatus>();

	/**
	 * <code>true</code> if this model presents un-staged resources that will be
	 * staged. <code>false</code> if this model presents staged resources that
	 * will be unstaged.
	 */
	private boolean forStaging;
	private Observer observer;

	public StagingResourcesTreeModel(TreeNode root, boolean forStaging, List<FileStatus> filesStatus) {
		super(root);
		this.forStaging = forStaging;
		this.filesStatus = filesStatus;
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
		} else if (changeEvent.getNewState() == StageState.UNSTAGED) {
			if (forStaging) {
				deleteNodes(fileToBeUpdated);
			} else {
				insertNodes(fileToBeUpdated);
			}
		} else {
			if (forStaging) {
				deleteNodes(filesStatus);
			}
		}

		fireTreeStructureChanged(this, null, null, null);

	}

	private void insertNodes(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			TreeFormatter.buildTreeFromString(this, fileStatus.getFileLocation());
		}
		filesStatus.addAll(fileToBeUpdated);
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
		filesStatus.removeAll(fileToBeUpdated);
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

	public void switchFilesStageState(List<String> selectedFiles) {

		List<FileStatus> filesToRemove = new ArrayList<FileStatus>();
		for (String string : selectedFiles) {
			for (FileStatus fileStatus : filesStatus) {
				if (fileStatus.getFileLocation().contains(string)) {
					filesToRemove.add(new FileStatus(fileStatus));
				}
			}
		}

		StageState newSTate = StageState.UNSTAGED;
		StageState oldState = StageState.STAGED;
		if (!forStaging) {
			newSTate = StageState.STAGED;
			oldState = StageState.UNSTAGED;
		}

		ChangeEvent changeEvent = new ChangeEvent(newSTate, oldState, filesToRemove, this);
		notifyObservers(changeEvent);
	}

	private void notifyObservers(ChangeEvent changeEvent) {
		observer.stateChanged(changeEvent);
	}

}
