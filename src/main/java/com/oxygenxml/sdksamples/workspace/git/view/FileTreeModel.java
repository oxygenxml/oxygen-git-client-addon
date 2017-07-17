package com.oxygenxml.sdksamples.workspace.git.view;

import java.util.List;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import com.oxygenxml.sdksamples.workspace.git.service.entities.FileStatus;
import com.oxygenxml.sdksamples.workspace.git.utils.TreeFormatter;
import com.oxygenxml.sdksamples.workspace.git.view.event.ChangeEvent;
import com.oxygenxml.sdksamples.workspace.git.view.event.Observer;
import com.oxygenxml.sdksamples.workspace.git.view.event.Subject;

public class FileTreeModel extends DefaultTreeModel implements Subject, Observer {

	public FileTreeModel(TreeNode root) {
		super(root);
	}

	@Override
	public void stateChanged(ChangeEvent changeEvent) {
		// TODO Put the new files.
		List<FileStatus> fileToBeUpdated = changeEvent.getFileToBeUpdated();
		for (FileStatus fileStatus : fileToBeUpdated) {
			TreeFormatter.buildTreeFromString(this, fileStatus.getFileLocation());
		}
		
		// TODO Fire with the changed paths
		fireTreeStructureChanged(this, null, null, null);
		
	}

	@Override
	public void addObserver(Observer obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeObserver(Observer obj) {
		// TODO Auto-generated method stub
		
	}
}
