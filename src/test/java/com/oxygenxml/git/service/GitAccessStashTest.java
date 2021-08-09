package com.oxygenxml.git.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

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

  /**
   * The local repository.
   */
  private final static String LOCAL_TEST_REPOSITORY = "target/test-resources/GItAccessStagedFilesTest";
  
  /**
   * The GitAccess instance.
   */
  private GitAccess gitAccess;

  
  /**
   * Initialise the git, repository and first local commit.
   * 
   * @throws IllegalStateException
   * @throws GitAPIException
   */
  @Before
  public void init() throws IllegalStateException, GitAPIException {
    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITORY);
    File file = new File(LOCAL_TEST_REPOSITORY + "/test.txt");
    File file2 = new File(LOCAL_TEST_REPOSITORY + "/test2.txt");
    try {
      file.createNewFile();
      file2.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.ADD, file.getName()));
    gitAccess.add(new FileStatus(GitChangeType.ADD, file2.getName()));
    gitAccess.commit("file test added");
  }

  
  /**
   * Tests the com.oxygenxml.git.service.GitAccess.createStash() API.
   * 
   * @throws Exception
   */
  @Test
  public void testCreateMethod() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    assertTrue(gitAccess.isStashEmpty());
    assertNotNull(gitAccess.createStash());
    assertFalse(gitAccess.isStashEmpty());
    gitAccess.stashDrop(0);
    assertTrue(gitAccess.isStashEmpty());
  }
  
  
  /**
   * Tests the com.oxygenxml.git.service.GitAccess.stashApply(String stashRef) API.
   * 
   * @throws Exception
   */
  @Test
  public void testApplyMethod() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    assertTrue(gitAccess.isStashEmpty());
    assertFalse(gitAccess.isStashEmpty());
    
    boolean noCommitFound = false;
    try {
      gitAccess.applyStash("No exists.");
    } catch(Exception e) {
      noCommitFound = true;
    }
    assertTrue(noCommitFound);
    
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("", content);
    
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
  }
  
  
  /**
   * Tests the situation in which we want to apply a stash and we have uncommitted changes that do not cause conflicts.
   * 
   * @throws Exception
   */
  @Test
  public void testStashWithUncommittedChangesWithoutConflicts() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("test");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    assertTrue(gitAccess.isStashEmpty());
    RevCommit ref = gitAccess.createStash();
    assertFalse(gitAccess.isStashEmpty());
    
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test2.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    gitAccess.applyStash(ref.getName());
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("test", content);
    
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test2.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
  }
  
  
  /**
   * Tests the situation in which we want to apply a stash and we have committed changes that do cause conflicts.
   * 
   * @throws Exception
   */
  @Test
  public void testStashWithCommittedChangesWithConflicts() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("test");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    assertTrue(gitAccess.isStashEmpty());
    RevCommit ref = gitAccess.createStash();
    assertFalse(gitAccess.isStashEmpty());
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("file test modified");
    assertNull(gitAccess.createStash());
    
    gitAccess.applyStash(ref.getName());
    
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertTrue(content.contains("<<<<<<<") || content.contains("=======") || content.contains(">>>>>>>"));
  }
  
  
  /**
   * Used to free up test resources.
   */
  @After
  public void freeResources() {
    gitAccess.closeRepo();
    File dirToDelete = new File(LOCAL_TEST_REPOSITORY);
    try {
      FileUtils.deleteDirectory(dirToDelete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
 
}
