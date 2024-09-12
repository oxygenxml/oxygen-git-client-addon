package com.oxygenxml.git.view;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.mockito.Mockito;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitTestBase;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.staging.OpenProjectDialog;
import com.oxygenxml.git.view.staging.WorkingCopySelectionPanel;

import ro.sync.basic.io.FileSystemUtil;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Contains tests for xpr files detection.
 * 
 * @author alex_smarandache
 *
 */
public class WorkingCopyXprDetectionTest extends GitTestBase {

  /**
   * <p><b>Description:</b> Tests auto opening an .xpr file project when option is enabled and disabled.</p>
   * 
   * <p><b>Bug ID:</b> EXM-46694</p>
   *
   * @author alex_smarandache
   * @author gabriel_nedianu
   * 
   * @throws Exception When problems occur.
   */
  public void testAutoOpenXPR() throws Exception {
    OptionsManager.getInstance().setDetectAndOpenXprFiles(true);

    final String localRepository1 = "localrepo1"; 
    final String[] currentProject = new String[1];
    final String localRepository2 = "localrepo2";
    final String dir = "target/test-resources/WorkingCopyXprDetection";

    final StandalonePluginWorkspace pluginWS = Mockito.mock(StandalonePluginWorkspace.class);

    final UtilAccess utilAccessMock = Mockito.mock(UtilAccess.class);
    Mockito.when(utilAccessMock.locateFile(Mockito.any(URL.class))).then(args -> new File(((URL)args.getArgument(0)).toURI()));
    Mockito.when(pluginWS.getUtilAccess()).thenReturn(utilAccessMock);
    
    final JFrame frame = new JFrame();
    Mockito.when(pluginWS.getParentFrame()).thenReturn(frame);

    final List<File> files = new ArrayList<>();
    createResorces(dir, localRepository1, 1, localRepository2, 3, files);
    assertEquals(7, files.size());

    final int mainDirectoryIndex = 0;
    final int localRepo2Index = 2;
    final int xpr1Index = 3;
    final int xpr2Index = 4;

    final ProjectController projectManager = Mockito.mock(ProjectController.class);
    Mockito.when(projectManager.getCurrentProjectURL()).thenReturn(files.get(localRepo2Index).toURI().toURL());
    Mockito.when(pluginWS.getProjectManager()).thenReturn(projectManager);
    Mockito.doAnswer(invocation -> {
      currentProject[0] = ((File)invocation.getArgument(0)).getPath();
      return null;
    }).when(projectManager).loadProject(Mockito.any(File.class));
    PluginWorkspaceProvider.setPluginWorkspace(pluginWS);

    try {
      GitAccess instance = GitAccess.getInstance();
      WorkingCopySelectionPanel wcPanel = new WorkingCopySelectionPanel(new GitController(instance), true);
      frame.getContentPane().add(wcPanel);
      frame.pack();

      SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
      sleep(100);
      
      assertNull(wcPanel.getWorkingCopyCombo().getToolTipText());

      File repository1 = new File(dir, localRepository1);
      instance.createNewRepository(repository1.getPath());
      Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() ->
      files.get(xpr1Index).getAbsolutePath().equals(currentProject[0]));

      File repository2 = new File(dir, localRepository2);
      instance.createNewRepository(repository2.getPath());
      OpenProjectDialog dialog = (OpenProjectDialog) findDialog(
          Translator.getInstance().getTranslation(Tags.DETECT_AND_OPEN_XPR_FILES_DIALOG_TITLE));
      assertNotNull(dialog);
      assertEquals(3, dialog.getFilesCombo().getItemCount());
      
      // assert that the repository was not changed without confirmation.
      Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() ->
      files.get(xpr1Index).getAbsolutePath().equals(currentProject[0]));
      
