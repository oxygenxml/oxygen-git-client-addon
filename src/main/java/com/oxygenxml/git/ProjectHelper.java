package com.oxygenxml.git;

import java.io.File;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Optional;

import javax.swing.JComboBox;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.OxygenGitOptionPagePluginExtension.WhenRepoDetectedInProject;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.staging.StagingPanel;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.project.ProjectController;

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
    boolean repoChanged = false;
    try {
      File currentRepo = null;
      if (gitAccess.isRepoInitialized()) {
        currentRepo = gitAccess.getRepository().getDirectory().getParentFile();
      }
      if (currentRepo == null || !FileUtil.same(currentRepo, repoDir)) {
        if (wcComboBox.isPopupVisible()) {
          wcComboBox.setPopupVisible(false);
        }

        WhenRepoDetectedInProject whatToDo = OptionsManager.getInstance().getWhenRepoDetectedInProject();
        String projectDirPath = FileUtil.getCanonicalPath(repoDir);
        if (whatToDo == WhenRepoDetectedInProject.ASK_TO_SWITCH_TO_WC) {
          repoChanged = switchToProjectRepoIfUserAgrees(projectDirPath);
        } else if (whatToDo == WhenRepoDetectedInProject.AUTO_SWITCH_TO_WC) {
          GitAccess.getInstance().setRepositoryAsync(projectDirPath);
          repoChanged = true;
        }
      }
    } catch (NoRepositorySelected e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return repoChanged;
  }
  
  /**
   * Switch to the given repository if the user agrees.
   * 
   * @param projectDir  The project directory.
   * 
   * @return <code>true</code> if repository changed.
   */
  private boolean switchToProjectRepoIfUserAgrees(String projectDir) {
    boolean repoChanged = false;
    final PluginWorkspace pluginWS =
        PluginWorkspaceProvider.getPluginWorkspace();
    int response = pluginWS.showConfirmDialog(
        TRANSLATOR.getTranslation(Tags.CHANGE_WORKING_COPY),
        MessageFormat.format(
            TRANSLATOR.getTranslation(Tags.CHANGE_TO_PROJECT_REPO_CONFIRM_MESSAGE),
            projectDir),
        new String[] {
            TRANSLATOR.getTranslation(Tags.CHANGE),
            TRANSLATOR.getTranslation(Tags.KEEP_CURRENT_WC)
        },
        new int[] { 0, 1 });
    if (response == 0) {
      GitAccess.getInstance().setRepositoryAsync(projectDir);
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
    if (!optionsManager.getProjectsTestedForGit().contains(projectDir)) {
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
   * Checks the current loaded project and:
   * 
   * 1. load it if it contains a Git project and the Oxygen > Git preferences allow it.
   * 2. create a new Git repo if the project doesn't contains a Git project and the user agrees.
   * 
   * @return <code>true</code> if the repository changed.
   */
  public boolean loadRepositoryFromOxygenProject(final StagingPanel stagingPanel) {
    boolean repoChanged = false;
    if (stagingPanel != null && stagingPanel.hasFocus()) {
      PluginWorkspace pluginWS = PluginWorkspaceProvider.getPluginWorkspace();
      // Can be null from tests.
      if (pluginWS.getUtilAccess() != null) {
        String projectDir = pluginWS.getUtilAccess().expandEditorVariables("${pd}", null);
        if (projectDir != null && !projectDir.equals(lastOpenedProject)) {
          String projectName = pluginWS.getUtilAccess().expandEditorVariables("${pn}", null) + ".xpr";
          File projectFile = new File(projectDir, projectName); // NOSONAR findsecbugs:PATH_TRAVERSAL_IN - false positive
          File detectedRepo = RepoUtil.detectRepositoryInProject(projectFile);
          if (detectedRepo == null) {
            repoChanged = createNewRepoIfUserAgrees(projectDir, projectName);
          } else {
            repoChanged = tryToSwitchToRepo(detectedRepo, stagingPanel.getWorkingCopySelectionPanel()
                .getWorkingCopyCombo());
          }
          lastProjectXPRFile = null;
        }
        lastOpenedProject = projectDir;
      }      
    }
   
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
 
}
