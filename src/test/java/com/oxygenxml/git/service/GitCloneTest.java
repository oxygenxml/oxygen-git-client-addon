package com.oxygenxml.git.service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;
import org.mockito.Mockito;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.io.FileSystemUtil;

/**
 * Test cases for Git cloning.
 * 
 * @author sorin_carbunaru
 */
public class GitCloneTest extends GitTestBase {

  /**
   * <p><b>Description:</b> clone a repository. First checkout the default branch, 
   * the second time checkout another branch.</p>
   * <p><b>Bug ID:</b> EXM-</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testClone1() throws Exception {
    String localRepoLoc = "target/test-resources/GitCloneTest/testClone1-local";
    String remoteRepoLoc = "target/test-resources/GitCloneTest/testClone1-remote";
    String cloneDestLoc = "target/test-resources/GitCloneTest/testClone1-cloneDest";
    String cloneDestLoc2 = "target/test-resources/GitCloneTest/testClone1-cloneDest2";
    File localDir = new File(localRepoLoc);
    File cloneDest = new File(cloneDestLoc);
    File cloneDest2 = new File(cloneDestLoc2);

    try {
      Repository localRepo = createRepository(localRepoLoc);
      Repository remoteRepo = createRepository(remoteRepoLoc);
      bindLocalToRemote(localRepo, remoteRepo);

      GitAccess gitAccess = GitAccess.getInstance();
      gitAccess.setRepository(localRepoLoc);

      // Push a file in order to create the remote master branch
      localDir.mkdirs();
      File localTestFile = new File(localDir, "test.txt");
      localTestFile.createNewFile();
      gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
      gitAccess.commit("");
      gitAccess.push("", "");

      // Create a second branch ("slave")
      RefSpec spec = new RefSpec("refs/heads/master:refs/heads/slave");
      gitAccess.getGitForTests().push().setRefSpecs(spec).call();
      
      // Commit another file on "master"
      localTestFile = new File(localDir, "test2.txt");
      localTestFile.createNewFile();
      gitAccess.add(new FileStatus(GitChangeType.ADD, "test2.txt"));
      gitAccess.commit("");
      gitAccess.push("", "");
      
      // Check branches
      Collection<Ref> branches = gitAccess.listRemoteBranchesForURL(
          new URIish(remoteRepo.getWorkTree().toURI().toURL()), null);
      assertEquals(2, branches.size());
      Ref[] branchesArray = branches.toArray(new Ref[0]);
      assertEquals("refs/heads/master", branchesArray[0].getName());
      assertEquals("refs/heads/slave", branchesArray[1].getName());
      
      // Now clone the repository and checkout the default branch.
      // Should be "master"
      gitAccess.clone(
          new URIish(remoteRepo.getDirectory().toURI().toURL()),
          cloneDest,
          null,
          null);
      assertEquals("master", gitAccess.getRepository().getBranch());
      
      // Check what we have in the destination folder
      List<File> files = new ArrayList<>();
      FileSystemUtil.listRecursively(new File[] {cloneDest}, false, null, files);
      assertEquals(14, files.size());
      assertTrue(files.toString().contains("test.txt"));
      // Only the "master" branch should have this
      assertTrue(files.toString().contains("test2.txt"));
      
      // Now clone the repository and checkout the "slave" branch
      gitAccess.clone(
          new URIish(remoteRepo.getDirectory().toURI().toURL()),
          cloneDest2,
          null,
          "slave");
      assertEquals("slave", gitAccess.getRepository().getBranch());
      
      // Check what we have in the destination folder
      files = new ArrayList<>();
      FileSystemUtil.listRecursively(new File[] {cloneDest2}, false, null, files);
      assertEquals(13, files.size());
      assertTrue(files.toString().contains("test.txt"));
      // The second file shouldn't be here. Only on "master", because
      // "slave" was created earlier than when "test2.txt" was pushed. 
      assertFalse(files.toString().contains("test2.txt"));
      
    } finally {
      // Free the resources and clean the destination folders
      GitAccess.getInstance().close();
      FileUtils.deleteDirectory(cloneDest);
      FileUtils.deleteDirectory(cloneDest2);
    }
  }
  
  
  /**
   * <p><b>Description:</b> Clone a project that has submodules, and try to load the submodule.</p>
   * <p><b>Bug ID:</b> EXM-42006</p>
   *
   * @author alex_jitianu 
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testCloneSubmodules() throws Exception {
    GitAccess gitAccess = GitAccess.getInstance();
    
    // PARENT repos
    String localTestRepositoryP = "target/test-resources/GitCloneTest/testCloneSubmodules-localCS";
    String remoteTestRepositoryP = "target/test-resources/GitCloneTest/testCloneSubmodules-remoteCS";
    
    Repository remoteRepoP = createRepository(remoteTestRepositoryP);
    Repository localRepoP = createRepository(localTestRepositoryP);
    bindLocalToRemote(localRepoP, remoteRepoP);
    
    // Do a commit to bind the repositories together.
    gitAccess.setRepository(localTestRepositoryP);
    // A setup is performed on the first commit.
    gitAccess.commit("");
    gitAccess.push("", "");
    
    // SUBMODULE repos
    String remoteTestRepositorySubModule = "target/test-resources/GitCloneTest/testCloneSubmodules-remoteCS-SubModule/";
    Repository remoteRepoSubModule = createRepository(remoteTestRepositorySubModule);
    gitAccess.setRepository(remoteTestRepositorySubModule);
    gitAccess.commit("Commit");
    
    // Link the submodule in the main repository.
    gitAccess.setRepository(localTestRepositoryP);
    // Add SUBMODULE
    SubmoduleAddCommand addCommand = gitAccess.submoduleAdd();
    addCommand.setURI(remoteRepoSubModule.getDirectory().toURI().toString());
    addCommand.setPath("modules/submodule");
    Repository subRepo = addCommand.call();
    subRepo.close();
    
    gitAccess.setRepository(localTestRepositoryP);
    gitAccess.commit("Submodule add");
    gitAccess.push("", "");
    
    File cloneDest = new File("target/test-resources/GitCloneTest/testCloneSubmodules-remoteCS-2");
    FileUtils.deleteDirectory(cloneDest);
    // Clone the remote.
    gitAccess.clone(
        new URIish(localRepoP.getDirectory().toURI().toURL()),
        cloneDest,
        null,
        null);
    
    // The newly cloned repository must be removed when the test is done.
    Repository repository = gitAccess.getRepository();
    record4Cleanup(repository);
    
    Set<String> submodules = gitAccess.getSubmodules();
    assertEquals("[modules/submodule]", submodules.toString());
    
    gitAccess.setSubmodule("modules/submodule");
    
    File module = new File(cloneDest, ".git\\modules\\modules\\submodule");
    assertEquals(module.getAbsolutePath(), gitAccess.getRepository().getDirectory().getAbsolutePath());
  }
}
