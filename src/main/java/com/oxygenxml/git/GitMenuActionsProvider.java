package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.event.GitController;

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
  private StandalonePluginWorkspace pluginWS;
  /**
   * Translator.
   */
  private Translator translator = Translator.getInstance();
  /**
   * Stage controller.
   */
  private GitController stageCtrl;

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
  public GitMenuActionsProvider(StandalonePluginWorkspace pluginWorkspaceAccess, GitController stageCtrl) {
    this.pluginWS = pluginWorkspaceAccess;
    this.stageCtrl = stageCtrl;
  }

  /**
   * Get the Git-specific actions for the current selection from the Project view.
   */
  public List<AbstractAction> getActionsForProjectViewSelection() {
    List<AbstractAction> actions = new ArrayList<>();
    
    // Create the Git actions, if not already created
    if (gitDiffAction == null) {
      gitDiffAction = new AbstractAction(translator.getTranslation(Tags.GIT_DIFF)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          GitOperationScheduler.getInstance().schedule(GitMenuActionsProvider.this::doGitDiff);
        }
      };
    }
    if (commitAction == null) {
      commitAction = new AbstractAction(translator.getTranslation(Tags.COMMIT)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          GitOperationScheduler.getInstance().schedule(GitMenuActionsProvider.this::doPrepareCommit);
        } 
      };
    }
  
    
    // Enable/disable
    commitAction.setEnabled(true);
    gitDiffAction.setEnabled(shouldEnableGitDiffAction());
    
    // Add the Git actions to the list
    actions.add(gitDiffAction);
    actions.add(commitAction);

    return actions;
  }

  /**
   * Do Git Diff.
   */
  private void doGitDiff() {
    File selFile = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWS)[0];
    String repository = getRepositoryForFile(selFile);
    if (repository != null) {
      try {
        String previousRepository = OptionsManager.getInstance().getSelectedRepository();
        if (!repository.equals(previousRepository)) {
          GitAccess.getInstance().setRepositorySynchronously(repository);
        }
        
        List<FileStatus> gitFiles = getStagedAndUnstagedFiles();
        boolean wasDiffShown = false;
        if (!gitFiles.isEmpty()) {
          String selectedFilePath = FileHelper.rewriteSeparator(selFile.getAbsolutePath());
          for (FileStatus fileStatus : gitFiles) {
            if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
              SwingUtilities.invokeLater(() -> DiffPresenter.showDiff(fileStatus, stageCtrl));
              wasDiffShown = true;
              break;
            }
          }
        }
        if (!wasDiffShown) {
          SwingUtilities.invokeLater(
              () -> pluginWS.showInformationMessage(translator.getTranslation(Tags.NO_CHANGES)));
        }
      } catch (Exception e1) {
        if (logger.isDebugEnabled()) {
          logger.debug(e1, e1);
        }
      }
    }
  }

  /**
   * Prepare commit.
   */
  private void doPrepareCommit() {
    File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWS);
    String repository = getRepositoryForFile(selectedFiles[0]);
    if (repository != null) {
      try {
        String previousRepository = OptionsManager.getInstance().getSelectedRepository();
        if (!repository.equals(previousRepository)) {
          GitAccess.getInstance().setRepositorySynchronously(repository);
        }
        
        List<FileStatus> gitFiles = getStagedAndUnstagedFiles();
        boolean canCommit = false;
        for (File selFile : selectedFiles) {
          String selectedFilePath = FileHelper.rewriteSeparator(selFile.getAbsolutePath());
          for (FileStatus fileStatus : gitFiles) {
            if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
              canCommit = true;
              break;
            }
          }
        }
        
        if (canCommit) {
          SwingUtilities.invokeLater(
              () -> pluginWS.showView(OxygenGitPluginExtension.GIT_STAGING_VIEW, true));
          stageFiles(repository);
        } else {
          SwingUtilities.invokeLater(
              () -> pluginWS.showInformationMessage(translator.getTranslation(Tags.NOTHING_TO_COMMIT)));
        }
      } catch (IOException e1) {
        if (logger.isDebugEnabled()) {
          logger.debug(e1, e1);
        }
      }
    }
  }
  
  /**
   * @return The staged and the unstaged files.
   */
  private List<FileStatus> getStagedAndUnstagedFiles() {
    List<FileStatus> gitFiles = new ArrayList<>();
    GitStatus status = GitAccess.getInstance().getStatus();
    gitFiles.addAll(status.getUnstagedFiles());
    gitFiles.addAll(status.getStagedFiles());
    return gitFiles;
  }
  
  /**
   * Stage files.
   * 
   * @param repository The current repository.
   * 
   * @return <code>true</code> if we have staged files.
   */
  private void stageFiles(String repository) {
    repository = FileHelper.rewriteSeparator(repository);
    
    List<FileStatus> unstagedFiles = GitAccess.getInstance().getUnstagedFiles();
    Set<String> allSelectedFiles = ProjectViewManager.getSelectedFilesDeep(pluginWS);
    List<FileStatus> stagedFiles = new ArrayList<>();
    for (FileStatus unstagedFileStatus : unstagedFiles) {
      if (allSelectedFiles.contains(repository + "/" + unstagedFileStatus.getFileLocation())
          && unstagedFileStatus.getChangeType() != GitChangeType.CONFLICT) {
        stagedFiles.add(unstagedFileStatus);
      }
    }
    GitAccess.getInstance().addAll(stagedFiles);
  }
  
  /**
   * Check if the Git diff action is enabled.
   * 
   * @return <code>true</code> if the action is enabled.
   */
  private boolean shouldEnableGitDiffAction() {
    boolean shouldEnable = true;
    File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWS);
    if (selectedFiles != null) {
      if (selectedFiles.length > 1 || selectedFiles.length == 1 && selectedFiles[0].isDirectory()) {
        // disable the diff action if there are 2 or more files selected or if
        // the files selected is a directory
        shouldEnable = false;
      }
    }
    return shouldEnable;
  }

  /**
   * Get the repository corresponding to the given file.
   * We search for only one file because in oXygen all files from the Project view
   * are in the same project/repository.
   * 
   * @param file The file.
   * 
   * @return the repository, or <code>null</code> if couldn't be detected.
   */
  private String getRepositoryForFile(File file) {
    String repository = null;
    while (repository == null && file.getParent() != null) {
      if (FileHelper.isGitRepository(file.getPath())) {
        repository = file.getAbsolutePath();
      }
      file = file.getParentFile();
    }
    return repository;
  }

}
