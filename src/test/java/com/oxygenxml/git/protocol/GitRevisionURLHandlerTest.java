package com.oxygenxml.git.protocol;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.script.RepoGenerationScript;
import com.oxygenxml.git.view.historycomponents.CommitCharacteristics;

/**
 * Tests for obtaining different versions of a file ({@link VersionIdentifier}) through a protocol.
 * 
 * 
 * 
 * @author alex_jitianu
 */
public class GitRevisionURLHandlerTest extends GitTestBase {

  /**
   * Tests the URLs that access the INDEX version of a file and the HEAD version. 
   */
  @Test
  public void testGetVersionContent() throws Exception {
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
    file.createNewFile();    // Modify the newly created file.
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
    
    // Get the INDEX version.
    String indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/test.txt";
    assertEquals("index content", read(new URL(indexVersionURL)));
    
    // Get the HEAD version.
    String headVersionURL = "git://" + VersionIdentifier.LAST_COMMIT  + "/test.txt";
    assertEquals("initial content", read(new URL(headVersionURL)));
  }
  
  /**
   * Tests the URL form that can get the file content from any revision.
   * 
   * @throws Exception If it fails.
   */
  @Test
  public void testCommitID() throws Exception {
    URL script = getClass().getClassLoader().getResource("scripts/file_content_script.txt");
    
    File wcTree = new File("target/gen/GitRevisionURLHandlerTest_testCommitID");
    RepoGenerationScript.generateRepository(script, wcTree);
    
    try {
      GitAccess.getInstance().setRepository(wcTree.getAbsolutePath());

      String indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/file1.txt";
      assertEquals("file 1 Third commit.", read(new URL(indexVersionURL)));
      
      indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/file1.txt";
      assertEquals("file 1 Third commit.", read(new URL(indexVersionURL)));
      
      List<CommitCharacteristics> commitsCharacteristics = GitAccess.getInstance().getCommitsCharacteristics(null);
      
      assertEquals(3, commitsCharacteristics.size());
      
      Iterator<CommitCharacteristics> iterator = commitsCharacteristics.iterator();
      CommitCharacteristics revCommit = iterator.next();
      
      assertEquals("Third.", revCommit.getCommitMessage());
      indexVersionURL = "git://" + revCommit.getCommitId() + "/file1.txt";
      assertEquals("file 1 Third commit.", read(new URL(indexVersionURL)));
      
      revCommit = iterator.next();
      assertEquals("Second.", revCommit.getCommitMessage());
      indexVersionURL = "git://" + revCommit.getCommitId() + "/file1.txt";
      assertEquals("file 1 Second commit.", read(new URL(indexVersionURL)));
      
      revCommit = iterator.next();
      assertEquals("First commit.", revCommit.getCommitMessage());
      indexVersionURL = "git://" + revCommit.getCommitId() + "/file1.txt";
      assertEquals("file 1 First commit.", read(new URL(indexVersionURL)));
      
      
      
      
    } finally {
      GitAccess.getInstance().close();
      
      FileUtils.deleteDirectory(wcTree);
    }
  }

}
