package com.oxygenxml.git.service;

import java.io.File;

import org.eclipse.jgit.lib.Repository;

import junit.framework.TestCase;

/**
 * Test case for {@link GitAccess} DefaultBranchName.
 */
public class GitAccess2Test extends TestCase {

  /**
   * <p>
   * <b>Description:</b> Use "main" instead of "master" as the name of the default
   * branch
   * </p>
   * <p>
   * <b>Bug ID:</b> EXM-47940
   * </p>
   * 
   * @author gabriel_nedianu
   * 
   * @throws Exception
   */
  public void testDefaultBranchName() throws Exception {
    File newRepoDirectory = new File("src/test/resources/newRepo");

    try {
      newRepoDirectory.mkdir();

      GitAccess.getInstance().createNewRepository(newRepoDirectory.getAbsolutePath());
      GitAccess.getInstance().setRepositorySynchronously(newRepoDirectory.getAbsolutePath());
      Repository newRepository = GitAccess.getInstance().getRepository();
      String branchName = newRepository.getBranch();

      assertEquals(GitAccess.DEFAULT_BRANCH_NAME, branchName);

    } finally {
      org.apache.commons.io.FileUtils.deleteDirectory(newRepoDirectory);
    }

  }

}
