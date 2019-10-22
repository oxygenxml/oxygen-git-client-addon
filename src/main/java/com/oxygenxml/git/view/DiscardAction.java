package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.ProjectViewManager;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.GitController;

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
  private static Logger logger = Logger.getLogger(DiscardAction.class.getName());

  /**
   * Translator for i18n.
   */
  private static final Translator translator = Translator.getInstance();

  /**
   * File statuses.
   */
  private List<FileStatus> fileStatuses;

  /**
   * Staging controller.
   */
  private GitController stageController;

  /**
   * Constructor.
   * 
   * @param fileStatuses      File statuses.
   * @param stageController   Staging controller.
   */
  public DiscardAction(List<FileStatus> fileStatuses, GitController stageController) {
    super(translator.getTranslation(Tags.DISCARD));
    this.fileStatuses = fileStatuses;
    this.stageController = stageController;
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
    int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
        translator.getTranslation(Tags.DISCARD),
        translator.getTranslation(Tags.DISCARD_CONFIRMATION_MESSAGE), options, optonsId);
    if (response == 0) {
      Set<File> foldersToRefresh = new HashSet<>();
      Set<File> deletedFilesParentDirs = new HashSet<>();
      String selectedRepository = OptionsManager.getInstance().getSelectedRepository();
      
      for (FileStatus file : fileStatuses) {
        if (file.getChangeType() == GitChangeType.ADD
            || file.getChangeType() == GitChangeType.UNTRACKED) {
          try {
            File fileToDiscard = new File(selectedRepository, file.getFileLocation());
            FileUtils.forceDelete(fileToDiscard);
            // Collect the parent folders. We'll later have to find the common ancestor and refresh it.
            deletedFilesParentDirs.add(fileToDiscard.getParentFile());
          } catch (IOException e1) {
            logger.error(e1, e1);
          }
        } else if (file.getChangeType() == GitChangeType.SUBMODULE) {
          discardSubmodule(file, foldersToRefresh, selectedRepository);
        }
      }
      
      // Also refresh the common folder of all the individual discarded files
      if (!deletedFilesParentDirs.isEmpty()) {
        foldersToRefresh.add(FileHelper.getCommonDir(deletedFilesParentDirs));
      }
      
      // Execute Git command
      stageController.doGitCommand(fileStatuses, GitCommand.DISCARD);
      
      // Refresh the Project view
      ProjectViewManager.refreshFolders(foldersToRefresh.toArray(new File[0]));
    }
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
      GitAccess.getInstance().discardSubmodule();
      foldersToRefresh.add(new File(selectedRepository, submoduleDir.getFileLocation()));
    } catch (GitAPIException e1) {
      logger.error(e1, e1);
    }
  }

}
