package com.oxygenxml.git.view.event;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

/**
 * 
 * Executes Git commands. A higher level wrapper over the GitAccess.
 *  
 * @author Beniamin Savu
 */
public class StageController {
  /**
   * Logger for logging.
   */
  private static Logger logger = Logger.getLogger(GitAccess.class);

	/**
	 * the git API
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * Executes the given action on the given files.
	 * 
	 * @param filesStatus The files to be processed. 
	 * @param action
	 */
	public void doGitCommand(List<FileStatus> filesStatus, GitCommand action) {
	  if (logger.isDebugEnabled()) {
	    logger.debug("Do action " + action + " on " + filesStatus);
	  }
	  
	  if (action == GitCommand.STAGE) {
	    gitAccess.addAll(filesStatus);
	  } else if (action == GitCommand.UNSTAGE) {
	    // Remove from index.
	    gitAccess.resetAll(filesStatus);
	  } else if (action == GitCommand.DISCARD || action == GitCommand.RESOLVE_USING_MINE) {
      gitAccess.resetAll(filesStatus);
      List<String> paths = new LinkedList<>();
      for (FileStatus file : filesStatus) {
        if (file.getChangeType() != GitChangeType.SUBMODULE) {
          paths.add(file.getFileLocation());
        }
      }
      
      gitAccess.restoreLastCommitFile(paths);
      
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
	}
}
