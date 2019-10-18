package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.swing.JFrame;

import org.eclipse.jgit.api.SubmoduleAddCommand;
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
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

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
      StandalonePluginWorkspace mock = Mockito.mock(StandalonePluginWorkspace.class);
      PluginWorkspaceProvider.setPluginWorkspace(mock);
      
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
    StageController stageController = Mockito.mock(StageController.class);
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
    StageController stageController = Mockito.mock(StageController.class);
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
   * <p><b>Description:</b> diff on submodule.</p>
   * <p><b>Bug ID:</b> EXM-40621</p>
   *
   * @author sorin_carbunaru
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
        Mockito.mock(StageController.class));
    
    assertNotNull(leftDiff);
    assertNotNull(rightDiff);
    
    String left = "git://CurrentSubmodule/modules/submodule.txt";
    assertEquals(left, leftDiff.toString());
    assertTrue(read(new URL(left)).startsWith("Subproject commit "));
    
    String right = "git://PreviousSubmodule/modules/submodule.txt";
    assertEquals(right, rightDiff.toString());
    assertTrue(read(new URL(right)).startsWith("Subproject commit "));
  }
  
}
