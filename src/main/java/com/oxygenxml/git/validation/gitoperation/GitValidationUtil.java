package com.oxygenxml.git.validation.gitoperation;

import java.util.List;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.FileStatusUtil;

/**
 * Contains methods usefully to validate before git operations.
 * 
 * @author alex_smarandache
 *
 */
public class GitValidationUtil {
  
  /**
   * Hidden constructor.
   */
  private GitValidationUtil() {
    // not needed
  }

  /**
   * @param includeStagedFiles <code>true</code> if the staged files should be included.
   * 
   * @return <code>true</code> if there are uncommited changes without ".xpr" files.
   */
  public static boolean hasUncommitedChanges(final boolean includeStagedFiles) {
    final List<FileStatus> unstagedFiles = GitAccess.getInstance().getUnstagedFiles();
    boolean toReturn = unstagedFiles.stream().anyMatch(f -> !FileStatusUtil.isUnreachableFile(f));
    if(includeStagedFiles && !toReturn) {
      final List<FileStatus> stagedFiles = GitAccess.getInstance().getStagedFiles();
      toReturn = stagedFiles.stream().anyMatch(f -> !FileStatusUtil.isUnreachableFile(f));
    }
    return toReturn;
  }
  
}
