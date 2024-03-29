package com.oxygenxml.git;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.swing.JComboBox;
import javax.swing.JFrame;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.staging.OpenProjectDialog;
import com.oxygenxml.git.view.staging.StagingPanel;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.util.UtilAccess;

/**
 * Contains methods for projects processing.
 *  
 * @author alex_smarandache
 *
 */
public class ProjectHelper {
  
  /**
   * Translation support.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectHelper.class);
  
  /**
   * The last opened project in the Project side-view.
   */
  private String lastOpenedProject;
  
  /**
   * The git access instance.
   */
  private final GitAccess gitAccess;
  
  /**
   * <code>true</code> only if the repository was changed in the last Oxygen project switching detection and the method 
   * is called for the first time after that.
   */
  private AtomicBoolean wasRepoChangedInLastProjectSwitch = new AtomicBoolean(false);
  
  /**
   * <code>true</code> if we already treat a project change event.
   */
  private boolean isProjectChangeEventBeingTreated;
  
  /**
   * The status of loading project operation.
   * 
   * @author alex_smarandache
   */
  public enum LoadProjectOperationStatus {
    /**
     * The project was loaded with success.
     */
    SUCCESS,
    /**
     * The project loading was stopped by the user.
     */
    CANCELED_BY_USER,
    /**
     * The project was not found so it doesn't be loaded.
     */
    PROJECT_NOT_FOUND
  }
  
  /**
   * Hidden constructor.
   */
  private ProjectHelper() {
    gitAccess = GitAccess.getInstance();
  }

  /**
   * Helper class to manage the singleton instance.
   *
   * @author Alex_Smarandache
   */
  private static class SingletonHelper {
    static final ProjectHelper INSTANCE = new ProjectHelper();
  }

  /**
   * Get the unique instance.
   *
   * @return The instance.
   */
  public static ProjectHelper getInstance() {
    return SingletonHelper.INSTANCE;
  }
  
  /**
   * Try to switch to repository, if the user will agree.
   * 
   * @param repoDir    Repository directory.
   * @param wcComboBox The working copy combo box.
   * 
   * @return <code>true</code> if repository was changed.
   * 
   * @throws IOException if the repository to set could not be accessed.
   */
  private boolean tryToSwitchToRepo(final File repoDir, final JComboBox<String> wcComboBox) throws IOException {
    final WhenRepoDetectedInProject whatToDo = OptionsManager.getInstance().getWhenRepoDetectedInProject();
    boolean repoChanged = false;
    if(whatToDo != WhenRepoDetectedInProject.DO_NOTHING) {
      File currentRepo = null;
      if (gitAccess.isRepoInitialized()) {
        try {
          currentRepo = gitAccess.getRepository().getDirectory().getParentFile();
        } catch (NoRepositorySelected e) {
          LOGGER.warn("Could not get the current repository.", e);
        }
      }
      if (currentRepo == null || !FileUtil.same(currentRepo, repoDir)) {
        if (wcComboBox.isPopupVisible()) {
          wcComboBox.setPopupVisible(false);
        }

        String projectDirPath = FileUtil.getCanonicalPath(repoDir);
        if (whatToDo == WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC) {
          repoChanged = switchToProjectRepoIfUserAgrees(repoDir);
        } else if (whatToDo == WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC) {
          GitAccess.getInstance().setRepositorySynchronously(projectDirPath);
          repoChanged = true;
        }
      }
    }
    
    return repoChanged;
  }
  
