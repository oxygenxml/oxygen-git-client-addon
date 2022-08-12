/*
 * Copyright (c) 2022 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

package com.oxygenxml.git.view.staging;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.annotation.TestOnly;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to choose the project that should be open
 * 
 * @author gabriel_nedianu
 *
 */
public class OpenProjectDialog extends OKCancelDialog {
  
  /**
   * The translator for the messages that are displayed in this panel
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER =  LoggerFactory.getLogger(WorkingCopySelectionPanel.class);
  
  /**
   * XPR files combo
   */
  private JComboBox<File> filesCombo;

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
    createGUI(xprFiles);
    this.pack();
  }
  
  /**
   * Add the label and combo  
   */
  private void createGUI(List<File> xprFiles) {

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    
    JLabel dialogText = new JLabel(TRANSLATOR.getTranslation(Tags.DETECT_AND_OPEN_XPR_FILES_DIALOG_TEXT));
    panel.add(dialogText, gbc);
    
    gbc.insets = new Insets(
       2*UIConstants.COMPONENT_TOP_PADDING,
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.gridy++;
    JLabel selectXprText = new JLabel(TRANSLATOR.getTranslation(Tags.SELECT_OXYGEN_PROJECT) + ":");
    panel.add(selectXprText, gbc);
    
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING,
         UIConstants.COMPONENT_LEFT_PADDING,
         UIConstants.COMPONENT_BOTTOM_PADDING,
         UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.gridy++;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    xprFiles.sort(Comparator.naturalOrder());
    filesCombo = new JComboBox<>(xprFiles.toArray(new File[0]));
    filesCombo.setRenderer(new OpenProjectFilesCellRenderer());
    panel.add(filesCombo, gbc);
    
    this.getContentPane().add(panel);
  }

  /**
   * @return The selected from the dialog
   */
  public File getSelectedFile() {
    return (File) filesCombo.getSelectedItem();
  }
  
  /**
   * Renderer for the xpr comboBox
   * 
   * @author gabriel_nedianu
   *
   */
  @SuppressWarnings("serial")
  private final class OpenProjectFilesCellRenderer extends DefaultListCellRenderer {
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      @SuppressWarnings("unchecked")
      JLabel comp = (JLabel) super.getListCellRendererComponent((JList<File>)list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        File currentFile = (File) value;
        URI currentUri = currentFile.toURI();
        comp.setText(currentFile.getName());
        if (currentUri != null && currentUri.equals(getCurrentXprURI())) {
          comp.setText(comp.getText()+ " (" + TRANSLATOR.getTranslation(Tags.CURRENT) + ")");
        }
        comp.setToolTipText(currentFile.getPath().replace("\\", "/"));
      }
      return comp;
    }
  }
  
  /**
   * @return The uri of the current project(xpr) or <code>null</code>
   */
  private static URI getCurrentXprURI(){
    URI currentXPRuri = null;
    StandalonePluginWorkspace spw = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    try {
      currentXPRuri = spw.getProjectManager().getCurrentProjectURL().toURI();
    } catch (URISyntaxException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(),e);
      }
    }
    return currentXPRuri;
  }
  
  /**
   * Getter.
   * 
   * @return The combo with files.
   */
  @TestOnly
  public JComboBox<File> getFilesCombo() {
    return filesCombo;
  }

}
