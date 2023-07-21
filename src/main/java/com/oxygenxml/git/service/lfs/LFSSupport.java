package com.oxygenxml.git.service.lfs;

import org.eclipse.jgit.lfs.BuiltinLFS;

import com.oxygenxml.git.view.event.GitController;

/**
 * Class related to the large-files-system in Git.
 * 
 * @author alex_smarandache
 */
public class LFSSupport {

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
  	BuiltinLFS.register();
  }
  
}
