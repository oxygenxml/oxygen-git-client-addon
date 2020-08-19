package com.oxygenxml.git.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.TreeUtil;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitCommandState;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEvent;

/**
 * Custom tree model
 * 
 * @author Beniamin Savu
 *
 */
public class StagingResourcesTreeModel extends DefaultTreeModel {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(StagingResourcesTreeModel.class);

	/**
	 * The files in the model
	 */
	private List<FileStatus> filesStatuses = Collections.synchronizedList(new ArrayList<>());

  /**
   * <code>true</code> if this model presents the resources inside the index.
   * <code>false</code> if it presents the modified resources that can be put in the index.
   */
	private boolean inIndex;
	/**
	 * Stage controller.
	 */
  private GitController stageController;

	/**
	 * Constructor.
	 * 
	 * @param controller Staging controller.
	 * @param root Root folder's name.
	 * @param inIndex <code>true</code> if this model presents the resources inside the index.
   * <code>false</code> if it presents the modified resources that can be put in the index.
	 * @param filesStatus The files statuses in the model.
	 */
	public StagingResourcesTreeModel(GitController controller, String root, boolean inIndex, List<FileStatus> filesStatus) {
		super(new GitTreeNode(root != null ? root : ""));
    this.stageController = controller;
		this.inIndex = inIndex;
    
    setFilesStatus(filesStatus);
	}

	public void stateChanged(GitEvent changeEvent) {
	  if (logger.isDebugEnabled()) {
      logger.debug("Tree model for index: " + inIndex + " event " + changeEvent);
    }
	  
  	if (changeEvent.getGitComandState() == GitCommandState.SUCCESSFULLY_ENDED) {
  		List<FileStatus> oldStates = changeEvent.getOldAffectedFiles();
      List<FileStatus> newStates = 
          inIndex ? GitAccess.getInstance().getStagedFile(changeEvent.getAffectedFiles()) 
              : GitAccess.getInstance().getUnstagedFiles(changeEvent.getAffectedFiles());
              
      switch (changeEvent.getGitCommand()) {
        case STAGE:
          if (inIndex) {
            insertNodes(newStates);
          } else {
            deleteNodes(oldStates);
          }
          break;
        case UNSTAGE:
          if (inIndex) {
            deleteNodes(oldStates);
          } else {
             // Things were taken out of the index / "staged" area. 
            // The same resource might be present in the Unstaged and Staged. Remove old states.
            deleteNodes(oldStates);
            insertNodes(newStates);
          }
          break;
        case COMMIT:
          if (inIndex) {
            deleteNodes(filesStatuses);
            filesStatuses.clear();
          }
          break;
        case DISCARD:
          deleteNodes(oldStates);
          break;
        case MERGE_RESTART:
          filesStatuses.clear();
          List<FileStatus> fileStatuses = inIndex ? GitAccess.getInstance().getStagedFiles() 
              : GitAccess.getInstance().getUnstagedFiles();
          insertNodes(fileStatuses);
          break;
        case ABORT_REBASE:
        case CONTINUE_REBASE:
          filesStatuses.clear();
          break;
        default:
          // Nothing
          break;
      }
  
  		fireTreeStructureChanged(this, null, null, null);
  	}
	}

	/**
	 * Insert nodes to the tree based on the given files
	 * 
	 * @param fileToBeUpdated
	 *          - the files on which the nodes will be created
	 *          
	 * TODO EXM-41133 Sorin Return the added TreePaths.
	 */
	private void insertNodes(List<FileStatus> fileToBeUpdated) {

		for (FileStatus fileStatus : fileToBeUpdated) {
			TreeUtil.buildTreeFromString(this, fileStatus.getFileLocation());
		}
		filesStatuses.addAll(fileToBeUpdated);
		TreeUtil.sortGitTree(this);
	}

	/**
	 * Delete nodes from the tree based on the given files
	 * 
	 * @param fileToBeUpdated
	 *          - the files on which the nodes will be deleted
	 *          
	 * TODO EXM-41133 Sorin Return deleted TreePath.
	 */
	private void deleteNodes(List<FileStatus> fileToBeUpdated) {
		for (FileStatus fileStatus : fileToBeUpdated) {
			GitTreeNode node = TreeUtil.getTreeNodeFromString(this, fileStatus.getFileLocation());
			while (node != null && node.getParent() != null) {
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
		filesStatuses.removeAll(fileToBeUpdated);
		TreeUtil.sortGitTree(this);
	}

	/**
	 * Return the file from the given path
	 * 
	 * @param path
	 *          - the path
	 * @return the file
	 */
	public FileStatus getFileByPath(String path) {
	  FileStatus toReturn = null;
	  synchronized (filesStatuses) {
	    for (FileStatus fileStatus : filesStatuses) {
	      if (path.equals(fileStatus.getFileLocation())) {
	        toReturn = fileStatus;
	        break;
	      }
	    }
    }
		return toReturn;
	}

	/**
	 * Return the files from the given paths
	 * 
	 * @param selectedPaths
	 *          - the paths
	 * @return a list containing the files from the path
	 */
	public List<FileStatus> getFilesByPaths(List<String> selectedPaths) {
	  List<FileStatus> containingPaths = new ArrayList<>();
	  for (String path : selectedPaths) {
	    synchronized (filesStatuses) {
	      for (FileStatus fileStatus : filesStatuses) {
	        if (fileStatus.getFileLocation().startsWith(path)) {
	          containingPaths.add(new FileStatus(fileStatus));
	        }
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
	  List<FileStatus> containingPaths = new ArrayList<>();
	  for (String path : selectedPaths) {
	    synchronized (filesStatuses) {
	      for (FileStatus fileStatus : filesStatuses) {
	        if (fileStatus.getFileLocation().equals(path)) {
	          containingPaths.add(new FileStatus(fileStatus));
	        }
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
	private void setFilesStatus(List<FileStatus> filesStatus) {
	  if (filesStatus == null) {
	    filesStatus = Collections.emptyList();
	  }
	  
		deleteNodes(this.filesStatuses);
		insertNodes(filesStatus);
		
		fireTreeStructureChanged(this, null, null, null);
	}

	/**
	 * @return The files in the model.
	 */
	public List<FileStatus> getFilesStatuses() {
    return filesStatuses;
  }

  /**
   * Change the files stage state from unstaged to staged or from staged to
   * unstaged
   * 
   * @param selectedFiles
   *          - the files to change their stage state
   */
  public void switchAllFilesStageState() {
    List<FileStatus> filesToBeUpdated = new ArrayList<>();
    synchronized (filesStatuses) {
      for (FileStatus fileStatus : filesStatuses) {
        if (fileStatus.getChangeType() != GitChangeType.CONFLICT) {
          filesToBeUpdated.add(fileStatus);
        }
      }
    }
    
    GitCommand action = GitCommand.UNSTAGE;
    if (!inIndex) {
      action = GitCommand.STAGE;
    }
    
    stageController.doGitCommand(filesToBeUpdated, action);
  }
}
