package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.google.common.io.Files;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitController;
import com.oxygenxml.git.service.GitStatus;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.RepoUtil;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.dialog.UIUtil;
import com.oxygenxml.git.view.history.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * Provides Git-specific actions for the contextual menu of the Project view.
 * 
 * @author sorin_carbunaru
 */
public class ProjectMenuGitActionsProvider {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(ProjectMenuGitActionsProvider.class.getName());
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
  private GitController gitCtrl;

  /**
   * The "Commit" action.
   */
  private AbstractAction commitAction;
  
  /**
   * The Git diff action. Compares local and remote versions of the file.
   */
  private AbstractAction gitDiffAction;

  /**
   * Show resource history.
   */
  private AbstractAction showHistoryAction;
  
  /**
   * Show blame.
   */
  private AbstractAction showBlameAction;
  
  /**
   * Project view.
   */
  private ViewInfo prjViewInfo;
  
  /**
   * Git history view.
   */
  private ViewInfo gitHistoryViewInfo;
  
  /**
   * History controller.
   */
  private HistoryController historyCtrl; 
  
  /**
   * Constructor.
   * 
   * @param pluginWorkspaceAccess Plug-in workspace access.
   * @param gitCtrl             The staging panel.
   */
  public ProjectMenuGitActionsProvider(
      StandalonePluginWorkspace pluginWorkspaceAccess,
      GitController gitCtrl,
      HistoryController historyController) {
    this.pluginWS = pluginWorkspaceAccess;
    this.gitCtrl = gitCtrl;
    this.historyCtrl = historyController;
    
    createAllActions();

    pluginWorkspaceAccess.addViewComponentCustomizer(
        viewInfo -> {
          if ("Project".equals(viewInfo.getViewID())) {
            prjViewInfo = viewInfo;
          } else if (OxygenGitPluginExtension.GIT_HISTORY_VIEW.equals(viewInfo.getViewID())) {
            gitHistoryViewInfo = viewInfo;
          }
        });
  }
  
  /**
   * @return the Git-specific actions for the current selection from the Project view.
   */
  public List<AbstractAction> getActionsForProjectViewSelection() {
    List<AbstractAction> actions = new ArrayList<>();
    
    // Enable/disable
    commitAction.setEnabled(true);
    gitDiffAction.setEnabled(isSingleNonBinaryFileSelected());
    showBlameAction.setEnabled(isSingleNonBinaryFileSelected());
    showHistoryAction.setEnabled(isSingleFileOrFolderSelected());
    
    // Add the Git actions to the list
    actions.add(gitDiffAction);
    actions.add(commitAction);
    actions.add(showHistoryAction);
    actions.add(showBlameAction);

    return actions;
  }

  /**
   * Create all actions.
   */
  private void createAllActions() {
    GitOperationScheduler gitOpScheduler = GitOperationScheduler.getInstance();
    gitDiffAction = new AbstractAction(translator.getTranslation(Tags.GIT_DIFF)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitOpScheduler.schedule(ProjectMenuGitActionsProvider.this::doGitDiff);
      }
    };
    commitAction = new AbstractAction(translator.getTranslation(Tags.COMMIT)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitOpScheduler.schedule(ProjectMenuGitActionsProvider.this::doPrepareCommit);
      } 
    };
    showHistoryAction = new AbstractAction(translator.getTranslation(Tags.SHOW_HISTORY)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitOpScheduler.schedule(
            () -> ProjectAndEditorPageMenuActionsUtil.showHistory(
                pluginWS.getProjectManager().getSelectedFiles()[0],
                historyCtrl,
                getViewsToSetCursorOn()));
      }
    };
    showBlameAction = new AbstractAction(translator.getTranslation(Tags.SHOW_BLAME)) {
      @Override
      public void actionPerformed(ActionEvent e) {
        gitOpScheduler.schedule(
            () -> ProjectAndEditorPageMenuActionsUtil.showBlame(
                pluginWS.getProjectManager().getSelectedFiles()[0],
                historyCtrl,
                getViewsToSetCursorOn()));
      }
    };
  }
  
  /**
   * @return a list of views to shown a certain type of cursor on. Never <code>null</code>.
   */
  private List<ViewInfo> getViewsToSetCursorOn() {
    List<ViewInfo> views = new ArrayList<>();
    views.add(prjViewInfo);
    if (gitHistoryViewInfo != null) {
      views.add(gitHistoryViewInfo);
    }
    return views;
  }

  /**
   * Do Git Diff.
   */
  private void doGitDiff() {
    try {
      UIUtil.setBusyCursor(true, Arrays.asList(prjViewInfo));
      
      File selFile = pluginWS.getProjectManager().getSelectedFiles()[0];
      String repository = RepoUtil.getRepositoryForFile(selFile);
      if (repository != null) {
        try {
          RepoUtil.updateCurrentRepository(repository);

          List<FileStatus> gitFiles = getStagedAndUnstagedFiles();
          boolean wasDiffShown = false;
          if (!gitFiles.isEmpty()) {
            String selectedFilePath = FileHelper.rewriteSeparator(selFile.getAbsolutePath());
            for (FileStatus fileStatus : gitFiles) {
              if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
                SwingUtilities.invokeLater(() -> DiffPresenter.showDiff(fileStatus, gitCtrl));
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
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Repository opening failed due to: " + e1.getMessage());
          
          logger.error(e1, e1);
        }
      }
    } finally {
      UIUtil.setBusyCursor(false, Arrays.asList(prjViewInfo));
    }
  }

  /**
   * Prepare commit.
   */
  private void doPrepareCommit() {
    try {
      UIUtil.setBusyCursor(true, Arrays.asList(prjViewInfo));
      
      File[] selectedFiles = pluginWS.getProjectManager().getSelectedFiles();
      String repository = RepoUtil.getRepositoryForFile(selectedFiles[0]);
      if (repository != null) {
        try {
          RepoUtil.updateCurrentRepository(repository);

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
          PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage("Repository opening failed due to: " + e1.getMessage());
          
          logger.error(e1, e1);
        }
      }
    } finally {
      UIUtil.setBusyCursor(false, Arrays.asList(prjViewInfo));
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
    Set<String> allSelectedFiles = ProjectViewManager.getSelectedFilesDeep();
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
   * @return <code>true</code> if a single file (not folder) is selected.
   */
  private boolean isSingleNonBinaryFileSelected() {
    boolean isNonBinaryFile = true;
    File[] selectedFiles = pluginWS.getProjectManager().getSelectedFiles();
    if (selectedFiles != null) {
      if (selectedFiles.length > 1 || selectedFiles.length == 1 && selectedFiles[0].isDirectory()) {
        isNonBinaryFile = false;
      } else if (selectedFiles.length == 1) {
        URL selFileURL = null;
        try {
          selFileURL = selectedFiles[0].toURI().toURL();
        } catch (MalformedURLException e) {
          logger.warn(e, e);
        }
        String ext = Files.getFileExtension(selectedFiles[0].getName());
        isNonBinaryFile = selFileURL != null 
            && !pluginWS.getUtilAccess().isUnhandledBinaryResourceURL(selFileURL)
            && !FileHelper.isArchiveExtension(ext);
      }
    }
    return isNonBinaryFile;
  }
  
  /**
   * @return <code>true</code> if a single resource (file or folder) is selected.
   */
  private boolean isSingleFileOrFolderSelected() {
    boolean isSingleRes = false;
    File[] selectedFiles = pluginWS.getProjectManager().getSelectedFiles();
    if (selectedFiles != null && selectedFiles.length == 1) {
      isSingleRes = true;
    }
    return isSingleRes;
  }

}
