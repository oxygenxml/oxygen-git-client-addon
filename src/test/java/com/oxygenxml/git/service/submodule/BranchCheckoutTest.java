package com.oxygenxml.git.service.submodule;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.service.TestUtil;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.event.GitController;

/**
 * Submodules processing when changing branches.
 * 
 * @author alex_jitianu
 */
public class BranchCheckoutTest extends GitTestBase {
  /**
   * Logger for logging.
   */
  private static final Logger logger = LoggerFactory.getLogger(BranchCheckoutTest.class);

  /**
   * <p><b>Description: </b> When switching branches submodules should also be reset to teh commit from the branch.</p>
   * <p><b>Bug ID:</b> EXM-53610</p>
   *
   * @author alex_jitianu
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testCheckoutSubmodulesonBranchCheckout() throws Exception {
    try {

      Repository submoduleRepo = createRepository("target/test-resources/branch-checkout--sub");
      String fileName = "file.txt";
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 1");
      
      // Committing a file in the remote makes required initializations.
      Repository remote = createRepository("target/test-resources/branch-checkout--main-remote");
      TestUtil.commitOneFile(remote, "base.txt", "base");
      setupSubmodule(remote, submoduleRepo, "sub");

      Repository mainRepo = createRepository("target/test-resources/branch-checkout--main");

      bindLocalToRemote(mainRepo, remote);

      GitController ctrl = new GitController();
      GitAccess.getInstance().setGit(new Git(mainRepo));
      ctrl.pull().get();

      String content = TestUtil.read(new File(mainRepo.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must be initialized and updated", "version 1", content);
      
      
      ObjectId id = submoduleRepo.resolve(Constants.HEAD);
      // Change the submodule to the last commit from target.
      updateSubmoduleToBranchHead(mainRepo, "sub", id);
      GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "sub"));
      GitAccess.getInstance().commit("Moved ahead.");
      
      logSubmodule();
      
      // Switch to a new branch and forward the submodule.
      GitAccess.getInstance().createBranch("v2");
      GitAccess.getInstance().setBranch("v2");

      // Move the submodule target forward.
      TestUtil.commitOneFile(submoduleRepo, fileName, "version 2");
      id = submoduleRepo.resolve(Constants.HEAD);
      // Change the submodule to the last commit from target.
      updateSubmoduleToBranchHead(mainRepo, "sub", id);
      GitAccess.getInstance().add(new FileStatus(GitChangeType.MODIFIED, "sub"));
      GitAccess.getInstance().commit("Moved ahead.");
      
      logSubmodule();

      content = TestUtil.read(new File(mainRepo.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules must be initialized and updated", "version 2", content);
      
      
      // Switch back on 'main' branch.
      GitAccess.getInstance().setBranch("main");
      content = TestUtil.read(new File(mainRepo.getWorkTree(), "sub/file.txt").toURI().toURL());
      assertEquals("The submodules was set to previous commit", "version 1", content);
    } finally {
      GitAccess.getInstance().setGit(null);
    }
  }

  private void logSubmodule() throws GitAPIException {
    if (logger.isDebugEnabled()) {
      logger.debug("Main repo branch {}", GitAccess.getInstance().getBranchInfo().getBranchName());
      Map<String, SubmoduleStatus> call = GitAccess.getInstance().getGit().submoduleStatus().call();
      Set<String> keySet = call.keySet();
      for (String modID : keySet) {
        logger.debug("  SUBMODULE " + modID);
        logger.debug("  -path-" + call.get(modID).getPath());
        logger.debug("  -head-" + call.get(modID).getHeadId());
        logger.debug("  -index-" + call.get(modID).getIndexId());
        logger.debug("  -type-" + call.get(modID).getType());
      }
    }
  }
  
  /**
   * Updates a submodule to the branch head commit.
   * 
   * @param mainRepo Main repository.
   * @param submoduleName Submodule name.
   * @param id 
   * 
   * @throws Exception If it fails.
   */
  private void updateSubmoduleToBranchHead(Repository mainRepo, String submoduleName, ObjectId id) throws Exception {
    // Update submodule to "main" branch head.
    try (
        Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(mainRepo, submoduleName);
        Git git = new Git(submoduleRepository)) {
        git.fetch().setRecurseSubmodules(FetchRecurseSubmodulesMode.YES).call();
        git.checkout().setName(id.getName()).call();
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
}
