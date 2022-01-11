package com.oxygenxml.git.view;

import java.io.File;

import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.StagingPanel;
import com.oxygenxml.git.view.staging.ToolbarPanel;

import ro.sync.basic.io.FileSystemUtil;

/**
 * Toolbar panel tests.
 */
public class ToolbarPanelTest extends GitTestBase {
  
  private GitAccess gitAccess = GitAccess.getInstance();
  private StagingPanel stagingPanel;
  
  
  
  private void createRepo(String remote, String local) throws Exception {
    createRepository(remote);
    Repository remoteRepository = gitAccess.getRepository();

    //Creates the local repository.
    createRepository(local);
    Repository localRepository = gitAccess.getRepository();

    bindLocalToRemote(localRepository, remoteRepository);
  }
  
  
  /**
   * <p><b>Description:</b> when trying to switch to another branch from the branches menu
   * and the checkout fails, tests the dialog</p>
   * <p><b>Bug ID:</b> EXM-48502</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testButtonsWhenNoRepo() throws Exception {
    
    File testDir = new File(String.format("target/test-resources/ToolbarPanelTest/%s", this.getName()));
    
    try {
      GitController gitCtrl = new GitController();
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      refreshSupport.setStagingPanel(stagingPanel);
      
      ToolbarPanel toolbar = stagingPanel.getToolbarPanel();
      assertFalse("No repo, the button should be disabled.", toolbar.getPullMenuButton().isEnabled());
      assertFalse(toolbar.getPushButton().isEnabled());
      assertFalse(toolbar.getStashButton().isEnabled());

      //Creates repos
      String local = String.format("target/test-resources/ToolbarPanelTest/%s/localRepository", this.getName());
      String remote = String.format("target/test-resources/ToolbarPanelTest/%s/remoteRepository", this.getName());

      //Creates the remote repository.
      createRepo(remote, local);

      Repository remoteRepository = createRepository(remote);
      Repository localRepository = createRepository(local);
      bindLocalToRemote(localRepository, remoteRepository);
      flushAWT();

      assertTrue("Pull Merge action should be enabled.", gitActionsManager.getPullMergeAction().isEnabled());
      assertTrue("Pull Rebase action should be enabled.", gitActionsManager.getPullRebaseAction().isEnabled());
      assertTrue(toolbar.getPullMenuButton().isEnabled());

      assertTrue("Push action should be enabled.", gitActionsManager.getPushAction().isEnabled());
      assertTrue(toolbar.getPushButton().isEnabled());

      assertFalse(toolbar.getStashButton().isEnabled());
    } finally {
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }
  
}
