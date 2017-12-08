package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

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
  private StageController stageController;

  /**
   * Constructor.
   * 
   * @param fileStatuses      File statuses.
   * @param stageController   Staging controller.
   */
  public DiscardAction(List<FileStatus> fileStatuses, StageController stageController) {
    super(translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD));
    this.fileStatuses = fileStatuses;
    this.stageController = stageController;
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    String[] options = new String[] { "   Yes   ", "   No   " };
    int[] optonsId = new int[] { 0, 1 };
    int response = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
        translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD),
        translator.getTranslation(Tags.CONTEXTUAL_MENU_DISCARD_CONFIRMATION_MESSAGE), options, optonsId);
    if (response == 0) {
      for (FileStatus file : fileStatuses) {
        if (file.getChangeType() == GitChangeType.ADD
            || file.getChangeType() == GitChangeType.UNTRACKED) {
          try {
            FileUtils.forceDelete(
                new File(OptionsManager.getInstance().getSelectedRepository() + '/' + file.getFileLocation()));
          } catch (IOException e1) {
            logger.error(e1, e1);
          }
        } else if (file.getChangeType() == GitChangeType.SUBMODULE) {
          GitAccess.getInstance().discardSubmodule();
        }
      }
      stageController.doGitCommand(fileStatuses, GitCommand.DISCARD);
    }
  }

}
