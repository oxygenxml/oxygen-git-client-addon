package com.oxygenxml.git;

import java.io.File;
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

import org.eclipse.jgit.annotations.Nullable;
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
   * The current project xpr file.
   */
  private File lastProjectXPRFile = null;
  
  /**
   * The git access instance.
   */
  private final GitAccess gitAccess;
  
  /**
   * <code>true</code> only if the repository was changed in the last Oxygen project switching detection and the method 
   * is called for the first time after that.
   */
  private AtomicBoolean wasRepoJustChanged = new AtomicBoolean(false);

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
   */
  public boolean tryToSwitchToRepo(final File repoDir, final JComboBox<String> wcComboBox) {
    final WhenRepoDetectedInProject whatToDo = OptionsManager.getInstance().getWhenRepoDetectedInProject();
    boolean repoChanged = false;
    if(whatToDo != WhenRepoDetectedInProject.DO_NOTHING) {
      try {
        File currentRepo = null;
        if (gitAccess.isRepoInitialized()) {
          currentRepo = gitAccess.getRepository().getDirectory().getParentFile();
        }
        if (currentRepo == null || !FileUtil.same(currentRepo, repoDir)) {
          if (wcComboBox.isPopupVisible()) {
            wcComboBox.setPopupVisible(false);
          }
          
          String projectDirPath = FileUtil.getCanonicalPath(repoDir);
          if (whatToDo == WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC) {
            repoChanged = switchToProjectRepoIfUserAgrees(repoDir);
          } else if (whatToDo == WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC) {
            GitAccess.getInstance().setRepositoryAsync(projectDirPath);
            repoChanged = true;
          }
        }
      } catch (NoRepositorySelected e) {
        LOGGER.warn(e.getMessage(), e);
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
    final PluginWorkspace pluginWS =
        PluginWorkspaceProvider.getPluginWorkspace();
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
                optionsManager.saveDestinationPath(new File(pm.getCurrentProjectURL().toURI()).getParent());
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
  public boolean wasRepoJustChanged() {
    return wasRepoJustChanged.getAndSet(false);
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
   */
  private boolean loadRepositoryFromOxygenProject(final StagingPanel stagingPanel, final URL newProjectURL) {
    boolean repoChanged = false;
    if(stagingPanel != null) {
      final File projectFile = PluginWorkspaceProvider.getPluginWorkspace().getUtilAccess().locateFile(newProjectURL);
      if(projectFile != null) {
        String projectDir = projectFile.getParent();
        if (!projectDir.equals(lastOpenedProject)) {
          File detectedRepo = RepoUtil.detectRepositoryInProject(projectFile);
          repoChanged = detectedRepo == null ? createNewRepoIfUserAgrees(projectDir,  projectFile.getName()) :
            tryToSwitchToRepo(detectedRepo, stagingPanel.getWorkingCopySelectionPanel().getWorkingCopyCombo());
          lastProjectXPRFile = null;
        }
        lastOpenedProject = projectDir;      
      } 
    }
    
    wasRepoJustChanged.set(repoChanged);
    return repoChanged;
  }
  
  /**
   * @return <code>true</code> if a project was loaded, or <code>false</code> if the application is after a restart.
   */
  public boolean wasProjectLoaded() {
    return lastOpenedProject != null;
  }
  
  /**
   * @return The first xpr file found in the current project from Git Staging.
   */
  @Nullable public File findXPRFromCurrentGitProject() {
    if(lastProjectXPRFile == null) {
      try {
        File currentRepo = null;
        if (GitAccess.getInstance().isRepoInitialized()) {
          currentRepo = GitAccess.getInstance().getRepository().getDirectory().getParentFile();
        }
        if(currentRepo != null) {
          lastProjectXPRFile = FileUtil.searchFileByExtension(currentRepo, ".xpr");
        }
      } catch(NoRepositorySelected e) {
        if(LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }
      }
    }

    return lastProjectXPRFile;
  }
  
  /**
   * Installs a listener to automatically detect the Oxygen projects switching and update the current repository.
   * <br>
   * The strategy is get by the com.oxygenxml.git.options.OptionsManager.getWhenRepoDetectedInProject() method.
   * 
   * @param projectCtrl             The project controller to install the listener.
   * @param stagingPanelSupplier    A supplier for the staging panel to use to switch project.
   */
  public void installUpdateProjectOnChangeListener(final ProjectController projectCtrl, final Supplier<StagingPanel> stagingPanelSupplier) {
    projectCtrl.addProjectChangeListener(
        (oldProjectURL, newProjectURL) -> loadRepositoryFromOxygenProject(stagingPanelSupplier.get(), newProjectURL));
  }
  
  /**
   * Reset fields.
   */
  @TestOnly
  public void reset() {
    lastOpenedProject = null;
    lastProjectXPRFile = null;
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
   * @return <code>1</code> if the repository was loaded, 
   * <code>0</code> if the user refuse the project loading, 
   * <code>-1</code> if there was no project file to be loaded found.
   */
  public int openOxygenProjectFromLoadedRepository(final File repositoryDir) {
    int toReturn = 0;
    List<File> xprFiles = FileUtil.findAllFilesByExtension(repositoryDir, ".xpr");
    if(xprFiles.isEmpty()) {
      toReturn = -1;
    } else {
      URL xprUrl = getXprURLfromXprFiles(xprFiles);
      if (xprUrl != null) {
        final StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
        pluginWS.getProjectManager().loadProject(pluginWS.getUtilAccess().locateFile(xprUrl));
        toReturn = 1;
      }
    }
    return toReturn;
  }
 
  /**
   * Get the project file URL from a list of project files.
   * A dialog will be displayed for choosing one project if there are multiple files
   *  
   * @param xprFiles The project files 
   * 
   * @return A URL for the project or <code>null</code> if the URL is malformed or xprFiles is empty
   */
  private URL getXprURLfromXprFiles(final List<File> xprFiles) {
    URL xprUrl = null;
    URI currentXprURI = getCurrentXprURI();
    try {
      if(!xprFiles.isEmpty() && (currentXprURI == null || !xprFiles.contains(new File(currentXprURI)))) {
        if (xprFiles.size() == 1) {
          xprUrl = xprFiles.get(0).toURI().toURL();
        } else {
          OpenProjectDialog dialog= new OpenProjectDialog(
              (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
              xprFiles);
          dialog.setVisible(true);
          if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
            xprUrl = dialog.getSelectedFile().toURI().toURL();
          } 
        }
      }
    } catch (MalformedURLException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
    return xprUrl;
  }
  
  /**
   * @return The uri of the current project(xpr) or <code>null</code>
   */
  private URI getCurrentXprURI(){
    URI currentXPRuri = null;
    StandalonePluginWorkspace spw = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    try {
      Optional<URL> projectURLOpt = Optional.ofNullable(spw.getProjectManager().getCurrentProjectURL());
      currentXPRuri = projectURLOpt.isPresent() && FileUtil.isURLForLocalFile(projectURLOpt.get()) ? projectURLOpt.get().toURI() : null;
    } catch (URISyntaxException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(),e);
      }
    }
    return currentXPRuri;
  }
}
