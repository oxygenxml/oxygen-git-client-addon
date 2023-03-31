package com.oxygenxml.git.service.lfs;

import org.eclipse.jgit.lfs.BuiltinLFS;
import org.eclipse.jgit.util.LfsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;

public class LFSHelper {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LFSHelper.class);
  
  private static final LfsFactory LFS_DISABLED_INSTANCE;
  static {
    LFS_DISABLED_INSTANCE = new LFSDefault();
  }

  private LFSHelper() {
    
  }
  
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
  
  public static void enableLFS() {
    BuiltinLFS.register();
  }
  
  public static void disableLFS() {
    LfsFactory.setInstance(LFS_DISABLED_INSTANCE);
  }
  
  
  private static final class LFSDefault extends LfsFactory {
    
  }
}
