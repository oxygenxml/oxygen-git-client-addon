package com.oxygenxml.git.protocol;

import java.io.File;
import java.net.URL;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

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
    
    // Get the INDEX version.
    String indexVersionURL = "git://" + VersionIdentifier.INDEX_OR_LAST_COMMIT  + "/test.txt";
    assertEquals("index content", read(new URL(indexVersionURL)));
    
    // Get the HEAD version.
    String headVersionURL = "git://" + VersionIdentifier.LAST_COMMIT  + "/test.txt";
    assertEquals("initial content", read(new URL(headVersionURL)));
  }
  
  @Test
  public void testConflict() throws Exception {
    
//    Pattern compile = Pattern.compile("Icons.", Pattern.DOTALL);
//    Matcher matcher = compile.matcher("Icons\r");
//    System.out.println("matcher.matches() " + matcher.matches());
//    
//    // TODO Use GitAccessConflictTest as inspiration.
//    IMatcher createPathMatcher = PathMatcher.createPathMatcher("Icons\r", '/', false);
//    System.out.println(createPathMatcher.getClass());
//    boolean matches = createPathMatcher.matches("Icons\r", false, false);
//    
//    assertTrue(matches);
//    
//    createPathMatcher = PathMatcher.createPathMatcher("Icons*", '/', false);
//    System.out.println(createPathMatcher.getClass());
//    matches = createPathMatcher.matches("Icons\r", false, false);
//    
////    Field declaredField = createPathMatcher.getClass().getDeclaredField("p");
////    declaredField.setAccessible(true);
////    Pattern p = (Pattern) declaredField.get(createPathMatcher);
////    System.out.println("Pattern " + p);
//    assertTrue(matches);
  }

}
