package com.oxygenxml.git.view.staging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.GitTreeNode;
import com.oxygenxml.git.view.event.FileGitEventInfo;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.util.TreeUtil;

/**
 * Tree model for staged or unstaged resources.
 * 
 * @author Beniamin Savu
 */
public class StagingResourcesTreeModel extends DefaultTreeModel {
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(StagingResourcesTreeModel.class);

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
	 * Git controller.
	 */
  private GitControllerBase gitController;

	/**
	 * Constructor.
	 * 
	 * @param controller  Git controller.
	 * @param root        Root folder's name.
	 * @param inIndex     <code>true</code> if this model presents the resources inside the index.
   *                    <code>false</code> if it presents the modified resources that can be put in the index.
	 * @param filesStatus The files statuses in the model.
	 */
	public StagingResourcesTreeModel(GitControllerBase controller, String root, boolean inIndex, List<FileStatus> filesStatus) {
		super(new GitTreeNode(root != null ? root : ""));
    this.gitController = controller;
		this.inIndex = inIndex;
    
    setFilesStatus(filesStatus);
	}

	/**
	 * File states changed.
	 * 
	 * @param eventInfo Event information.
	 */
	public void fileStatesChanged(GitEventInfo eventInfo) {
	  if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Tree model for index: {} event {}", inIndex, eventInfo);
	  }

	  GitAccess gitAccess = GitAccess.getInstance();
	  switch (eventInfo.getGitOperation()) {
	    case STAGE:
	      if (inIndex) {
	        insertNodes(gitAccess.getStagedFile(((FileGitEventInfo) eventInfo).getAffectedFilePaths()));
	      } else {
	        deleteNodes(((FileGitEventInfo) eventInfo).getAffectedFileStatuses());
	      }
	      break;
	    case UNSTAGE:
	      if (inIndex) {
	        deleteNodes(((FileGitEventInfo) eventInfo).getAffectedFileStatuses());
	      } else {
	        // Things were taken out of the index / "staged" area. 
	        // The same resource might be present in the Unstaged and Staged. Remove old states.
	        deleteNodes(((FileGitEventInfo) eventInfo).getAffectedFileStatuses());
	        insertNodes(gitAccess.getUnstagedFiles(((FileGitEventInfo) eventInfo).getAffectedFilePaths()));
	      }
	      break;
	    case COMMIT:
	      if (inIndex) {
	        clearModel();
	      }
	      break;
	    case DISCARD:
	      deleteNodes(((FileGitEventInfo) eventInfo).getAffectedFileStatuses());
	      break;
	    case MERGE_RESTART:
	      clearModel();
	      List<FileStatus> fileStatuses = inIndex ? gitAccess.getStagedFiles() 
	          : gitAccess.getUnstagedFiles();
	      insertNodes(fileStatuses);
	      break;
	    case ABORT_REBASE:
	    case CONTINUE_REBASE:
	      clearModel();
	      break;
	    case ABORT_MERGE:
	      deleteNodes(((FileGitEventInfo) eventInfo).getAffectedFileStatuses());
	      break;
	    default:
	      // Nothing
	      break;
	  }

	  fireTreeStructureChanged(this, null, null, null);
	}

	/**
	 * Clears all the nodes in the model and leaves an empty root.
	 */
  private void clearModel() {
    filesStatuses.clear();
    // Rebuild the tree
    GitTreeNode root = (GitTreeNode) getRoot();
    root.removeAllChildren();
  }

	/**
	 * Insert nodes to the tree based on the given files
	 * 
	 * @param fileToBeUpdated
	 *          - the files on which the nodes will be created
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
	        if (includePath(path, fileStatus.getFileLocation())) {
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
   * Change the files stage state from unstaged to staged or from staged to unstaged.
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
    
    if (inIndex) {
      gitController.asyncReset(filesToBeUpdated);
    } else {
      gitController.asyncAddToIndex(filesToBeUpdated);
    }
  }
  
  /**
   * @param path            A file path.
   * @param candidatePath   A candidate to be child or even the path.
   * 
   * @return <code>true</code> if the candidate path is equals or a child of the given path.
   */
  private boolean includePath(final String path, final String candidatePath) {
    boolean toReturn = false;
    final int pathLength = path.length();
    final int candidatePathLength = candidatePath.length();
    if(candidatePathLength == pathLength) {
      toReturn = path.equals(candidatePath);
    } else if(candidatePathLength > pathLength) {
      toReturn = candidatePath.startsWith(path + "/");
    }
    
    return toReturn;
  }
  
}
