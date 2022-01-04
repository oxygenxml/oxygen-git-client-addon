/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.actions.internal;

import javax.swing.AbstractAction;

/**
 * TODO comentat 
 */
public abstract class AlwaysEnabledAction extends AbstractAction {
  
  protected AlwaysEnabledAction(String name) {
    super(name);
  }
  
  
  @Override
  public void setEnabled(boolean newValue) {
    super.setEnabled(true);
  }
}
