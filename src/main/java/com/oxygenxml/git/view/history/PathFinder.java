package com.oxygenxml.git.view.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.oxygenxml.git.service.RevCommitUtil;

/**
 * Finds the path of a file at certain times.
 * 
 * @author alex_smarandache
 *
 */
public class PathFinder {
	
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = Logger.getLogger(PathFinder.class);
	
	/**
     * We have the following map: 
     * The keys are the time of the next commit in which the file whose history 
     *   is shown is involved(or even the commit if this is the last one). 
     * The values ​​have the path of the file that was before it changed.
     */
    private Map<Integer, String> fileAllPaths;
    
	
	/**
	 * Constructor.
	 * 
	 * @param repository      The current repository.
	 * @param fileActualPath  Actual file path.
	 */
	public PathFinder(Repository repository, String fileActualPath) {
		try {
			fileAllPaths = RevCommitUtil.getCurrentFileOldPaths(fileActualPath, repository);
   		} catch (GitAPIException | IOException e) {
   			LOGGER.error(e, e);
   			fileActualPath = null;
   		}
	}
	
	
	/**
     * Get the file path on the given commit.
     * 
     * @param commit The commit.
     * 
     * @return file path or <code>null</code>.
     */
    public String getFilePathOnCommit(RevCommit commit) {
  	  String searchedPath = null;
  	  if(fileAllPaths != null) {
  		  List<Integer> commitsTime = new ArrayList<>(fileAllPaths.keySet());
  		  if(commitsTime.size() == 1) {
  			  searchedPath = fileAllPaths.get(commitsTime.get(0));
  		  } else {
  			  int current = commit.getCommitTime();
  			  for(int i = 0; i < commitsTime.size(); i++) {
  				  int after = commitsTime.get(i);	  
  				  if(current < after) {
  					  searchedPath = fileAllPaths.get(commitsTime.get(i));
  					  break;
  				  }
  			  }
  			  
  			  if(searchedPath == null) {
  				  searchedPath = fileAllPaths.get(commitsTime.get(commitsTime.size() - 1));
  			  }
  		  }
  	  }
  	  
  	  return searchedPath;
    }

}
