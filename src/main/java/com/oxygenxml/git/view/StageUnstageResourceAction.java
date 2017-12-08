package com.oxygenxml.git.view;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.event.GitCommand;
import com.oxygenxml.git.view.event.StageController;

public class StageUnstageResourceAction extends AbstractAction {
  
  /**
   * <code>true</code> if the action is "Stage".
   */
  private boolean stage;
  
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
   * @param stage <code>true</code> if the action is "Stage".
   */
  public StageUnstageResourceAction(
      List<FileStatus> fileStatuses,
      boolean stage,
      StageController stageCtrl) {
    super(stage ? Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_UNSTAGE)
        : Translator.getInstance().getTranslation(Tags.CONTEXTUAL_MENU_STAGE));
    this.fileStatuses = fileStatuses;
    this.stage = stage;
    this.stageCtrl = stageCtrl;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    GitCommand newState = GitCommand.STAGE;
    if (!stage) {
      newState = GitCommand.UNSTAGE;
    }
    stageCtrl.doGitCommand(fileStatuses, newState);
  }
  
}
