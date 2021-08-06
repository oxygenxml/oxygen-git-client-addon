package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;


/**
 * Tests the methods for stash action.
 * 
 * @author Alex_Smarandache
 *
 */
public class GitAccessStashTest {

  private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/GItAccessStagedFilesTest";
  private GitAccess gitAccess;

  @Before
  public void init() throws IllegalStateException, GitAPIException {
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
    File file = new File(LOCAL_TEST_REPOSITPRY + "/test.txt");
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
    gitAccess.commit("file test added");
  }

  @Test
  public void testCreateMethod() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    assertTrue(gitAccess.isStashEmpty());
    assertNotNull(gitAccess.createStash());
    assertFalse(gitAccess.isStashEmpty());
  }
  
  @Test
  public void testApplyMethod() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITPRY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    assertTrue(gitAccess.isStashEmpty());
    RevCommit ref = gitAccess.createStash();
    assertFalse(gitAccess.isStashEmpty());
    boolean noCommitFound = false;
    try {
      gitAccess.stashApply("No exists.");
    } catch(Exception e) {
      noCommitFound = true;
    }
    assertTrue(noCommitFound);
    assertEquals(gitAccess.stashApply(ref.getName()).getName(), ref.getName());
  }
  
  @After
  public void freeResources() {
    gitAccess.closeRepo();
    File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
    try {
      FileUtils.deleteDirectory(dirToDelete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
 
}
