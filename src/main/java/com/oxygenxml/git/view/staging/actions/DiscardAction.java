package com.oxygenxml.git.view.staging.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.staging.ChangesPanel.SelectedResourcesProvider;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Discard action.
 * 
 * @author alex_jitianu
 */
public class DiscardAction extends AbstractAction {
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(DiscardAction.class.getName());

  /**
   * Translator for i18n.
   */
  private static Translator translator = Translator.getInstance();

  /**
   * Selected resources provider.
   */
  private SelectedResourcesProvider selResProvider;

  /**
   * Git controller.
   */
  private GitControllerBase gitController;

  /**
   * Constructor.
   * 
   * @param selResProvider  Selected resources provider..
   * @param gitController   Git controller.
   */
  public DiscardAction(SelectedResourcesProvider selResProvider, GitControllerBase gitController) {
    super(translator.getTranslation(Tags.DISCARD));
    this.selResProvider = selResProvider;
    this.gitController = gitController;
  }
  
  /**
   * Action performed.
   * 
   * @param event The event.
   */
  @Override
  public void actionPerformed(ActionEvent event) {
    String[] options = new String[] { 
        "   " + translator.getTranslation(Tags.YES) + "   ",
        "   " + translator.getTranslation(Tags.NO) + "   "};
    int[] optonsId = new int[] { 0, 1 };
    StandalonePluginWorkspace wsAccess = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    int response = wsAccess.showConfirmDialog(
        translator.getTranslation(Tags.DISCARD),
        translator.getTranslation(Tags.DISCARD_CONFIRMATION_MESSAGE), options, optonsId);
    if (response == 0) {
      List<FileStatus> allSelectedResources = selResProvider.getAllSelectedResources();
      
      // Execute Git command
      gitController.asyncDiscard(allSelectedResources, () -> {
        Set<File> foldersToRefresh = getFoldersToRefresh(allSelectedResources);
        refreshFilesInProject(wsAccess, foldersToRefresh);
      }
      );
      
    }
  }

  private Set<File> getFoldersToRefresh(List<FileStatus> allSelectedResources) {
    Set<File> foldersToRefresh = new HashSet<>();
    Set<File> deletedFilesParentDirs = new HashSet<>();
    String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
    
    for (FileStatus file : allSelectedResources) {
      if (file.getChangeType() == GitChangeType.ADD
          || file.getChangeType() == GitChangeType.UNTRACKED) {
        try {
          File fileToDiscard = new File(selectedRepository, file.getFileLocation());
          FileUtils.forceDelete(fileToDiscard);
          // Collect the parent folders. We'll later have to find the common ancestor and refresh it.
          deletedFilesParentDirs.add(fileToDiscard.getParentFile());
        } catch (IOException e1) {
          LOGGER.error(e1.getMessage(), e1);
        }
      } else if (file.getChangeType() == GitChangeType.SUBMODULE) {
        discardSubmodule(file, foldersToRefresh, selectedRepository);
      } else if (file.getChangeType() == GitChangeType.REMOVED
          || file.getChangeType() == GitChangeType.MISSING) {
        deletedFilesParentDirs.add(new File(selectedRepository, file.getFileLocation()).getParentFile());
      }
    }
    
    // Also refresh the common folder of all the individual discarded files
    if (!deletedFilesParentDirs.isEmpty()) {
      foldersToRefresh.add(FileUtil.getCommonDir(deletedFilesParentDirs));
    }
    return foldersToRefresh;
  }

  /**
   * Refresh the files in the project view.
   * 
   * @param wsAccess Workspace access.
   * @param foldersToRefresh Files to refresh.
   */
  private static void refreshFilesInProject(StandalonePluginWorkspace wsAccess, Set<File> foldersToRefresh) {
    // Refresh the Project view
    SwingUtilities.invokeLater(() -> wsAccess.getProjectManager().refreshFolders(foldersToRefresh.toArray(new File[0])));
  }

  /**
   * Discard submodule.
   * 
   * @param submoduleDir       The submodule directory.
   * @param foldersToRefresh   The folders to refresh after the discard operation.
   * @param selectedRepository The current repository.
   */
  private void discardSubmodule(
      FileStatus submoduleDir,
      Set<File> foldersToRefresh,
      String selectedRepository) {
    try {
      GitAccess.getInstance().getSubmoduleAccess().discardSubmodule();
      foldersToRefresh.add(new File(selectedRepository, submoduleDir.getFileLocation()));
    } catch (GitAPIException e1) {
      LOGGER.error(e1.getMessage(), e1);
    }
  }

}
