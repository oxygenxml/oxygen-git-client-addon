package com.oxygenxml.git.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;

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

}
