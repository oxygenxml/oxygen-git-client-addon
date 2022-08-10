/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.staging;

import java.io.File;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to choose the project that should be open
 * 
 * @author gabriel_nedianu
 *
 */
public class OpenProjectDialog extends OKCancelDialog {
  
  /**
   * XPR files combo
   */
  JComboBox<File> filesCombo;

  /**
   * Constructor 
   * 
   * @param parentFrame Parent Frame
   * @param title Title of the dialog
   * @param modal <code>true</code> if it should be modal
   * @param xprFiles The list of files that will be shown in the dialog
   */
  public OpenProjectDialog(JFrame parentFrame, String title, boolean modal, List<File> xprFiles) {
    super(parentFrame, title, modal);
    filesCombo = new JComboBox<>(xprFiles.toArray(new File[0]));
    this.getContentPane().add(filesCombo);
    this.pack();
  }
  
  /**
   * @return The selected from the dialog
   */
  public File getSelectedFile() {
    return (File) filesCombo.getSelectedItem();
  }
  

}
