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
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.StashListCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.stash.StashApplyStatus;


/**
 * <p><b>Description:</b> Tests the methods for stash action.</p>
 * <p><b>Bug ID:</b> EXM-45983</p>
 *
 * @author Alex_Smarandache
 *
 * @throws Exception
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
   * Helper method returning whether the stash is empty or not.
   *
   * @return <code>true</code> if the git stash is empty
   *
   * @throws GitAPIException
   */
  protected boolean isStashEmpty() throws GitAPIException {
    StashListCommand stashList = gitAccess.getGit().stashList();
    Collection<RevCommit> stashedRefsCollection = stashList.call();
    return stashedRefsCollection.isEmpty();
  }


  /**
   * <p><b>Description:</b> tests the com.oxygenxml.git.service.GitAccess.createStash() API.</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
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
    assertTrue(isStashEmpty());
    assertEquals("stash_description", gitAccess.createStash(false,"stash_description").getFullMessage());
    assertFalse(isStashEmpty());
    assertEquals(1, gitAccess.listStashes().size());
    gitAccess.dropStash(0);
    assertTrue(isStashEmpty());
  }


  /**
   * <p><b>Description:</b> tests the com.oxygenxml.git.service.GitAccess.stashApply(String stashRef) API.</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
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

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false);
    assertFalse(isStashEmpty());

    boolean noCommitFound = false;
    try {
      gitAccess.applyStash("No exists.");
    } catch (Exception e) {
      noCommitFound = true;
    }
    assertTrue(noCommitFound);

   
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("", content);

    gitAccess.applyStash(commitStash.getName());
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
  }


  /**
   * <p><b>Description:</b> tests the situation in which we want to apply a stash and we have uncommitted changes that do not cause conflicts.</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
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
    
    assertTrue(isStashEmpty());
    RevCommit ref = gitAccess.createStash(false);
    assertFalse(isStashEmpty());
    
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
   * <p><b>Description:</b> tests the pop method</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testStashPop() throws Exception {
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false);
    assertFalse(isStashEmpty());
   
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("", content);

    assertEquals(StashApplyStatus.APPLIED_SUCCESSFULLY, gitAccess.popStash(commitStash.getName()));
    
    assertEquals(0, gitAccess.listStashes().size());
    
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("modify", content);
  }  
  

  /**
   * <p><b>Description:</b> tests the situation in which we want to apply a stash and we have committed changes that do cause conflicts.</p>
   * <p><b>Bug ID:</b>EXM-45983</p>
   *
   * @author Alex_Smarandache
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
    
    assertTrue(isStashEmpty());
    RevCommit ref = gitAccess.createStash(false);
    assertFalse(isStashEmpty());
    try {
      PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
      out.println("modify");
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("file test modified");
    assertNull(gitAccess.createStash(false));
    
    gitAccess.applyStash(ref.getName());
    
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertTrue(content.contains("<<<<<<<") || content.contains("=======") || content.contains(">>>>>>>"));
  }
  
  
  /**
   * <p><b>Description:</b> tests the drop all method</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  @Test
  public void testStashDropAll() throws Exception {
    
    String[] texts = {"test1", "test2", "test3", "test4", 
        "test5", "test6", "test7", "test8", "test9", "test10"};
    
    for(String fileAddedContent: texts) {
      try {
        PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt");
        out.println(fileAddedContent);
        out.close();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      gitAccess.addAll(gitAccess.getUnstagedFiles());

      RevCommit commitStash = gitAccess.createStash(true);
      assertNotNull(commitStash);
    }
    
    assertEquals(10, gitAccess.listStashes().size());
    
    gitAccess.dropAllStashes();
    
    assertTrue(isStashEmpty());
    
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
