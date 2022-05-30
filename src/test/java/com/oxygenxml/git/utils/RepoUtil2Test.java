package com.oxygenxml.git.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Contains automatic tests for @RepoUtil methods for extracting a repo's name, URL, and more.
 * 
 * @author Alex_Smarandache
 *
 */
public class RepoUtil2Test {

  /**
   * <p><b>Description:</b> Tests if the extracted repository name is correct.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50538</p>
   *
   * @author alex_smarandache 
   *
   * @throws Exception
   */ 
  @Test
  public void testExtractRepositoryName() throws Exception {
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project/"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project.git"));
    assertEquals("project", RepoUtil.extractRepositoryName("https://github/oxygen/project.git/"));
  }

  /**
   * <p><b>Description:</b> Tests if the extracted repository URL from git clone command is correct.</p>
   * 
   * <p><b>Bug ID:</b> EXM-50507</p>
   *
   * @author alex_smarandache 
   *
   * @throws Exception
   */ 
  @Test
  public void testExtractRepositoryURLFromCloneCommand() throws Exception {
    assertEquals("https://github/oxygen/project", RepoUtil.extractRepositoryURLFromCloneCommand(
        "git clone https://github/oxygen/project"));
    assertEquals("https://github/oxygen/project", RepoUtil.extractRepositoryURLFromCloneCommand(
        "git clone https://github/oxygen/project     "));
    assertEquals("https://github/oxygen/project", RepoUtil.extractRepositoryURLFromCloneCommand(
        "   git clone https://github/oxygen/project     "));
  }

}
