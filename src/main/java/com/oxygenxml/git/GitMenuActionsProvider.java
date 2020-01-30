package com.oxygenxml.git;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

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
import com.oxygenxml.git.view.blame.BlameManager;
import com.oxygenxml.git.view.event.GitController;
import com.oxygenxml.git.view.historycomponents.HistoryController;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

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
  private HistoryController historyController; 
  
  /**
   * Constructor.
   * 
   * @param pluginWorkspaceAccess Plug-in workspace access.
   * @param gitCtrl             The staging panel.
   */
  public GitMenuActionsProvider(
      StandalonePluginWorkspace pluginWorkspaceAccess,
      GitController gitCtrl,
      HistoryController historyController) {
    this.pluginWS = pluginWorkspaceAccess;
    this.gitCtrl = gitCtrl;
    this.historyController = historyController;

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
   * Get the Git-specific actions for the current selection from the Project view.
   */
  public List<AbstractAction> getActionsForProjectViewSelection() {
    List<AbstractAction> actions = new ArrayList<>();
    GitOperationScheduler gitOpScheduler = GitOperationScheduler.getInstance();
    
    // Create the Git actions, if not already created
    if (gitDiffAction == null) {
      gitDiffAction = new AbstractAction(translator.getTranslation(Tags.GIT_DIFF)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          gitOpScheduler.schedule(GitMenuActionsProvider.this::doGitDiff);
        }
      };
    }
    if (commitAction == null) {
      commitAction = new AbstractAction(translator.getTranslation(Tags.COMMIT)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          gitOpScheduler.schedule(GitMenuActionsProvider.this::doPrepareCommit);
        } 
      };
    }
    if (showHistoryAction == null) {
      showHistoryAction = new AbstractAction(translator.getTranslation(Tags.SHOW_HISTORY)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          gitOpScheduler.schedule(GitMenuActionsProvider.this::doShowHistoryForSelectedFile);
        }
      };
    }
    if (showBlameAction == null) {
      showBlameAction = new AbstractAction(translator.getTranslation(Tags.SHOW_BLAME)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          gitOpScheduler.schedule(GitMenuActionsProvider.this::doShowBlameForSelectedFile);
        }
      };
    }
  
    
    // Enable/disable
    commitAction.setEnabled(true);
    gitDiffAction.setEnabled(shouldEnableGitDiffAction());
    
    // Add the Git actions to the list
    actions.add(gitDiffAction);
    actions.add(commitAction);
    actions.add(showHistoryAction);
    actions.add(showBlameAction);

    return actions;
  }
  
  /**
   * Show blame for selected resource.
   */
  private void doShowBlameForSelectedFile() {
    try {
      setBusyCursor(true);
      
      File selFile = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWS)[0];
      String repository = getRepositoryForFile(selFile);
      if (repository != null) {
        Path repo = Paths.get(repository);
        Path file = Paths.get(selFile.getAbsolutePath());
        String relativeFilePath = repo.relativize(file).toString();
        try {
          BlameManager.getInstance().doBlame(
              FileHelper.rewriteSeparator(relativeFilePath),
              historyController);
        } catch (IOException | GitAPIException e1) {
          logger.error(e1, e1);
        }
      }
    } finally {
      setBusyCursor(false);
    }
  }
  
  /**
   * Show history for selected resource.
   */
  private void doShowHistoryForSelectedFile() {
    try {
      setBusyCursor(true);
      
      File selFile = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWS)[0];
      String repository = getRepositoryForFile(selFile);
      if (repository != null) {
        Path repo = Paths.get(repository);
        Path file = Paths.get(selFile.getAbsolutePath());
        String relativeFilePath = repo.relativize(file).toString();
        historyController.showResourceHistory(FileHelper.rewriteSeparator(relativeFilePath));
      }
    } finally {
      setBusyCursor(false);
    }
  }

  /**
   * Do Git Diff.
   */
  private void doGitDiff() {
    try {
      setBusyCursor(true);
      
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
          logger.error(e1, e1);
        }
      }
    } finally {
      setBusyCursor(false);
    }
  }

  /**
   * Prepare commit.
   */
  private void doPrepareCommit() {
    try {
      setBusyCursor(true);
      
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
    } finally {
      setBusyCursor(false);
    }
  }
  
  /**
   * Set busy cursor or default.
   * 
   * @param isSetBusy <code>true</code> to set busy cursor.
   */
  private void setBusyCursor(boolean isSetBusy) {
    if (isSetBusy) {
      SwingUtilities.invokeLater(() -> {
        Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        
        prjViewInfo.getComponent().setCursor(busyCursor);
        ((JFrame) pluginWS.getParentFrame()).setCursor(busyCursor);
        if (gitHistoryViewInfo != null) {
          gitHistoryViewInfo.getComponent().setCursor(busyCursor);
        }
        
        WSEditor currentEditorAccess = pluginWS.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
        WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
        JComponent pageComp = null;
        if (currentPage instanceof WSAuthorEditorPage) {
          pageComp = (JComponent) ((WSAuthorEditorPage) currentPage).getAuthorComponent();
        } else if (currentPage instanceof WSTextEditorPage) {
          pageComp = (JComponent) ((WSTextEditorPage) currentPage).getTextComponent();
        }
        if (pageComp != null) {
          pageComp.setCursor(busyCursor);
        }
      });
    } else {
      SwingUtilities.invokeLater(() -> {
        Cursor defaultCursor = Cursor.getDefaultCursor();
        
        prjViewInfo.getComponent().setCursor(defaultCursor);
        ((JFrame) pluginWS.getParentFrame()).setCursor(defaultCursor);
        if (gitHistoryViewInfo != null) {
          gitHistoryViewInfo.getComponent().setCursor(defaultCursor);
        }
        
        WSEditor currentEditorAccess = pluginWS.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
        WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
        JComponent pageComp = null;
        if (currentPage instanceof WSAuthorEditorPage) {
          pageComp = (JComponent) ((WSAuthorEditorPage) currentPage).getAuthorComponent();
        } else if (currentPage instanceof WSTextEditorPage) {
          pageComp = (JComponent) ((WSTextEditorPage) currentPage).getTextComponent();
        }
        if (pageComp != null) {
          pageComp.setCursor(defaultCursor);
        }
      });
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
