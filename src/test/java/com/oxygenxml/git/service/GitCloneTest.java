package com.oxygenxml.git.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.FileUtil;

import ro.sync.basic.io.FileSystemUtil;

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
  @Test
  public void testClone1() throws Exception {
    String localRepoLoc = "target/test-resources/GitCloneTest2/testClone1-local";
    String remoteRepoLoc = "target/test-resources/GitCloneTest2/testClone1-remote";
    String cloneDestLoc = "target/test-resources/GitCloneTest2/testClone1-cloneDest";
    String cloneDestLoc2 = "target/test-resources/GitCloneTest2/testClone1-cloneDest2";
    File localDir = new File(localRepoLoc);
    File cloneDest = new File(cloneDestLoc);
    File cloneDest2 = new File(cloneDestLoc2);

    try {
      Repository localRepo = createRepository(localRepoLoc);
      Repository remoteRepo = createRepository(remoteRepoLoc);
      bindLocalToRemote(localRepo, remoteRepo);

      GitAccess gitAccess = GitAccess.getInstance();
      gitAccess.setRepositorySynchronously(localRepoLoc);

      // Push a file in order to create the remote main branch
      localDir.mkdirs();
      File localTestFile = new File(localDir, "test.txt");
      localTestFile.createNewFile();
      gitAccess.add(new FileStatus(GitChangeType.ADD, "test.txt"));
      gitAccess.commit("");
      push("", "");
      

      // Create a second branch ("slave")
      RefSpec spec = new RefSpec("refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME + ":refs/heads/slave");
      gitAccess.getGit().push().setRefSpecs(spec).call();
      
      // Commit another file on main
      localTestFile = new File(localDir, "test2.txt");
      localTestFile.createNewFile();
      gitAccess.add(new FileStatus(GitChangeType.ADD, "test2.txt"));
      gitAccess.commit("");
      push("", "");
      
      // Check branches
      Collection<Ref> branches = gitAccess.listRemoteBranchesForURL(
          new URIish(remoteRepo.getWorkTree().toURI().toURL()), null);
      assertEquals(2, branches.size());
      Ref[] branchesArray = branches.toArray(new Ref[0]);
      assertEquals("refs/heads/" + GitAccess.DEFAULT_BRANCH_NAME , branchesArray[0].getName());
      assertEquals("refs/heads/slave", branchesArray[1].getName());
      
      // Now clone the repository and checkout the default branch.
      // Should be "main"
      gitAccess.clone(
          new URIish(remoteRepo.getDirectory().toURI().toURL()),
          cloneDest,
          Optional.empty(),
          null);
      assertEquals(GitAccess.DEFAULT_BRANCH_NAME, gitAccess.getRepository().getBranch());
      
      // Check what we have in the destination folder
      List<File> files = new ArrayList<>();
      FileSystemUtil.listRecursively(new File[] {cloneDest}, false, f -> !f.getName().equals(".git") , files);
      assertEquals(2, files.size());
      assertTrue(files.toString().contains("test.txt"));
      // Only the "main" branch should have this
      assertTrue(files.toString().contains("test2.txt"));
      
      // Now clone the repository and checkout the "slave" branch
      gitAccess.clone(
          new URIish(remoteRepo.getDirectory().toURI().toURL()),
          cloneDest2,
          Optional.empty(),
          "refs/heads/slave");
      assertEquals("slave", gitAccess.getRepository().getBranch());
      // EXM-47079 Test the fetch value
      StoredConfig config = gitAccess.getRepository().getConfig();
      String value = config.getString(ConfigConstants.CONFIG_REMOTE_SECTION, Constants.DEFAULT_REMOTE_NAME, ConfigConstants.CONFIG_FETCH_SECTION);
      assertEquals("+refs/heads/*:refs/remotes/origin/*", value);
      
      // Check what we have in the destination folder
      files = Arrays.asList(cloneDest2.listFiles(File::isFile));
      assertEquals(1, files.size());
      assertTrue(files.toString().contains("test.txt"));
      // The second file shouldn't be here. Only on "main", because
      // "slave" was created earlier than when "test2.txt" was pushed. 
      assertFalse(files.toString().contains("test2.txt"));
      
    } finally {
      // Free the resources and clean the destination folders
      GitAccess.getInstance().closeRepo();
      FileUtil.deleteRecursivelly(cloneDest);
      FileUtil.deleteRecursivelly(cloneDest2);
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
    gitAccess.setRepositorySynchronously(localTestRepositoryP);
    // A setup is performed on the first commit.
    gitAccess.commit("");
    push("", "");
    
    // SUBMODULE repos
    String remoteTestRepositorySubModule = "target/test-resources/GitCloneTest/testCloneSubmodules-remoteCS-SubModule/";
    Repository remoteRepoSubModule = createRepository(remoteTestRepositorySubModule);
    gitAccess.setRepositorySynchronously(remoteTestRepositorySubModule);
    gitAccess.commit("Commit");
    
    // Link the submodule in the main repository.
    gitAccess.setRepositorySynchronously(localTestRepositoryP);
    // Add SUBMODULE
    SubmoduleAddCommand addCommand = gitAccess.getGit().submoduleAdd();
    addCommand.setURI(remoteRepoSubModule.getDirectory().toURI().toString());
    addCommand.setPath("modules/submodule");
    Repository subRepo = addCommand.call();
    subRepo.close();
    
    gitAccess.setRepositorySynchronously(localTestRepositoryP);
    gitAccess.commit("Submodule add");
    push("", "");
    
    File cloneDest = new File("target/test-resources/GitCloneTest/testCloneSubmodules-remoteCS-2");
    FileUtil.deleteRecursivelly(cloneDest);
    // Clone the remote.
    gitAccess.clone(
        new URIish(localRepoP.getDirectory().toURI().toURL()),
        cloneDest,
        Optional.empty(),
        null);
    
    // The newly cloned repository must be removed when the test is done.
    Repository repository = gitAccess.getRepository();
    record4Cleanup(repository);
    
    Set<String> submodules = gitAccess.getSubmoduleAccess().getSubmodules();
    assertEquals("[modules/submodule]", submodules.toString());
    
    gitAccess.setSubmodule("modules/submodule");
    
    assertTrue("The current host must be registered for auth.", AuthenticationInterceptor.isBound(gitAccess.getHostName()));;
    
    File module = new File(cloneDest, ".git/modules/modules/submodule");
    assertEquals(module.getAbsolutePath(), gitAccess.getRepository().getDirectory().getAbsolutePath());
  }
}
