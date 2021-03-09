package com.oxygenxml.git.service;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.submodule.SubmoduleStatus;

public class SubmoduleAccess {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(SubmoduleAccess.class);
  /**
   * Git repository API.
   */
  private Supplier<Git> git;
  /**
   * Private constructor.
   * 
   * @param git Git repository API.
   */
  private SubmoduleAccess(Supplier<Git> git) {
    this.git = git;
    
  }
  
  static SubmoduleAccess wrap(Supplier<Git> git) {
    return new SubmoduleAccess(git);
  }

  /**
   * Returns for the given submodule the SHA-1 commit id for the Index if the
   * given index boolean is <code>true</code> or the SHA-1 commit id for the HEAD
   * if the given index boolean is <code>false</code>
   * 
   * @param submodulePath - the path to get the submodule
   * @param index         - boolean to determine what commit id to return
   * @return the SHA-1 id
   */
  public ObjectId submoduleCompare(String submodulePath, boolean index) {
    ObjectId objID = null;
    try {
      SubmoduleStatus submoduleStatus = git.get().submoduleStatus().addPath(submodulePath).call().get(submodulePath);
      if (submoduleStatus != null) {
        objID = index ? submoduleStatus.getIndexId() : submoduleStatus.getHeadId();
      }
    } catch (GitAPIException e) {
      logger.error(e, e);
    }
    return objID;
  }
  

  /**
   * Returns a list with all the submodules name for the current repository
   * 
   * @return a list containing all the submodules
   */
  public Set<String> getSubmodules() {
    try {
      if (git.get() != null) {
        return git.get().submoduleStatus().call().keySet();
      }
    } catch (GitAPIException e) {
      logger.error(e, e);
    }
    return new HashSet<>();
  }
  
  /**
   * Return the submodule head commit to the previously one
   * 
   * @throws GitAPIException when an error occurs while trying to discard the
   *                         submodule.
   */
  public void discardSubmodule() throws GitAPIException {
    git.get().submoduleSync().call();
    git.get().submoduleUpdate().setStrategy(MergeStrategy.RECURSIVE).call();
  }
}