  /**
   * Switch to the given repository if the user agrees.
   * 
   * @param repositoryDir  The repository directory.
   * 
   * @return <code>true</code> if repository changed.
   */
  private boolean switchToProjectRepoIfUserAgrees(File repositoryDir) {
    boolean repoChanged = false;
    PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
    int response = pluginWS.showConfirmDialog(
        TRANSLATOR.getTranslation(Tags.DETECTED_LOCAL_GIT_REPO),
        new StringBuilder(MessageFormat.format(TRANSLATOR.getTranslation(Tags.CHANGE_TO_PROJECT_REPO_CONFIRM_MESSAGE), repositoryDir.getAbsolutePath()))
          .append("\n\n").append(MessageFormat.format(TRANSLATOR.getTranslation(Tags.ASK_LOAD_REPOSITORY), repositoryDir.getName())).toString(),
        new String[] {
            TRANSLATOR.getTranslation(Tags.LOAD),
            TRANSLATOR.getTranslation(Tags.DONT_LOAD)
        },
        new int[] { 0, 1 });
    if (response == 0) {
      GitAccess.getInstance().setRepositoryAsync(FileUtil.getCanonicalPath(repositoryDir));
      repoChanged = true;
    }

    return repoChanged;
  }
  
  
  /**
   * Create a new repository if the user agrees.
   * 
   * @param projectDir   Project directory.
   * @param projectName  Project name.
   * 
   * @return <code>true</code> if repository changed.
   */
  private boolean createNewRepoIfUserAgrees(String projectDir, String projectName) {
    boolean repoChanged = false;
    // Fast check to see if this is actually not a Git repository.
    final OptionsManager optionsManager = OptionsManager.getInstance();
    if (optionsManager.getAskUserToCreateNewRepoIfNotExist() && !optionsManager.getProjectsTestedForGit().contains(projectDir)) {
      StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
      int response = pluginWS.showConfirmDialog(
          TRANSLATOR.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT_TITLE),
          MessageFormat.format(TRANSLATOR.getTranslation(Tags.CHECK_PROJECTXPR_IS_GIT), projectName),
          new String[] {
              "   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
              "   " + TRANSLATOR.getTranslation(Tags.NO) + "   "
          },
          new int[] { 0, 1 });
      if (response == 0) {
        try {
          gitAccess.createNewRepository(projectDir);
          
          // save destination path in options if no one were saved.
          if(!optionsManager.getDestinationPaths().isEmpty()) {
            final Optional<ProjectController> projectManager = Optional.ofNullable(pluginWS.getProjectManager());
            projectManager.ifPresent(pm -> {
              try {
                optionsManager.saveDestinationPath(new File(pm.getCurrentProjectURL().toURI()).getParent()); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
              } catch (URISyntaxException e) {
                LOGGER.error(e.getMessage(), e);
              }
            });
          }
          
          repoChanged = true;
        } catch (IllegalStateException | GitAPIException e) {
          LOGGER.debug(e.getMessage(),  e);
          pluginWS.showErrorMessage("Failed to create a new repository.", e);
        }
      }

      // Don't ask the user again.
      OptionsManager.getInstance().saveProjectTestedForGit(projectDir);
    }

