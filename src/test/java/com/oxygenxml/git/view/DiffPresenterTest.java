package com.oxygenxml.git.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.swing.JFrame;

import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.protocol.VersionIdentifier;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.service.PullResponse;
import com.oxygenxml.git.service.PullStatus;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.Observer;
import com.oxygenxml.git.view.event.PullType;
import com.oxygenxml.git.view.event.PushPullController;
import com.oxygenxml.git.view.event.PushPullEvent;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

/**
 * @author alex_jitianu
 */
public class DiffPresenterTest extends GitTestBase {
  /**
   * The left side URL in the DIFF frame.
   */
  private URL leftDiff = null;
  /**
   * The right side URL in the DIFF frame.
   */
  private URL rightDiff = null;
  /**
   * Oxygen's API received a request to open this URL.
   */
  private URL toOpen = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    try {
      StandalonePluginWorkspace mock = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      
      Mockito.when(mock.open((URL)Mockito.any())).thenAnswer(new Answer<Boolean>() {
        @Override
        public Boolean answer(InvocationOnMock invocation) throws Throwable {
          toOpen = (URL) invocation.getArguments()[0];
          return true;
        }
      });

      final JFrame frame = Mockito.mock(JFrame.class);
      
      Mockito.when(mock.openDiffFilesApplication((URL)Mockito.any(), (URL) Mockito.any())).thenAnswer(new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {

          leftDiff = (URL) invocation.getArguments()[0];
          rightDiff = (URL) invocation.getArguments()[1];

          return frame;
        }
      });
      
      ProjectController projectManager = Mockito.mock(ProjectController.class);
      Mockito.when(mock.getProjectManager()).thenReturn(projectManager);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Scenario 1:
   * - a new file. Added into the index.
   * - modify the new.
   * 
   * To test:
   * - Diff in not-staged: compares the modified version with the index version
   * - Diff in staged: compares the index version with nothing (no remote)
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testNewFileDiff() throws Exception {
    String localTestRepository = "target/test-resources/local";
    String remoteTestRepository = "target/test-resources/remote";
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo , remoteRepo);
    
    // Create a new file.
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    
    // Add it to the index / Stage it.
    GitAccess.getInstance().add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // Modify the newly created file.
    setFileContent(file, "content");
    
    FileStatus fileStatus = new FileStatus(GitChangeType.MODIFIED, "test.txt");
    GitController stageController = Mockito.mock(GitController.class);
    // Mock the translator.
    Translator translator = Mockito.mock(Translator.class);
    Mockito.when(translator.getTranslation(Mockito.anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    });
    
