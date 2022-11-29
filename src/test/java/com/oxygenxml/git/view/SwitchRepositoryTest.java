package com.oxygenxml.git.view;

import java.io.File;
import java.net.URL;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.ProjectHelper;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.view.actions.GitActionsManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.staging.StagingPanel;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectChangeListener;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

/**
 * Tests the strategies of Working Copy update on the case when the project opened in Project View is changed.
 * 
 * @author alex_smarandache
 *
 */
public class SwitchRepositoryTest extends GitTestBase {

  /**
   * The path to the first local repository.
   */
  private final static String LOCAL_REPO = "target/test-resources/SwitchRepositoryTest/localRepository";
  
  /**
   * The path to the second local repository.
   */
  private final static String LOCAL_REPO2 = "target/test-resources/SwitchRepositoryTest/localRepository2";
  
  /**
   * The unique instance of Git Access class. 
   */
  private GitAccess gitAccess = GitAccess.getInstance();
  
  /**
   * The staging panel used on tests.
   */
  private StagingPanel stagingPanel; 
  

  /**
   * Call the super method on the parent class and create the repositories and staging panel.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    createRepository(LOCAL_REPO);
    createRepository(LOCAL_REPO2);

    // Init UI
    GitController gitCtrl = new GitController();
    GitActionsManager gitActionsManager = new GitActionsManager(gitCtrl, null, null, refreshSupport);
    stagingPanel = new StagingPanel(refreshSupport, gitCtrl, null, gitActionsManager);
  }
 
  /**
   * <p><b>Description:</b> Tests the strategy when the current repository should be auto-switched on project update.</p>
   * <p><b>Bug ID:</b> EXM-47264</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testAutoSwitch() throws Exception {
    final String project1Name = "Project1.xpr";
    final String project2Name = "Project2.xpr";
    final File project1File = new File(LOCAL_REPO, project1Name);
    final File project2File = new File(LOCAL_REPO2, project2Name);
    assertTrue(project1File.createNewFile());
    assertTrue(project2File.createNewFile());
    final URL project1URL = project1File.toURI().toURL();
    final URL project2URL = project2File.toURI().toURL();

    try {
      gitAccess.setRepositorySynchronously(LOCAL_REPO);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project1File.getParentFile().getAbsolutePath()));
      final StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
      final ProjectChangeListener projectListener[] = new ProjectChangeListener[1];

      OptionsManager.getInstance().setWhenRepoDetectedInProject(WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC);

      final ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);  
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          projectListener[0] = (ProjectChangeListener)invocation.getArgument(0);
          return null;
        }
      }).when(projectCtrlMock).addProjectChangeListener(Mockito.any());
      Mockito.when(projectCtrlMock.getCurrentProjectURL()).thenReturn(project1URL);
      Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);

      ProjectHelper.getInstance().installUpdateProjectOnChangeListener(projectCtrlMock, () -> stagingPanel);
      assertNotNull(projectListener[0]);
      projectListener[0].projectChanged(project1URL, project2URL);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project2File.getParentFile().getAbsolutePath()));
    } finally {
      project1File.getParentFile().delete();
      project2File.getParentFile().delete();
    }
  }

  /**
   * <p><b>Description:</b> Tests the strategy when the user agreement is needed to update the current repository.</p>
   * <p><b>Bug ID:</b> EXM-47264</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testAskOnSwitch() throws Exception {
    final String project1Name = "Project1.xpr";
    final String project2Name = "Project2.xpr";
    final File project1File = new File(LOCAL_REPO, project1Name);
    final File project2File = new File(LOCAL_REPO2, project2Name);
    assertTrue(project1File.createNewFile());
    assertTrue(project2File.createNewFile());
    final URL project1URL = project1File.toURI().toURL();
    final URL project2URL = project2File.toURI().toURL();

    try {
      gitAccess.setRepositorySynchronously(LOCAL_REPO);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project1File.getParentFile().getAbsolutePath()));
      final StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
      final ProjectChangeListener projectListener[] = new ProjectChangeListener[1];

      OptionsManager.getInstance().setWhenRepoDetectedInProject(WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC);

      final ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);  
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          projectListener[0] = (ProjectChangeListener)invocation.getArgument(0);
          return null;
        }
      }).when(projectCtrlMock).addProjectChangeListener(Mockito.any());
      Mockito.when(projectCtrlMock.getCurrentProjectURL()).thenReturn(project1URL);
      Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
      Mockito.when(pluginWSMock.showConfirmDialog( Mockito.anyString(), 
          Mockito.anyString(), Mockito.any(String[].class),  Mockito.any(int[].class))).thenReturn(0); // answer to update the repository
      ProjectHelper.getInstance().installUpdateProjectOnChangeListener(projectCtrlMock, () -> stagingPanel);
      assertNotNull(projectListener[0]);
      projectListener[0].projectChanged(project1URL, project2URL);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project2File.getParentFile().getAbsolutePath()));
      Mockito.when(pluginWSMock.showConfirmDialog( Mockito.anyString(), 
          Mockito.anyString(), Mockito.any(String[].class),  Mockito.any(int[].class))).thenReturn(1); // answer to doesn't update the repository
      projectListener[0].projectChanged(project2URL, project1URL);
      assertEquals(project2File.getParentFile().getAbsolutePath(), gitAccess.getWorkingCopy().getAbsolutePath());
    } finally {
      project1File.getParentFile().delete();
      project2File.getParentFile().delete();
    }
  }
  
  /**
   * <p><b>Description:</b> Tests the strategy when there is noting to do on project update.</p>
   * <p><b>Bug ID:</b> EXM-47264</p>
   *
   * @author Alex_Smarandache
   *
   * @throws Exception
   */ 
  public void testDoNothingOnSwitch() throws Exception {
    final String project1Name = "Project1.xpr";
    final String project2Name = "Project2.xpr";
    final File project1File = new File(LOCAL_REPO, project1Name);
    final File project2File = new File(LOCAL_REPO2, project2Name);
    assertTrue(project1File.createNewFile());
    assertTrue(project2File.createNewFile());
    final URL project1URL = project1File.toURI().toURL();
    final URL project2URL = project2File.toURI().toURL();

    try {
      gitAccess.setRepositorySynchronously(LOCAL_REPO);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project1File.getParentFile().getAbsolutePath()));
      final StandalonePluginWorkspace pluginWSMock = Mockito.mock(StandalonePluginWorkspace.class);
      final ProjectChangeListener projectListener[] = new ProjectChangeListener[1];

