/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.actions;

/**
 * A listener that notifies when buttons from different components should updates their enabled states.
 *  
 * @author adrian_sorop
 * @author alex_smarandache 
 */
public interface UpdateActionsStatesListener {
  
  /**
   * The method that updates the button states.
   */
  public void updateButtonStates();
  
}
