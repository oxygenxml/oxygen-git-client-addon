package com.oxygenxml.git.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.StashListCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.mockito.Mockito;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.dialog.MessagePresenterProvider;
import com.oxygenxml.git.view.dialog.internal.DialogType;
import com.oxygenxml.git.view.dialog.internal.MessageDialog;
import com.oxygenxml.git.view.dialog.internal.MessageDialogBuilder;
import com.oxygenxml.git.view.stash.StashApplyStatus;

import junit.framework.TestCase;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;


/**
 * <p><b>Description:</b> Tests the methods for stash action.</p>
 * <p><b>Bug ID:</b> EXM-45983</p>
 *
 * @author Alex_Smarandache
 *
 * @throws Exception
 */
public class GitAccessStashTest extends TestCase {

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
  @Override
  protected void setUp() throws IllegalStateException, GitAPIException {
    StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWSMock);
    
    WSOptionsStorage wsOptions = new WSOptionsStorageTestAdapter();
    Mockito.when(pluginWSMock.getOptionsStorage()).thenReturn(wsOptions);
    
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
   
    MessagePresenterProvider.setBuilder(new MessageDialogBuilder(
        "test_stash", DialogType.WARNING) {
      @Override
      public MessageDialog buildAndShow() {
        return Mockito.mock(MessageDialog.class);
      }
    });
  }
  
  /**
   * Used to free up test resources.
   * @throws Exception 
   */
  @Override
  protected void tearDown() throws Exception {
    gitAccess.closeRepo();
    File dirToDelete = new File(LOCAL_TEST_REPOSITORY);
    FileUtil.deleteRecursivelly(dirToDelete);
    PluginWorkspaceProvider.setPluginWorkspace(null);
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
  public void testCreateMethod() throws Exception { 
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
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
  public void testApplyMethod() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    gitAccess.addAll(gitAccess.getUnstagedFiles());

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false, null);
    assertFalse(isStashEmpty());

    boolean noCommitFound = false;
    try {
      gitAccess.applyStash("No exists.");
    } catch (Throwable e) {
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
   * <p><b>Description:</b> Tests if the stash restore untracked files on a conflict</p>
   * <p><b>Bug ID:</b> EXM-52962</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  public void testApplyStashInConflictWithUntrackedFiles() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    File file = new File(LOCAL_TEST_REPOSITORY + "/test3.txt");
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.UNTRACKED, file.getName()));
    assertTrue(file.exists());

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false, null);
    assertFalse(isStashEmpty());
    assertFalse(file.exists());
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("notify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    gitAccess.commit("Test");
    
    boolean noCommitFound = false;
    try {
      gitAccess.applyStash("No exists.");
    } catch (Throwable e) {
      noCommitFound = true;
    }
    assertTrue(noCommitFound);
   
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("notify", content);

    gitAccess.applyStash(commitStash.getName());
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    content = content.replaceAll("\r", "");
    assertEquals("<<<<<<< HEAD\n"
    		+ "notify\n"
    		+ "=======\n"
    		+ "modify\n"
    		+ ">>>>>>> stash", content);
    assertTrue(file.exists());
  }
  
  /**
   * <p><b>Description:</b> Tests if the stash restore untracked files on a conflict</p>
   * <p><b>Bug ID:</b> EXM-52962</p>
   *
   * @author alex_smarandache
   *
   * @throws Exception
   */
  public void testPopStashInConflictWithUntrackedFiles() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    File file = new File(LOCAL_TEST_REPOSITORY + "/test3.txt");
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.UNTRACKED, file.getName()));
    assertTrue(file.exists());

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false, null);
    assertFalse(isStashEmpty());
    assertFalse(file.exists());
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("notify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    gitAccess.commit("Test");
    
    boolean noCommitFound = false;
    try {
      gitAccess.applyStash("No exists.");
    } catch (Throwable e) {
      noCommitFound = true;
    }
    assertTrue(noCommitFound);
   
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertEquals("notify", content);

    gitAccess.popStash(commitStash.getName());
    assertFalse(isStashEmpty());
    reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    content = content.replaceAll("\r", "");
    assertEquals("<<<<<<< HEAD\n"
    		+ "notify\n"
    		+ "=======\n"
    		+ "modify\n"
    		+ ">>>>>>> stash", content);
    assertTrue(file.exists());
  }

  /**
   * <p><b>Description:</b> tests the situation in which we want to apply a stash and we have uncommitted changes that do not cause conflicts.</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testStashWithUncommittedChangesWithoutConflicts() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("test");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    assertTrue(isStashEmpty());
    RevCommit ref = gitAccess.createStash(false, null);
    assertFalse(isStashEmpty());
    
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test2.txt")) {
      out.println("modify");
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
  public void testStashPop() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());

    assertTrue(isStashEmpty());
    RevCommit commitStash = gitAccess.createStash(false, null);
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
  public void testStashWithCommittedChangesWithConflicts() throws Exception {
    
    applyStashWithConflicts();
    
    BufferedReader reader = new BufferedReader(new FileReader(LOCAL_TEST_REPOSITORY + "/test.txt"));
    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
    reader.close();
    assertTrue(content.contains("<<<<<<<") || content.contains("=======") || content.contains(">>>>>>>"));
   
  }
  
  /**
   * <p><b>Description:</b> tests if the stash conflicts are detected.</p>
   * <p><b>Bug ID:</b>EXM-49850</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testStashConflictsAreDetected() throws Exception {
    
    applyStashWithConflicts();
    
    assertTrue(RepoUtil.isUnfinishedConflictState(gitAccess.getRepository().getRepositoryState()));
   
  }

  /**
   * Apply a stash which generate a conflicting file.
   */
  private void applyStashWithConflicts() throws Exception {
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("test");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.addAll(gitAccess.getUnstagedFiles());
    
    assertTrue(isStashEmpty());
    RevCommit ref = gitAccess.createStash(false, null);
    assertFalse(isStashEmpty());
    try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
      out.println("modify");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("file test modified");
    assertNull(gitAccess.createStash(false, null));
    
    gitAccess.applyStash(ref.getName());
  }
  
  
  /**
   * <p><b>Description:</b> tests the drop all method</p>
   * <p><b>Bug ID:</b> EXM-45983</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testStashDropAll() throws Exception {
    
    String[] texts = {"test1", "test2", "test3", "test4", 
        "test5", "test6", "test7", "test8", "test9", "test10"};
    
    for(String fileAddedContent: texts) {
      try (PrintWriter out = new PrintWriter(LOCAL_TEST_REPOSITORY + "/test.txt")) {
        out.println(fileAddedContent);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      gitAccess.addAll(gitAccess.getUnstagedFiles());

      RevCommit commitStash = gitAccess.createStash(true, null);
      assertNotNull(commitStash);
    }
    
    assertEquals(10, gitAccess.listStashes().size());
    
    gitAccess.dropAllStashes();
    assertTrue(isStashEmpty());

  }  
 
}