      OptionsManager.getInstance().setWhenRepoDetectedInProject(WhenRepoDetectedInProject.DO_NOTHING);

      final ProjectController projectCtrlMock = Mockito.mock(ProjectController.class);  
      Mockito.doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          projectListener[0] = (ProjectChangeListener)invocation.getArgument(0);
          return null;
        }
      }).when(projectCtrlMock).addProjectChangeListener(Mockito.any());
      Mockito.when(projectCtrlMock.getCurrentProjectURL()).thenReturn(project1URL);
      Mockito.when(pluginWSMock.getProjectManager()).thenReturn(projectCtrlMock);
      Mockito.when(pluginWSMock.showConfirmDialog( Mockito.anyString(), 
          Mockito.anyString(), Mockito.any(String[].class),  Mockito.any(int[].class))).thenReturn(0); // No dialog should be displayed but check this by return the ok answer
      ProjectHelper.getInstance().installUpdateProjectOnChangeListener(projectCtrlMock, () -> stagingPanel);
      assertNotNull(projectListener[0]);
      projectListener[0].projectChanged(project1URL, project2URL);
      Awaitility.await().atMost(Duration.ONE_SECOND).until(() -> gitAccess.getWorkingCopy().getAbsolutePath().equals(project1File.getParentFile().getAbsolutePath()));
      Mockito.when(pluginWSMock.showConfirmDialog( Mockito.anyString(), 
          Mockito.anyString(), Mockito.any(String[].class),  Mockito.any(int[].class))).thenReturn(1); // Make a test for cancel answer too
      projectListener[0].projectChanged(project2URL, project1URL);
      assertEquals(project1File.getParentFile().getAbsolutePath(), gitAccess.getWorkingCopy().getAbsolutePath());
    } finally {
      project1File.getParentFile().delete();
      project2File.getParentFile().delete();
    }
  }
  
}