      dialog.getOkButton().doClick();
      Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() ->
      files.get(xpr2Index).getAbsolutePath().equals(currentProject[0]));
      
      //Switching to the first repo shouldn't change the xpr when the option not set
      OptionsManager.getInstance().setDetectAndOpenXprFiles(false);
      wcPanel.getWorkingCopyCombo().setSelectedItem(repository1.getAbsolutePath());
      Awaitility.await().atMost(Duration.FIVE_HUNDRED_MILLISECONDS).until(() ->
      files.get(xpr2Index).getAbsolutePath().equals(currentProject[0]));
      assertEquals("First repo should be set in the working copy combo",
          repository1.getAbsolutePath(),
          wcPanel.getWorkingCopyCombo().getSelectedItem());
      assertEquals("The tooltip must be also updated", 
          repository1.getAbsolutePath(), wcPanel.getWorkingCopyCombo().getToolTipText());
      
      //Switching to the second repo shouldn't open the dialog
      wcPanel.getWorkingCopyCombo().setSelectedItem(repository2.getAbsolutePath());
      dialog = (OpenProjectDialog) findDialog(
          Translator.getInstance().getTranslation(Tags.DETECT_AND_OPEN_XPR_FILES_DIALOG_TITLE));
      assertNull("Switching to the second repo shouldn't open the dialog", dialog);
      assertEquals("Second repo should be set in the working copy combo",
          repository2.getAbsolutePath(),
          wcPanel.getWorkingCopyCombo().getSelectedItem());
      assertEquals("The tooltip must be also updated", 
          repository2.getAbsolutePath(), wcPanel.getWorkingCopyCombo().getToolTipText());
      
    } finally {
      FileSystemUtil.deleteRecursivelly(files.get(mainDirectoryIndex));
      frame.setVisible(false);
      frame.dispose();
      OptionsManager.getInstance().setDetectAndOpenXprFiles(false);
    }
  }

  /**
   * <p><b>Description:</b> load project on repo change.</p>
   * <p><b>Bug ID:</b> EXM-53504</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testLoadProjectOnRepoChange() throws Exception {
    final GitEventInfo[] wcChangeEventInfo = new GitEventInfo[1];
    
    GitAccess gitAccess = GitAccess.getInstance();
    new WorkingCopySelectionPanel(
        new GitController(gitAccess),
        false) {
      @Override
      protected boolean isProjectChangeEventBeingTreated() {
        return false; // No other project change event being processed
      }
      
      @Override
      protected boolean isDetectAndOpenXprFiles() {
        return true;
      }
      
      @Override
      protected void openOxyProjectFromLoadedRepo(GitEventInfo info) throws MalformedURLException {
        wcChangeEventInfo[0] = info;
      }
    };

    final String dir = "target/test-resources/WorkingCopyXprDetectionTest-loadProjectFromRepo";
    File repo = new File(dir, "repo_1");
    repo.deleteOnExit();
    gitAccess.createNewRepository(repo.getPath());
    
    gitAccess.setRepositorySynchronously(repo.getAbsolutePath());
    
    Thread.sleep(500); // NOSONAR
    
    assertNotNull(wcChangeEventInfo[0]);
    assertTrue(wcChangeEventInfo[0] instanceof WorkingCopyGitEventInfo);
    
    WorkingCopyGitEventInfo eventInfo = (WorkingCopyGitEventInfo) wcChangeEventInfo[0];
    assertEquals(
        repo.getAbsolutePath().replace("\\", "/"),
        eventInfo.getWorkingCopy().getAbsolutePath().replace("\\", "/"));
  }
  
  /**
   * <p><b>Description:</b> don't load project when repo changed if another
   * project change event is being processed.</p>
   * <p><b>Bug ID:</b> EXM-53504</p>
   *
   * @author sorin_carbunaru
   *
   * @throws Exception
   */
  public void testDontLoadProjectOnRepoChangeIfAnotherProjectChangeIsBeingProcessed() throws Exception {
    final GitEventInfo[] wcChangeEventInfo = new GitEventInfo[1];
    
    GitAccess gitAccess = GitAccess.getInstance();
    new WorkingCopySelectionPanel(
        new GitController(gitAccess),
        false) {
      @Override
      protected boolean isProjectChangeEventBeingTreated() {
        return true; // another project change event is being processed
      }
      
      @Override
      protected boolean isDetectAndOpenXprFiles() {
        return true;
      }
      
      @Override
      protected void openOxyProjectFromLoadedRepo(GitEventInfo info) throws MalformedURLException {
        wcChangeEventInfo[0] = info;
      }
    };

    final String dir = "target/test-resources/WorkingCopyXprDetectionTest-dontLoadProjectFromRepo";
    File repo = new File(dir, "repo_1");
    repo.deleteOnExit();
    gitAccess.createNewRepository(repo.getPath());
    
    gitAccess.setRepositorySynchronously(repo.getAbsolutePath());
    
    assertNull(wcChangeEventInfo[0]);
  }
  
  /**
   * Creates testing resources.
   *     
   * @param mainDirectory           The main directory.
   * @param localRepository1        The first local repository folder.
   * @param repo1XprFiles           The first repository number of xpr files
   * @param localRepository2        The second local repository folder.
   * @param repo2XprFiles           The second repository number of xpr files
   * @param files                   The list to collect created files.
   * 
   * @throws URISyntaxException
   * @throws IOException
   */
  private void createResorces(
      final String mainDirectory, 
      final String localRepository1, 
      final int repo1XprFiles,
      final String localRepository2,
      final int repo2XprFiles,
      final List<File> files) throws URISyntaxException, IOException {

    final File mainDir = new File(mainDirectory);
    if(!mainDir.exists()) {
      mainDir.mkdirs();
    }
    files.add(mainDir);

    final File localRepositoryFile1 = new File(mainDir, localRepository1);
    final File localRepositoryFile2 = new File(mainDir, localRepository2);
    localRepositoryFile1.mkdir();
    localRepositoryFile2.mkdir();
    files.add(localRepositoryFile1);
    files.add(localRepositoryFile2);

    IntStream.range(1, repo1XprFiles + 1).forEachOrdered(index -> {
      final File file = new File(localRepositoryFile1, "file" + index + ".xpr");
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      files.add(file);
    });
    
    IntStream.range(1, repo2XprFiles + 1).forEachOrdered(index -> {
      final File file = new File(localRepositoryFile2, "file" + index + ".xpr");
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      files.add(file);
    });

  }

}
