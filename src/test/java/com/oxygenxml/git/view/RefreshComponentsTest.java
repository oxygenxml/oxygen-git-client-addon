package com.oxygenxml.git.view;

import java.io.File;
import java.io.IOException;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.eclipse.jgit.lib.Repository;
import org.mockito.Mockito;

import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.branches.BranchManagementPanel;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.history.HistoryController;
import com.oxygenxml.git.view.history.HistoryPanel;
import com.oxygenxml.git.view.refresh.PanelRefresh;
import com.oxygenxml.git.view.staging.BranchSelectionCombo;
import com.oxygenxml.git.view.staging.StagingPanel;

/**
 * Used to test the refresh for some components.
 * 
 * @author alex_smarandache
 *
 */
public class RefreshComponentsTest extends GitTestBase {
  
  /**
   * The local repository string.
   */
  private final static String LOCAL_TEST_REPOSITPRY = "target/test-resources/RefreshComponentsTest/local";
  
  /**
   * The remote repository string.
   */
  private final static String REMOTE_TEST_REPOSITPRY = "target/test-resources/RefreshComponentsTest/remote";
  
  /**
   * The local repository.
   */
  private Repository localRepo;
  
  /**
   * The remote repository.
   */
  private Repository remoteRepo;
  
  /**
   * Git access.
   */
  private GitAccess gitAccess;

  /**
   * Set up before each test.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    deleteTestResources();

    gitAccess = GitAccess.getInstance();
    gitAccess.createNewRepository(REMOTE_TEST_REPOSITPRY);
    remoteRepo = gitAccess.getRepository();
    gitAccess.createNewRepository(LOCAL_TEST_REPOSITPRY);
    localRepo = gitAccess.getRepository();

    bindLocalToRemote(localRepo, remoteRepo);
  }

  /**
   * <p><b>Description:</b> Tests if the @HistoryPanel is refreshed on refresh support call.</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testRefreshHistory() {
    final boolean[] wasRefreshed = { false };
    final HistoryPanel historyPanel = new HistoryPanel(new GitController()) {
      @Override
      public boolean isShowing() {
        return true;
      }

      @Override
      public void scheduleRefreshHistory() {
        wasRefreshed[0] = true;
      }
    };

    final PanelRefresh refreshManager = new PanelRefresh(null);
    assertNotNull(historyPanel);
    refreshManager.setHistoryPanel(historyPanel);
    refreshManager.call();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> wasRefreshed[0]);
  }

  /**
   * <p><b>Description:</b> Tests if the @BranchManagementPanel is refreshed on refresh support call.</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testRefreshBranchesManager() {
    final boolean[] wasRefreshed = { false };
    final BranchManagementPanel branchManager = new BranchManagementPanel(new GitController()) {
      @Override
      public boolean isShowing() {
        return true;
      }

      @Override
      public void refreshBranches() {
        wasRefreshed[0] = true;
      }
    };

    final PanelRefresh refreshManager = new PanelRefresh(null);
    assertNotNull(branchManager);
    refreshManager.setBranchPanel(branchManager);
    refreshManager.call();
    Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> wasRefreshed[0]);
  }

  /**
   * <p><b>Description:</b> Tests if the @StagingPanel is refreshed on refresh support call.</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testRefreshStaging() {
    final int[] noRefreshes = { 0 };
    final PanelRefresh refreshManager = new PanelRefresh(null);
    final BranchSelectionCombo comboSel = new BranchSelectionCombo(new GitController()) {
      @Override
      public void refresh() {
        noRefreshes[0]++;
      }
    };

    final StagingPanel stagingPanel = new StagingPanel(refreshManager, new GitController(), 
        Mockito.mock(HistoryController.class), 
        new GitActionsManager(new GitController(), null, null, refreshManager)) {

      @Override
      public boolean isShowing() {
        return true;
      }

      @Override
      public BranchSelectionCombo getBranchesCombo() { 
        return comboSel;
      };

      @Override
      public void updateConflictButtonsPanelBasedOnRepoState() {
        noRefreshes[0]++;
      }

      @Override
      public void updateToolbarsButtonsStates() {
        noRefreshes[0]++;
      }
    };
    
    refreshManager.setStagingPanel(stagingPanel);
    refreshManager.call();
    Awaitility.await().atMost(Duration.FIVE_SECONDS).until(() -> noRefreshes[0] == 3);
  }
  
  /**
   * Delete test resources.
   */
  private void deleteTestResources() {
    File dirToDelete = new File(LOCAL_TEST_REPOSITPRY);
    FileUtil.deleteRecursivelly(dirToDelete);
    dirToDelete = new File(REMOTE_TEST_REPOSITPRY);
    FileUtil.deleteRecursivelly(dirToDelete);
  }

}
