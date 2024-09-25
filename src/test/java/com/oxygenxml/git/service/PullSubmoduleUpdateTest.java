package com.oxygenxml.git.service;

import java.io.File;
import java.util.Optional;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.URIish;
import org.junit.Test;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.event.GitController;

/**
 * Update submodules after a pull.
 * 
 * @author alex_jitianu
 */
public class PullSubmoduleUpdateTest extends GitTestBase {

  /**
   * <p><b>Description:</b> Update submodules on pull.</p>
   * <p><b>Bug ID:</b> EXM-47461</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testUpdate() throws Exception {
    try {

      Repository submoduleRepo = createRepository("target/test-resources/PullSubmoduleUpdate_sub");
      String fileName = "file.txt";
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 1");

      // Committing a file in the remote makes required initializations.
      Repository remote = createRepository("target/test-resources/PullSubmodule_main_remote");
      TestUtil.commitOneFile(remote, "base.txt", "base");
      setupSubmodule(remote, submoduleRepo, "sub");

      Repository db2 = createRepository("target/test-resources/PullSubmoduleUpdate_main");

      bindLocalToRemote(db2, remote);

      GitController ctrl = new GitController();
      GitAccess.getInstance().setGit(new Git(db2));
      ctrl.pull(Optional.empty()).get();

      String content = TestUtil.read(new File(db2.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must be initialized and updated", "version 1", content);

      // Move the submodule target forward.
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 2");
      // Change the submodule to the last commit from target.
      updateSubmoduleToBranchHead(remote, "sub");

      // Pull again.
      GitAccess.getInstance().setGit(new Git(db2));
      ctrl.pull(Optional.empty()).get();
      content = TestUtil.read(new File(db2.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must be initialized and updated", "version 2", content);
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }
  
  /**
   * Updates a submodule to the branch head commit.
   * 
   * @param mainRepo Main repository.
   * @param submoduleName Submodule name.
   * 
   * @throws Exception If it fails.
   */
  private void updateSubmoduleToBranchHead(Repository mainRepo, String submoduleName) throws Exception {
    // Update submodule to "main" branch head.
    try (
        Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(mainRepo, submoduleName);
        Git git = new Git(submoduleRepository)) {
        git.checkout().setName(GitAccess.DEFAULT_BRANCH_NAME).setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM).call();
        git.pull().call();
    }
    
