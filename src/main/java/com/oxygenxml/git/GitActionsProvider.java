package com.oxygenxml.git;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.DiffPresenter;
import com.oxygenxml.git.view.StagingPanel;

import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Provides Git-specific actions.
 * 
 * @author sorin_carbunaru
 */
public class GitActionsProvider {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(GitActionsProvider.class.getName());
  /**
   * Plug-in workspace access.
   */
  private StandalonePluginWorkspace pluginWorkspaceAccess;
  /**
   * Translator.
   */
  private Translator translator;
  /**
   * Staging panel.
   */
  private StagingPanel stagingPanel;

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
   * @param stagingPanel
   *          The staging panel.
   */
  public GitActionsProvider(StandalonePluginWorkspace pluginWorkspaceAccess, Translator translator,
      StagingPanel stagingPanel) {
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    this.translator = translator;
    this.stagingPanel = stagingPanel;
  }

  /**
   * Get the Git-specific actions for the current selection from the Project view.
   */
  public List<AbstractAction> getActionsForProjectViewSelection() {
    List<AbstractAction> actions = new ArrayList<AbstractAction>();
    
    // Create the Git actions, if not already created
    if (commitAction == null) {
      commitAction = new AbstractAction(translator.getTraslation(Tags.PROJECT_VIEW_COMMIT_CONTEXTUAL_MENU_ITEM)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          pluginWorkspaceAccess.showView(CustomWorkspaceAccessPluginExtension.GIT_STAGING_VIEW, true);
          // TODO Alex This INIT can be done in the view itself.
          boolean filesStaged = false;
          File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
          // TODO Sorin: the repository will be the same for all actions if project not changed
          // Maybe we can remove the repository search, as well as the repository switching all the time
          // It's probably what Alex is saying above.
          File repository = new File(selectedFiles[0].getAbsolutePath());
          while (repository.getParent() != null) {
            if (FileHelper.isGitRepository(repository.getAbsolutePath())) {
              break;
            }
            repository = repository.getParentFile();
          }
          String previousRepository = OptionsManager.getInstance().getSelectedRepository();
          try {
            GitAccess.getInstance().setRepository(repository.getAbsolutePath());
            List<FileStatus> gitFiles = GitAccess.getInstance().getUnstagedFiles();
            Set<String> allFiles = ProjectViewManager.getSelectedFilesDeep(pluginWorkspaceAccess);
            for (FileStatus fileStatus : gitFiles) {
              if (allFiles
                  .contains(repository.getAbsolutePath().replace("\\", "/") + "/" + fileStatus.getFileLocation())
                  && fileStatus.getChangeType() != GitChangeType.CONFLICT) {
                filesStaged = true;
                GitAccess.getInstance().add(fileStatus);
              }
            }

            if (filesStaged) {
              if (OptionsManager.getInstance().getRepositoryEntries().contains(repository.getAbsolutePath())) {
                stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
                    .setSelectedItem(repository.getAbsolutePath());
              } else {
                OptionsManager.getInstance().addRepository(repository.getAbsolutePath());
                stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
                    .addItem(repository.getAbsolutePath());
                stagingPanel.getWorkingCopySelectionPanel().getWorkingCopySelector()
                    .setSelectedItem(repository.getAbsolutePath());
              }
              return;
            }

            GitAccess.getInstance().setRepository(previousRepository);
          } catch (Exception e1) {
            if (logger.isDebugEnabled()) {
              logger.debug(e1, e1);
            }
          }
        }
      };
    }
    if (gitDiffAction == null) {
      gitDiffAction = new AbstractAction(translator.getTraslation(Tags.PROJECT_VIEW_GIT_DIFF_CONTEXTUAL_MENU_ITEM)) {
        @Override
        public void actionPerformed(ActionEvent e) {
          File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow(pluginWorkspaceAccess);
          // the diff action is enabled only for one file
          File repository = new File(selectedFiles[0].getAbsolutePath());

          // We try and find the
          while (repository.getParent() != null) {
            if (FileHelper.isGitRepository(repository.getAbsolutePath())) {
              break;
            }
            repository = repository.getParentFile();
          }
          try {
            String previousRepository = OptionsManager.getInstance().getSelectedRepository();
            GitAccess.getInstance().setRepository(repository.getAbsolutePath());
            OptionsManager.getInstance().saveSelectedRepository(repository.getAbsolutePath());
            List<FileStatus> gitFiles = GitAccess.getInstance().getUnstagedFiles();
            gitFiles.addAll(GitAccess.getInstance().getStagedFile());
            String selectedFilePath = selectedFiles[0].getAbsolutePath().replace("\\", "/");
            for (FileStatus fileStatus : gitFiles) {
              if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
                DiffPresenter diff = new DiffPresenter(fileStatus, stagingPanel.getStageController(), translator);
                diff.showDiff();
                break;
              }
            }
            OptionsManager.getInstance().saveSelectedRepository(previousRepository);
          } catch (Exception e1) {
            if (logger.isDebugEnabled()) {
              logger.debug(e1, e1);
            }
          }
        }
      };
    }
    
    // Enable/disable
    commitAction.setEnabled(true);
    gitDiffAction.setEnabled(isGitDiffActionEnabled());
    
    // Add the Git actions to the list
    actions.add(commitAction);
    actions.add(gitDiffAction);

    return actions;
  }
  
  /**
   * Check if the Git diff action is enabled.
   * 
   * @return <code>true</code> if the action is enabled.
   */
  private boolean isGitDiffActionEnabled() {
    boolean isEnabled = true;
    File[] selectedFiles = ProjectViewManager.getSelectedFilesAndDirsShallow((StandalonePluginWorkspace) pluginWorkspaceAccess);
    if (selectedFiles != null) {
      if (selectedFiles.length > 1 || selectedFiles.length == 1 && selectedFiles[0].isDirectory()) {
        // disable the diff action if there are 2 or more files selected or if
        // the files selected is a directory
        isEnabled = false;
      } else if (selectedFiles.length == 1) {
        String repository = null;
        File selectedFile = new File(selectedFiles[0].getAbsolutePath());
        while (repository == null && selectedFile.getParent() != null) {
          if (FileHelper.isGitRepository(selectedFile.getPath())) {
            repository = selectedFile.getPath();
          }
          selectedFile = selectedFile.getParentFile();
        }

        if (repository != null) {
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
            String selectedFilePath = selectedFiles[0].getAbsolutePath().replace("\\", "/");
            for (FileStatus fileStatus : gitFiles) {
              if (selectedFilePath.endsWith(fileStatus.getFileLocation())) {
                selectedFileStatus = new FileStatus(fileStatus);
                break;
              }
            }

            if (selectedFileStatus == null
                || selectedFileStatus != null && (selectedFileStatus.getChangeType() == GitChangeType.ADD
                    || selectedFileStatus.getChangeType() == GitChangeType.UNTRACKED
                    || selectedFileStatus.getChangeType() == GitChangeType.MISSING
                    || selectedFileStatus.getChangeType() == GitChangeType.REMOVED)) {

              isEnabled = false;
            }

          } catch (RepositoryNotFoundException e) {
            logger.error(e, e);
          } catch (IOException e) {
            logger.error(e, e);
          }
        }
      }
    }
    return isEnabled;
  }

}
