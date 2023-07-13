package com.oxygenxml.git.service.lfs;

import org.eclipse.jgit.lfs.BuiltinLFS;
import org.eclipse.jgit.util.LfsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

/**
 * This class 
 * 
 * @author alex_smarandache
 *
 */
public class LFSSupport {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LFSSupport.class);

  /**
   * Hidden constructor.
   */
  private LFSSupport() {
  	throw new UnsupportedOperationException("Instantiation of this class is not allowed!");
  }
  
  /**
   * When is installed, the LFS support will be enabled for each repository.
   * 
   * @param gitCtrl The Git controller.
   */
  public static void install(final GitController gitCtrl) {
    gitCtrl.addGitListener(new GitEventAdapter() {
      
      @Override
      public void operationSuccessfullyEnded(GitEventInfo info) {
        if(info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
          try {
            LfsFactory.getInstance().getInstallCommand().setRepository(gitCtrl.getGitAccess().getRepository()).call();
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          }
          enableLFS();
        }
      }
    });
  }
  
  /**
   * This method activate the LFS support.
   */
  public static void enableLFS() {
    BuiltinLFS.register();
  }
  
}