    return repoChanged;
  }
  
  /**
   * @return <code>true</code> only if the repository was changed in the last Oxygen project switching detection and the method 
   * is called for the first time after that.
   */
  public boolean wasRepoChangedInLastProjectSwitch() {
    return wasRepoChangedInLastProjectSwitch.getAndSet(false);
  }
  
  /**
   * Checks the current loaded project and:
   * <br>
   * 1. load it if it contains a Git project and the Oxygen > Git preferences allow it.
   * <br>
   * 2. create a new Git repo if the project doesn't contains a Git project and the user agrees.
   * <br>
   * 
   * @param stagingPanel  The Staging panel view.
   * @param newProjectURL The new URL of the current opened project.
   * 
   * @return <code>true</code> if the repository changed.
   * 
   * @throws IOException if the repository to set could not be accessed.
   */
  private boolean loadRepositoryFromOxygenProject(final StagingPanel stagingPanel, final URL newProjectURL) 
      throws IOException {
    boolean repoChanged = false;
    if(stagingPanel != null) {
      UtilAccess utilAccess = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess();
      final File projectFile = utilAccess.locateFile(newProjectURL);
      if(projectFile != null) {
        String projectDir = projectFile.getParent();
        boolean wasNewProjectLoaded = 
            wasProjectLoaded() // EXM-52788: ignore first project loading after launching Oxygen.
                && !projectDir.equals(lastOpenedProject);
        if (wasNewProjectLoaded) {
          File repoInProjectDir = RepoUtil.detectRepositoryInProject(projectFile);
          repoChanged = repoInProjectDir == null ? createNewRepoIfUserAgrees(projectDir, projectFile.getName())
            : tryToSwitchToRepo(repoInProjectDir, stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo());
        } 
        lastOpenedProject = projectDir;      
      } 
    }
    
    wasRepoChangedInLastProjectSwitch .set(repoChanged);
    return repoChanged;
  }
  
  /**
   * @return <code>true</code> if a project was loaded, or <code>false</code> if the application is after a restart.
   */
  public boolean wasProjectLoaded() {
    return lastOpenedProject != null;
  }
  
  /**
   * Installs a listener to automatically detect the Oxygen projects switching and update the current repository.
   * <br>
   * The strategy is get by the com.oxygenxml.git.options.OptionsManager.getWhenRepoDetectedInProject() method.
   * 
   * @param projectCtrl             The project controller to install the listener.
   * @param stagingPanelSupplier    A supplier for the staging panel to use to switch project.
   */
  public void installProjectChangeListener(
      final ProjectController projectCtrl,
      final Supplier<StagingPanel> stagingPanelSupplier) {
    projectCtrl.addProjectChangeListener(
        (oldProjectURL, newProjectURL) -> {
          isProjectChangeEventBeingTreated = true;
          try {
            loadRepositoryFromOxygenProject(stagingPanelSupplier.get(), newProjectURL);
          } catch (IOException e) {
            PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(
                TRANSLATOR.getTranslation(Tags.COULD_NO_LOAD_REPO_CORRESPONDING_TO_PROJECT),
                e);
          }
          isProjectChangeEventBeingTreated = false;
        });
  }
  
  /**
   * <code>true</code> if a project change event is being treated.
   */
  public boolean isProjectChangeEventBeingTreated() {
    return isProjectChangeEventBeingTreated;
  }
  
  /**
   * Reset fields.
   */
  @TestOnly
  public void reset() {
    lastOpenedProject = null;
  }
  
  /**
   * This method will open an Oxygen project file(.xpr) located inside the current repository directory.
   * <br>
   * If a single project file is found, this will be loaded.
   * <br>
   * If multiple project files are found, the user can select the desired file.
   *  
   * @param repositoryDir The current repository main directory.
   * 
   * @return The status of loading project operation.
   * 
   * @throws MalformedURLException When the URL of the chosen project file is not valid.
   */
  public LoadProjectOperationStatus openOxygenProjectFromLoadedRepository(final File repositoryDir) throws MalformedURLException {
    LoadProjectOperationStatus toReturn = LoadProjectOperationStatus.CANCELED_BY_USER;
    List<File> xprFiles = FileUtil.findAllFilesByExtension(repositoryDir, ".xpr");
    if(xprFiles.isEmpty()) {
      toReturn = LoadProjectOperationStatus.PROJECT_NOT_FOUND;
    } else {
      Optional<URI> projectToLoad = getProjectToLoad(xprFiles);
      if (projectToLoad.isPresent()) {
        StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
        UtilAccess utilAccess = pluginWS.getUtilAccess();
        File projectFileToLoad = utilAccess.locateFile(projectToLoad.get().toURL());
        pluginWS.getProjectManager().loadProject(projectFileToLoad);
        toReturn = LoadProjectOperationStatus.SUCCESS;
      }
    }
    return toReturn;
  }
 
  /**
   * If one project is found, this project will be automatically chosen.<br>
   * If there are multiple project files, a dialog will be displayed for choosing one of them.
   *  
   * @param xprFiles The project files 
   * 
   * @return An optional project URL.
   */ 
  private Optional<URI> getProjectToLoad(final List<File> xprFiles) {
    Optional<URI> xprUri = Optional.empty();
    
    StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    UtilAccess utilAccess = pluginWS.getUtilAccess();
    Optional<URL> projectURLOpt = Optional.ofNullable(pluginWS.getProjectManager().getCurrentProjectURL());
    URL currentXprURL = projectURLOpt.isPresent() && FileUtil.isURLForLocalFile(projectURLOpt.get()) 
        ? projectURLOpt.get() : null;
    if(!xprFiles.isEmpty() 
        && (currentXprURL == null
            || !xprFiles.contains(utilAccess.locateFile(currentXprURL)))) { // NOSONAR findsecbugs:PATH_TRAVERSAL_IN
      if (xprFiles.size() == 1) {
        xprUri = Optional.of(xprFiles.get(0).toURI());
      } else {
        OpenProjectDialog dialog= new OpenProjectDialog(
            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
            xprFiles);
        dialog.setVisible(true);
        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
          xprUri = Optional.of(dialog.getSelectedFile().toURI());
        } 
      }
    }
    
    return xprUri;
  }
  
}
