package com.oxygenxml.git.view.staging;

import java.io.File;
import java.util.Date;

import org.eclipse.jgit.lib.Repository;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.utils.RepositoryStatusInfo;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.refresh.PanelsRefreshSupport;

import ro.sync.basic.io.FileSystemUtil;

/**
 * Toolbar panel tests.
 */
public class ToolbarPanel2Test extends GitTestBase {
  
  private GitAccess gitAccess = GitAccess.getInstance();
  private StagingPanel stagingPanel;
  
  private void createRepo(String remote, String local) throws Exception {
    createRepository(remote);
    Repository remoteRepository = gitAccess.getRepository();

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
  
  /**
   * <p><b>Description:</b> The fetch method should be called before refresh the Git Staging</p>
   * <p><b>Bug ID:</b> EXM-50791</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */
  public void testRefreshToolbar() throws Exception {
    
    File testDir = new File(String.format("target/test-resources/ToolbarPanelTest/%s", this.getName()));
    
    try {
      final GitController gitCtrl = new GitController();
      final long[] fetchTime = new long[1];
      final long[] stagingPanelRefreshTime = new long[1];
      final PanelsRefreshSupport refreshManager = new PanelsRefreshSupport(null) {
    	  @Override
          protected int getScheduleDelay() {
            // Execute refresh events immediately from tests.
            return 1;
          }
    	  
    	  @Override
    	protected RepositoryStatusInfo fetch() {
    		if(fetchTime[0] == 0) {
    			fetchTime[0] = new Date().getTime();
    			sleep(1); // add a small delay for this method
    		}
    		return new RepositoryStatusInfo(null);
    	}
    	  
    	@Override
    	protected void updateStagingPanel(RepositoryStatusInfo repoStatus) {
    		if(stagingPanelRefreshTime[0] == 0) {
    			stagingPanelRefreshTime[0] = new Date().getTime();
    			sleep(1); // add a small delay for this method
    		}
    	}  
      };
      GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
      stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
      refreshManager.setStagingPanel(stagingPanel);
      
      //Creates repos
      String local = String.format("target/test-resources/ToolbarPanelTest/%s/localRepository", this.getName());
      String remote = String.format("target/test-resources/ToolbarPanelTest/%s/remoteRepository", this.getName());

      //Creates the remote repository.
      createRepo(remote, local);

      Repository remoteRepository = createRepository(remote);
      Repository localRepository = createRepository(local);
      bindLocalToRemote(localRepository, remoteRepository);
      flushAWT();

      refreshManager.call();
      waitForScheduler();
         
      assertTrue("The fetch method should be called.", fetchTime[0] > 0);
      assertTrue("The staging panel should be updated.", stagingPanelRefreshTime[0] > 0);
      assertTrue("The fetch should be executed before refreshing staging panel.", stagingPanelRefreshTime[0] > fetchTime[0]);
    } finally {
      FileSystemUtil.deleteRecursivelly(testDir);
    }
  }
  
}
