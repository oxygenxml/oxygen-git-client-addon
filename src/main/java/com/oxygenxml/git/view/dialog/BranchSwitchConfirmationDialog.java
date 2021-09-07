package com.oxygenxml.git.view.dialog;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

public class BranchSwitchConfirmationDialog extends OKOtherAndCancelDialog {

  /**
   * Constructor.
   *
   * @param title             Dialog title.
   * @param questionMessage   A question message connected to the presented information. May be <code>null</code>.
   * @param option1ButtonName Text to be written on the button in case the answer to the question is option 1.
   * @param option2ButtonName Text to be written on the button in case the answer to the question is option 2.
   * @param cancelButtonName  Text to be written on the button in case the answer to the question is negative.
   */
  public BranchSwitchConfirmationDialog(
          String title,
          String questionMessage,
          String option1ButtonName,
          String option2ButtonName,
          String cancelButtonName) {
    super(
            PluginWorkspaceProvider.getPluginWorkspace() != null ?
                    (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
            title,
            true);

    JPanel panel = getContentPanel();
    GridBagConstraints gbc = new GridBagConstraints();

    // Icon
    String iconPath = Icons.QUESTION_ICON;
    JLabel iconLabel = new JLabel();
    Icon infoIcon = Icons.getIcon(iconPath);
    if (infoIcon != null) {
      iconLabel.setIcon(infoIcon);
    }
    gbc.insets = new Insets(
            UIConstants.COMPONENT_TOP_PADDING,
            UIConstants.COMPONENT_LEFT_PADDING,
            UIConstants.COMPONENT_BOTTOM_PADDING,
            10);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    panel.add(iconLabel, gbc);

    JTextArea textArea = UIUtil.createMessageArea("");
    textArea.setDocument(new FileStatusDialog.CustomWrapDocument());
    textArea.setLineWrap(false);
    textArea.setText(questionMessage);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    panel.add(textArea, gbc);

    this.setButtonText(getOKButton(), option1ButtonName);
    this.setButtonText(getOtherButton(), option2ButtonName);
    this.setButtonText(getCancelButton(), cancelButtonName);

    setResizable(false);
    pack();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  }

}