    // Commit the submodule change, the "sub" marker file, in the main repository.
    try (Git git = new Git(mainRepo)) {
      git.add().addFilepattern("sub").call();
      git.commit().setMessage("Submodule commit v2").call();
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    OptionsManager.getInstance().setUpdateSubmodulesOnPull(true);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * 
   * @param submoduleRepo Target repository.
   * @param db1 Main repository.
   * 
   * @throws Exception If it fails.
   */
  private void setupSubmodule(Repository db1, Repository submoduleRepo, String submoduleName) throws Exception {
    
    try(Git git = new Git(db1)) {
      Repository call = git.submoduleAdd().setName(submoduleName).setPath(submoduleName)
          .setURI(submoduleRepo.getDirectory().toURI().toURL().toExternalForm().replace("file:/", "file:///")).call();
      
      call.close();
      
      git.add().addFilepattern("*").call();
      git.commit().setMessage("Submodule commit").call();
      
      RepoUtil.updateSubmodules(git);
    }
  }

  /**
   * <p><b>Description:</b> Update submodules on pull.</p>
   * <p><b>Bug ID:</b> EXM-47461</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testUpdateRecursively() throws Exception {
    
    try {


      Repository submoduleRepoLvl1 = createRepository("target/test-resources/PullSubmoduleUpdate_sub");
      String fileName = "file.txt";
      TestUtil.commitOneFile(submoduleRepoLvl1, fileName, "version 1");

      // Committing a file in the remote makes required initializations.
      Repository submoduleRepoLvl2 = createRepository("target/test-resources/PullSubmodule_main_remote");
      TestUtil.commitOneFile(submoduleRepoLvl2, "base.txt", "base");
      setupSubmodule(submoduleRepoLvl2, submoduleRepoLvl1, "sub");

      // Committing a file in the remote makes required initializations.
      Repository remote2 = createRepository("target/test-resources/PullSubmodule_main_remote_2");
      TestUtil.commitOneFile(remote2, "main.txt", "main");
      setupSubmodule(remote2, submoduleRepoLvl2, "main");

      // Assert submodules.
      assertEquals(
          "The submodules must be initialized and updated", 
          "base", 
          TestUtil.read(new File(remote2.getWorkTree(), "main/base.txt").toURI().toURL()));

      assertEquals(
          "The submodules must be initialized and updated", 
          "version 1", 
          TestUtil.read(new File(remote2.getWorkTree(), "main/sub/file.txt").toURI().toURL()));

      // Main entry point.
      Repository db2 = createRepository("target/test-resources/PullSubmoduleUpdate_main");
      bindLocalToRemote(db2, remote2);

      GitController ctrl = new GitController();
      GitAccess.getInstance().setGit(new Git(db2));
      ctrl.pull(Optional.empty()).get();

      assertEquals(
          "The submodules must be initialized and updated", 
          "base", 
          TestUtil.read(new File(db2.getWorkTree(), "main/base.txt").toURI().toURL()));

      assertEquals(
          "The submodules must be initialized and updated", 
          "version 1", 
          TestUtil.read(new File(db2.getWorkTree(), "main/sub/file.txt").toURI().toURL()));
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }

  /**
   * <p><b>Description:</b> Update submodules on clone.</p>
   * <p><b>Bug ID:</b> EXM-45658</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testClone() throws Exception {
    Repository submoduleRepoLvl1 = createRepository("target/test-resources/PullSubmoduleUpdate_sub");
    String fileName = "file.txt";
    TestUtil.commitOneFile(submoduleRepoLvl1, fileName, "version 1");
    
    // Committing a file in the remote makes required initializations.
    Repository submoduleRepoLvl2 = createRepository("target/test-resources/PullSubmodule_main_remote");
    TestUtil.commitOneFile(submoduleRepoLvl2, "base.txt", "base");
    setupSubmodule(submoduleRepoLvl2, submoduleRepoLvl1, "sub");
    
    // Committing a file in the remote makes required initializations.
    Repository remote2 = createRepository("target/test-resources/PullSubmodule_main_remote_2");
    TestUtil.commitOneFile(remote2, "main.txt", "main");
    setupSubmodule(remote2, submoduleRepoLvl2, "main");
    
    // Assert submodules.
    assertEquals(
        "The submodules must be initialized and updated", 
        "base", 
        TestUtil.read(new File(remote2.getWorkTree(), "main/base.txt").toURI().toURL()));
    
    assertEquals(
        "The submodules must be initialized and updated", 
        "version 1", 
        TestUtil.read(new File(remote2.getWorkTree(), "main/sub/file.txt").toURI().toURL()));
    
    File directory = new File("target/test-resources/clone");
    GitAccess.getInstance().clone(
        new URIish(remote2.getDirectory().toURI().toURL().toString()), 
        directory,
        Optional.empty(), 
        null);
    
    assertEquals(
        "The submodules must be initialized and updated", 
        "base", 
        TestUtil.read(new File(directory, "main/base.txt").toURI().toURL()));
    
    assertEquals(
        "The submodules must be initialized and updated", 
        "version 1", 
        TestUtil.read(new File(directory, "main/sub/file.txt").toURI().toURL()));
  }

  /**
   * <p><b>Description:</b> If the option is disabled, do not update submodules on pull.</p>
   * <p><b>Bug ID:</b> EXM-47461</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  public void testDoNotUpdate() throws Exception {
    
    try {
      Repository submoduleRepo = createRepository("target/test-resources/PullSubmoduleUpdate_sub");
      String fileName = "file.txt";
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 1");
      
      // Committing a file in the remote makes required initializations.
      Repository remote = createRepository("target/test-resources/PullSubmodule_main_remote");
      TestUtil.commitOneFile(remote, "base.txt", "base");
      setupSubmodule(remote, submoduleRepo, "sub");
      
      Repository db2 = createRepository("target/test-resources/PullSubmoduleUpdate_main");
      
      bindLocalToRemote(db2, remote);
      
      GitController ctrl = new GitController();
      GitAccess.getInstance().setGit(new Git(db2));
      ctrl.pull(Optional.empty()).get();
      
      String content = TestUtil.read(new File(db2.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must be initialized and updated", "version 1", content);
      
      // Move the submodule target forward.
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 2");
      // Change the submodule to the last commit from target.
      updateSubmoduleToBranchHead(remote, "sub");
      
      OptionsManager.getInstance().setUpdateSubmodulesOnPull(false);
      // Pull again.
      GitAccess.getInstance().setGit(new Git(db2));
      ctrl.pull(Optional.empty()).get();
      content = TestUtil.read(new File(db2.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must no be initialized and updated automatically", "version 1", content);
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }
}
