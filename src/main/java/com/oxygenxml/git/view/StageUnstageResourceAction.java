package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommandEvent;
import com.oxygenxml.git.view.event.GitController;

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
  private GitController stageCtrl;

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
      GitController stageCtrl) {
    super(isStage ? Translator.getInstance().getTranslation(Tags.STAGE)
        : Translator.getInstance().getTranslation(Tags.UNSTAGE));
    this.fileStatuses = fileStatuses;
    this.isStage = isStage;
    this.stageCtrl = stageCtrl;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitCommandEvent command = isStage ? GitCommandEvent.STAGE_STARTED : GitCommandEvent.UNSTAGE_STARTED;
    stageCtrl.doGitCommand(fileStatuses, command);
  }
  
}
