package com.oxygenxml.git.service;

import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
  /**
   * Logger for logging.
   */
  private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);
  
  /**
   * Logs submodules.
   * 
   * @throws GitAPIException
   */
  public static void logSubmodule() throws GitAPIException {

    if (logger.isDebugEnabled()) {
      logger.debug("Main repo branch {}", GitAccess.getInstance().getBranchInfo().getBranchName());
      Map<String, SubmoduleStatus> call = GitAccess.getInstance().getGit().submoduleStatus().call();
      Set<String> keySet = call.keySet();
      for (String modID : keySet) {
        logger.debug("  SUBMODULE " + modID);
        logger.debug("  -path-" + call.get(modID).getPath());
        logger.debug("  -head-" + call.get(modID).getHeadId());
        logger.debug("  -index-" + call.get(modID).getIndexId());
        logger.debug("  -type-" + call.get(modID).getType());
      }
    }
  }
}
