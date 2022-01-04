/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.actions.internal;

import javax.swing.AbstractAction;

/**
 * Abstract action which is always enabled.
 * 
 * @author alex_smarandache
 *
 */
public abstract class AlwaysEnabledAction extends AbstractAction {
  
  /**
   * Constructor.
   *	
   * @param name The action name.
   */
  protected AlwaysEnabledAction(String name) {
    super(name);
  }
  
  
  /**
   * This method will do nothing.
   */
  @Override
  public void setEnabled(boolean newValue) {
    super.setEnabled(true);
  }
  
}