    // Diff the first WC local file.
    DiffPresenter.showDiff(fileStatus, stageController);
    
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);
    
    String localVersionURL = file.toURI().toURL().toString();
    assertEquals("The local file should be on the left side: " + localVersionURL, localVersionURL, leftDiff.toString());
    String indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/test.txt";
    assertEquals("The index version should be on the right, but was: " + rightDiff.toString(), indexVersionURL, rightDiff.toString());
    
    leftDiff = null;
    rightDiff = null;
    // Diff the index file.
    fileStatus = new FileStatus(GitChangeType.ADD, "test.txt");
    
    DiffPresenter.showDiff(fileStatus, stageController);
    
    // On the left we present the Index version.
    assertEquals("git://IndexOrLastCommit/test.txt", leftDiff.toString());
    // On the right we present the HEAD version.
    assertNull(rightDiff);
    assertNull(toOpen);
    
    // Assert content.
    assertEquals("", read(new URL(indexVersionURL)));
    
  }


  /**
   * Scenario 2:
   * - an existing file modified. Added into the index.
   * - the new file modified again.
   * To test:
   * - Diff in not-staged: compares the modified version with the index version
   * - Diff in staged: compares the index version with the remote one.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testExistingFileDiff() throws Exception {
    /**
     * The local repository. 
     */
    String localTestRepository = "target/test-resources/local";
    /**
     * The remote repository.
     */
    String remoteTestRepository = "target/test-resources/remote";
    
    GitAccess gitAccess = GitAccess.getInstance();
    
    Repository remoteRepo = createRepository(remoteTestRepository);
    
    // Create the local repository.
    Repository localRepo = createRepository(localTestRepository);
    
    // Bind the local repository to the remote one.
    bindLocalToRemote(localRepo, remoteRepo);
    
    // Create a new file.
    File file = new File(localTestRepository + "/test.txt");
    file.createNewFile();
    // Modify the newly created file.
    setFileContent(file, "initial content");
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First version.");
    
    // Change the file.
    setFileContent(file, "index content");
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    
    // Change it again.
    setFileContent(file, "local content");
    
    FileStatus fileStatus = new FileStatus(GitChangeType.MODIFIED, "test.txt");
    GitController stageController = Mockito.mock(GitController.class);
    // Mock the translator.
    Translator translator = Mockito.mock(Translator.class);
    Mockito.when(translator.getTranslation(Mockito.anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    });
    
    // Diff the first WC local file.
    DiffPresenter.showDiff(fileStatus, stageController);
    
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);
    
    String localVersionURL = file.toURI().toURL().toString();
    assertEquals("The local file should be on the left side, but was: " + localVersionURL, localVersionURL, leftDiff.toString());
    String indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/test.txt";
    assertEquals("The index version should be on the right, but was: " + rightDiff.toString(), indexVersionURL, rightDiff.toString());
    
    leftDiff = null;
    rightDiff = null;
    // Diff the index file.
    fileStatus = new FileStatus(GitChangeType.CHANGED, "test.txt");
    
    DiffPresenter.showDiff(fileStatus, stageController);
    
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);
    
    assertEquals("The index version should be on the left, but was: " + leftDiff.toString(), 
        indexVersionURL, leftDiff.toString());
    
    String headVersionURL = "git://" + VersionIdentifier.LAST_COMMIT  + "/test.txt";
    assertEquals("The head version should be on the right, but was: " + rightDiff.toString(), 
        headVersionURL, rightDiff.toString());
    
    // Assert content.
    assertEquals("index content", read(new URL(indexVersionURL)));
  }
  
  /**
   * <p><b>Description:</b> Checks the URL used in a rebase conflicts.</p>
   * <p><b>Bug ID:</b> EXM-45843</p>
   *
   * @author bogdan_dragici
   *
   * @throws Exception
   */
  @Test
  public void testCompareSubmodule() throws Exception {
    GitAccess gitAccess = GitAccess.getInstance();
    
    // PARENT repos
    String localTestRepositoryP = "target/test-resources/localCS";
    String remoteTestRepositoryP = "target/test-resources/remoteCS";
    Repository remoteRepoP = createRepository(remoteTestRepositoryP);
    Repository localRepoP = createRepository(localTestRepositoryP);
    bindLocalToRemote(localRepoP, remoteRepoP);
    
    // SUBMODULE repos
    String remoteTestRepositorySubModule = "target/test-resources/remoteCS-SubModule/";
    Repository remoteRepoSubModule = createRepository(remoteTestRepositorySubModule);
    // Commit (very important)
    gitAccess.commit("Commit");
    
    // Set the PARENT repo as the current one, to which we'll add the submodule
    gitAccess.setRepositorySynchronously(localTestRepositoryP);
    
    // Add SUBMODULE
    SubmoduleAddCommand addCommand = gitAccess.submoduleAdd();
    addCommand.setURI(remoteRepoSubModule.getDirectory().toURI().toString());
    addCommand.setPath("modules/submodule");
    Repository subRepo = addCommand.call();
    subRepo.close();
    
    File parentWorkDir = gitAccess.getRepository().getWorkTree();
    assertTrue( new File( parentWorkDir, "modules/submodule" ).isDirectory() );
    assertTrue( new File( parentWorkDir, ".gitmodules" ).isFile() );

    // Check the SUBMODULE
    Map<String,SubmoduleStatus> submodules = gitAccess.submoduleStatus().call();
    assertEquals(1, submodules.size());
    SubmoduleStatus status = submodules.get("modules/submodule");
    assertNotNull(status);
    assertEquals(SubmoduleStatusType.INITIALIZED, status.getType());
    
    // SHOW DIFF
    DiffPresenter.showDiff(
        // The submodule
        gitAccess.getStagedFiles().get(1),
        Mockito.mock(GitController.class));
    
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);
    
    String left = "git://CurrentSubmodule/modules/submodule.txt";
    assertEquals(left, leftDiff.toString());
    assertTrue(read(new URL(left)).startsWith("Subproject commit "));
    
    String right = "git://PreviousSubmodule/modules/submodule.txt";
    assertEquals(right, rightDiff.toString());
    assertTrue(read(new URL(right)).startsWith("Subproject commit "));
  }
  
  @Test
  public void testRebasingFileDiff() throws Exception{
     //The local repositories. 
    String localTestRepository1 = "target/test-resources/local1";
    String localTestRepository2 = "target/test-resources/local2";
    
    //The remote repository. 
    String remoteTestRepository = "target/test-resources/remote";

    GitAccess gitAccess = GitAccess.getInstance();
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo1 = createRepository(localTestRepository1);
    Repository localRepo2 = createRepository(localTestRepository2);
    
    //-------------
    // Set up the repositories for a rebase conflict
    //-------------
    
    //----------------
    // LOCAL 1
    //----------------
    
    // Bind the local repository 1 to the remote one.
    bindLocalToRemote(localRepo1, remoteRepo);
    gitAccess.setRepositorySynchronously(localTestRepository1);

    // Create a new file for the first repository.
    File localFile1 = new File(localTestRepository1 + "/test.txt");
    localFile1.createNewFile();
    // Modify the newly created file.
    setFileContent(localFile1, "initial content");
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("First commit.");
    // Send it to remote/upstream.
    gitAccess.push("", "");
    
    //----------------
    // LOCAL 2
    //----------------
    
    // Bind the local repository 2 to the remote one.
    bindLocalToRemote(localRepo2, remoteRepo);
    gitAccess.setRepositorySynchronously(localTestRepository2);
    
    // Receive changes from remote/upstream.
    PullResponse pull = gitAccess.pull("", "");
    assertEquals(PullStatus.OK.toString(), pull.getStatus().toString());
    
    //Create new file for second repository.
    File local2File = new File(localTestRepository2, "test.txt");
    assertEquals("initial content", read(local2File.toURI().toURL()));
    
    // Modify the file.
    setFileContent(local2File, "changed in local 2, resolved");
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    gitAccess.commit("Second commit");
    // Send it to remote/upstream.
    gitAccess.push("", "");

    //----------------
    // LOCAL 1
    //----------------
    
    gitAccess.setRepositorySynchronously(localTestRepository1);
    // Modify the file.
    setFileContent(localFile1, "changed in local 1, conflict content, original");
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.MODIFIED, "test.txt"));
    // Commit the file.
    gitAccess.commit("Third commit, with conflict");
    
    //------------
    // Rebase conflict prepared and will happen after the pull.
    //------------
    
    final StringBuilder pullWithConflictsSB = new StringBuilder();
    boolean[] wasRebaseInterrupted = new boolean[1];
    final String[] pullFailedMessage = new String[1];
    PushPullController pc = new PushPullController() {
      @Override
      protected void showPullFailedBecauseOfCertainChanges(List<String> changes, String message) {
        pullFailedMessage[0] = message;
      };
      @Override
      protected void showPullSuccessfulWithConflicts(PullResponse response) {
        pullWithConflictsSB.append(response);
      }
      @Override
      protected void showRebaseInProgressDialog() {
        wasRebaseInterrupted[0] = true;
      }
    };
    
    final StringBuilder b = new StringBuilder();
    pc.addObserver(new Observer<PushPullEvent>() {
      @Override
      public void stateChanged(PushPullEvent changeEvent) {
        b.append(changeEvent).append("\n");
      }
    });
    
    // Get conflict
    pc.pull(PullType.REBASE).get();
    assertNull(pullFailedMessage[0]);
    assertFalse(wasRebaseInterrupted[0]);
    assertEquals("Status: CONFLICTS Conflicting files: [test.txt]", pullWithConflictsSB.toString());
    assertTrue(read(localFile1.toURI().toURL()).startsWith("<<<<<<< Upstream, based on branch 'master' of file:"));

    leftDiff = null;
    rightDiff = null;

    // Mock the GitController
    GitController stageController = Mockito.mock(GitController.class);
    FileStatus fileStatus = new FileStatus(GitChangeType.CONFLICT, "test.txt");

    // Invoke DIFF over the changed file.
    DiffPresenter.showDiff(fileStatus, stageController);
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);

    // Verify that each side has the proper tag and content.
    assertTrue(leftDiff.toString().contains("MineResolved"));
    assertTrue(rightDiff.toString().contains("MineOriginal"));
    assertEquals("changed in local 2, resolved", read(leftDiff));
    assertEquals("changed in local 1, conflict content, original", read(rightDiff));
  }
  
  @Test
  public void testLockFailedExceptionHandler() throws Exception {
    
    String localTestRepository = "target/test-resources/local";
    
    String remoteTestRepository = "target/test-resources/remote";
    GitAccess gitAccess = GitAccess.getInstance();
    Repository remoteRepo = createRepository(remoteTestRepository);
    Repository localRepo = createRepository(localTestRepository);
    
    bindLocalToRemote(localRepo, remoteRepo);
    gitAccess.setRepositorySynchronously(localTestRepository);
    
    // Create a new file for the first repository.
    File localFile = new File(localTestRepository + "/test.txt");
    localFile.createNewFile();
    // Modify the newly created file.
    setFileContent(localFile, "initial");
    
    // Add it to the index.
    gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
    gitAccess.commit("Test commit.");
    // Send it to remote/upstream.
    gitAccess.push("", "");
    
    //Create index.lock file in the local repository
    File indexFile = new File(localTestRepository, ".git/index.lock");
    indexFile.createNewFile();
    
    List<String> errorMessages = new ArrayList<>();
    List<Throwable> exceptions = new ArrayList<>();
   
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        errorMessages.add((String) invocation.getArguments()[0]);
        exceptions.add((Throwable) invocation.getArguments()[1]);
        
        return null;
      }
    }).when(PluginWorkspaceProvider.getPluginWorkspace()).showErrorMessage(Mockito.any(), Mockito.any());
    
    //Try to pull
    PushPullController pushPullController = new PushPullController();
    Future<?> pull = pushPullController.pull(PullType.MERGE_FF);
    pull.get();
    
    //Verify an exception was intercepted
    assertNotNull("The exception wan't intercepted.", exceptions.get(0));
    
    //Verify the error message to contain the information of an LockFailedException
    assertTrue(errorMessages.get(0).contains("Cannot lock"));
    assertTrue(errorMessages.get(0).contains("Ensure that no other process has an open file handle on the lock file"));
    assertTrue(errorMessages.get(0).contains("then you may delete the lock file and retry."));
    
    //See if the exception is an instance of LockFailedException
    assertEquals(LockFailedException.class.getName(), exceptions.get(0).getClass().getName());
  }
}
