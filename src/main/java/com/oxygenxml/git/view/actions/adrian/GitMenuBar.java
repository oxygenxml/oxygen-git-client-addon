/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.actions.adrian;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.oxygenxml.git.view.actions.GitActionsManager;

import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

public class GitMenuBar implements MenuBarCustomizer {
  
  /**
   * The Git menu.
   */
  private final JMenu gitMenu = OxygenUIComponentsFactory.createMenu("Meniu Nou de Git");
  
  public GitMenuBar(GitActionsManager actionsManager) {
    gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getCloneRepositoryAction()));
    gitMenu.add(OxygenUIComponentsFactory.createMenuItem(actionsManager.getShowTagsAction()));
  }

  @Override
  public void customizeMainMenu(JMenuBar mainMenu) {
    mainMenu.add(gitMenu);
  }

}
