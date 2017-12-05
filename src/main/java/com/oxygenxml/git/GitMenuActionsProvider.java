package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.event.StageController;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Provides Git-specific actions.
 * 
 * @author sorin_carbunaru
 */
public class GitMenuActionsProvider {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitMenuActionsProvider.class.getName());
  /**
   * Plug-in workspace access.
   */
  private StandalonePluginWorkspace pluginWorkspaceAccess;
  /**
   * Translator.
   */
  private Translator translator;
  /**
   * Stage controller.
   */
  private StageController stageCtrl;

  /**
   * The "Commit" action.
   */
  private AbstractAction commitAction;
  
  /**
   * The Git diff action. Compares local and remote versions of the file.
   */
  private AbstractAction gitDiffAction;

  /**
   * Constructor.
   * 
   * @param translator
   *          Translator used for i18n.
   * @param pluginWorkspaceAccess
   *          Plug-in workspace access.
   * @param stageCtrl
   *          The staging panel.
   */
  public GitMenuActionsProvider(StandalonePluginWorkspace pluginWorkspaceAccess, Translator translator,
      StageController stageCtrl) {
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    this.translator = translator;
    this.stageCtrl = stageCtrl;
  }

  /**
   * Get the Git-specific actions for the current selection from the Project view.
   */
  public List<AbstractAction> getActionsForProjectViewSelection() {
    List<AbstractAction> actions = new ArrayList<AbstractAction>();
    
    // Create the Git actions, if not already created
    if (commitAction == null) {
      createCommitAction();
    }
    if (gitDiffAction == null) {
      createGitDiffAction();
    }
    
    // Enable/disable
    commitAction.setEnabled(true);
    gitDiffAction.setEnabled(shouldEnableGitDiffAction());
    
    // Add the Git actions to the list
    actions.add(commitAction);
    actions.add(gitDiffAction);

    return actions;
  }

  /**
   * Create the "Git Diff" action.
   */
  private void createGitDiffAction() {
    gitDiffAction = new AbstractAction(translator.getTranslation(Tags.PROJECT_VIEW_GIT_DIFF_CONTEXTUAL_MENU_ITEM)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
        // The diff action is enabled only for one file
        String repository = getRepositoryForFiles(selectedFiles);
        if (repository != null) {
          try {
            String previousRepository = OptionsManager.getInstance().getSelectedRepository();
            if (!repository.equals(previousRepository)) {
              GitAccess.getInstance().setRepository(repository);
            }
            
            List<FileStatus> gitFiles = new ArrayList<FileStatus>();
            gitFiles.addAll(GitAccess.getInstance().getUnstagedFiles());
            gitFiles.addAll(GitAccess.getInstance().getStagedFile());
            
            String selectedFilePath = selectedFiles[0].getAbsolutePath().replace("\\", "/");
            for (FileStatus fileStatus : gitFiles) {
              if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
                DiffPresenter diff = new DiffPresenter(fileStatus, stageCtrl, translator);
                diff.showDiff();
                break;
              }
            }
          } catch (Exception e1) {
            if (logger.isDebugEnabled()) {
              logger.debug(e1, e1);
            }
          }
        }
      }
    };
  }

  /**
   * Create the "Commit" action.
   */
  private void createCommitAction() {
    commitAction = new AbstractAction(translator.getTranslation(Tags.PROJECT_VIEW_COMMIT_CONTEXTUAL_MENU_ITEM)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        pluginWorkspaceAccess.showView(OxygenGitPluginExtension.GIT_STAGING_VIEW, true);
        
        // Use the repository from the project view
        File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
        String repository = getRepositoryForFiles(selectedFiles);
        if (repository != null) {
          try {
            String previousRepository = OptionsManager.getInstance().getSelectedRepository();
            if (!repository.equals(previousRepository)) {
              GitAccess.getInstance().setRepository(repository);
            }
            
            stageFiles(repository);
          } catch (IOException e1) {
            if (logger.isDebugEnabled()) {
              logger.debug(e1, e1);
            }
          }
        }
      }
    };
  }
  
  /**
   * Stage files.
   * 
   * @param repository The current repository.
   * 
   * @return <code>true</code> if we have staged files.
   */
  private void stageFiles(String repository) {
    repository = repository.replace("\\", "/");
    List<FileStatus> unstagedFiles = GitAccess.getInstance().getUnstagedFiles();
    Set<String> allSelectedFiles = ProjectViewManager.getSelectedFilesDeep(pluginWorkspaceAccess);
    for (FileStatus unstagedFileStatus : unstagedFiles) {
      if (allSelectedFiles.contains(repository + "/" + unstagedFileStatus.getFileLocation())
          && unstagedFileStatus.getChangeType() != GitChangeType.CONFLICT) {
        GitAccess.getInstance().add(unstagedFileStatus);
      }
    }
  }
  
  /**
   * Check if the Git diff action is enabled.
   * 
   * @return <code>true</code> if the action is enabled.
   */
  private boolean shouldEnableGitDiffAction() {
    boolean shouldEnable = true;
    File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
    if (selectedFiles != null) {
      if (selectedFiles.length > 1 || selectedFiles.length == 1 && selectedFiles[0].isDirectory()) {
        // disable the diff action if there are 2 or more files selected or if
        // the files selected is a directory
        shouldEnable = false;
      } else if (selectedFiles.length == 1) {
        String repository = getRepositoryForFiles(selectedFiles);
        if (repository != null) {
          shouldEnable = isGitDiffEnabledBasedOnSelectedFileStatus(selectedFiles[0], repository);
        }
      }
    }
    return shouldEnable;
  }

  /**
   * Check if "Git diff" is enabled, based on the status of the selected files.
   * 
   * @param selectedFile  The selected file.
   * @param repository    The current repository.
   * 
   * @return <code>true</code> if "Git diff" should be enabled.
   */
  private boolean isGitDiffEnabledBasedOnSelectedFileStatus(File selectedFile, String repository) {
    boolean isEnabled = true;
    try {
      // Use the repository from the project view
      String previousRepository = OptionsManager.getInstance().getSelectedRepository();
      if (!repository.equals(previousRepository)) {
        GitAccess.getInstance().setRepository(repository);
      }

      List<FileStatus> gitFiles = new ArrayList<FileStatus>();
      gitFiles.addAll(GitAccess.getInstance().getUnstagedFiles());
      gitFiles.addAll(GitAccess.getInstance().getStagedFile());

      FileStatus selectedFileStatus = null;
      String selectedFilePath = selectedFile.getAbsolutePath().replace("\\", "/");
      for (FileStatus gitFileStatus : gitFiles) {
        if (selectedFilePath.endsWith(gitFileStatus.getFileLocation())) {
          selectedFileStatus = new FileStatus(gitFileStatus);
          break;
        }
      }

      if (selectedFileStatus == null
          || selectedFileStatus.getChangeType() == GitChangeType.ADD
          || selectedFileStatus.getChangeType() == GitChangeType.UNTRACKED
          || selectedFileStatus.getChangeType() == GitChangeType.MISSING
          || selectedFileStatus.getChangeType() == GitChangeType.REMOVED) {

        isEnabled = false;
      }
    } catch (IOException e) {
      isEnabled = false;
      logger.error(e, e);
    }
    return isEnabled;
  }

  /**
   * Get the repository corresponding to the given files.
   * 
   * @param files The files.
   * 
   * @return the repository, or <code>null</code> if couldn't be detected.
   */
  private String getRepositoryForFiles(File[] files) {
    String repository = null;
    // Search for first file. In oXygen all files from the Project view
    // are in the same project/repository.
    File file = new File(files[0].getAbsolutePath());
    while (repository == null && file.getParent() != null) {
      if (FileHelper.isGitRepository(file.getPath())) {
        repository = file.getAbsolutePath();
      }
      file = file.getParentFile();
    }
    return repository;
  }

}
