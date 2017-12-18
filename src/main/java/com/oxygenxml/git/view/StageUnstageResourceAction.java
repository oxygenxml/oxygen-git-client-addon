package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

/**
 * Moves files inside/outside the INDEX.
 */
public class StageUnstageResourceAction extends AbstractAction {
  
  /**
   * <code>true</code> if the action is "Stage".
   */
  private boolean isStage;
  
  /**
   * The files to be staged/unstaged.
   */
  private List<FileStatus> fileStatuses;

  /**
   * Staging controller.
   */
  private StageController stageCtrl;

  /**
   * Constructor.
   * 
   * @param fileStatuses Moves files inside/outside the INDEX.
   * @param isStage <code>true</code> if the action is "Stage".
   * @param stageCtrl Staging controller.
   */
  public StageUnstageResourceAction(
      List<FileStatus> fileStatuses,
      boolean isStage,
      StageController stageCtrl) {
    super(isStage ? Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_STAGE)
        : Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_UNSTAGE));
    this.fileStatuses = fileStatuses;
    this.isStage = isStage;
    this.stageCtrl = stageCtrl;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitCommand command = isStage ? GitCommand.STAGE : GitCommand.UNSTAGE;
    stageCtrl.doGitCommand(fileStatuses, command);
  }
  
}
