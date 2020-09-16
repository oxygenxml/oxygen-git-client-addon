package com.oxygenxml.git.view.historycomponents;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.jgit.api.ResetCommand.ResetType;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to reset the current branch to a specified commit given by its
 * ID.
 * 
 * @author Bogdan Draghici
 *
 */
public class ResetToCommitDialog extends OKCancelDialog {
  /**
   * Soft reset option message.
   */
  private final String SOFT_RESET = "Soft";
  /**
   * Mixed reset option message.
   */
  private final String MIXED_RESET = "Mixed";
  /**
   * Hard reset option message.
   */
  private final String HARD_RESET = "Hard";
  /**
   * The soft reset option button.
   */
  private JRadioButton softResetButton;
  /**
   * The mixed reset option button.
   */
  private JRadioButton mixedResetButton;
  /**
   * The hard reset option button.
   */
  private JRadioButton hardResetButton;
  /**
   * The translator instance.
   */
  private static final Translator translator = Translator.getInstance();

  /**
   * Public constructor.
   * 
   * @param branchName The name of the current branch.
   * @param commitId   The commit id to which to reset the branch.
   */
  public ResetToCommitDialog(String branchName, String commitId) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, translator.getTranslation(Tags.RESET_TO_COMMIT_MESSAGE), true);

    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());

    createGUI(panel, branchName, commitId);
    getContentPane().add(panel);
    setResizable(true);
    pack();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
    setMinimumSize(new Dimension(300, 135));
    setVisible(true);
  }

  /**
   * Adds the elements to the user interface
   * 
   * @param panel      The panel in which the components are added.
   * @param branchName The name of the current branch.
   * @param commitId   The commit id to which to reset the branch.
   */
  private void createGUI(JPanel panel, String branchName, String commitId) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);

    // Reset branch label.
    JLabel resetBranchArea = new JLabel(translator.getTranslation(Tags.RESET_THE_BRANCH_LABEL) + branchName);
    gbc.gridwidth = 3;
    panel.add(resetBranchArea, gbc);

    // Commit label.
    JLabel toCommitArea = new JLabel(translator.getTranslation(Tags.TO_COMMIT_LABEL) + commitId);
    gbc.gridy++;
    panel.add(toCommitArea, gbc);

    // Reset type label.
    JLabel resetTypeLabel = new JLabel(translator.getTranslation(Tags.CHOOSE_RESET_TYPE_LABEL));
    gbc.gridy++;
    panel.add(resetTypeLabel, gbc);

    addRadioButtons(panel, gbc);

    getOkButton().setText(translator.getTranslation(Tags.RESET_BUTTON_TEXT));
  }

  /**
   * Adds the radio buttons with the reset options to the panel.
   * 
   * @param panel The panel in which the components are added.
   * @param gbc   The GridBagConstraints instance.
   */
  private void addRadioButtons(JPanel panel, GridBagConstraints gbc) {
    ButtonGroup buttonGroup = new ButtonGroup();
    gbc.weightx = 1;
    gbc.gridwidth = 1;

    softResetButton = new JRadioButton(SOFT_RESET);
    gbc.gridy++;
    buttonGroup.add(softResetButton);
    panel.add(softResetButton, gbc);

    mixedResetButton = new JRadioButton(MIXED_RESET);
    gbc.gridx++;
    buttonGroup.add(mixedResetButton);
    panel.add(mixedResetButton, gbc);

    hardResetButton = new JRadioButton(HARD_RESET);
    gbc.gridx++;
    buttonGroup.add(hardResetButton);
    panel.add(hardResetButton, gbc);

    softResetButton.setSelected(true);
  }

  /**
   * Gets the reset type chosen.
   * 
   * @return The reset type. Can be null if none of the options was chosen.
   */
  public ResetType getResetType() {
    if (softResetButton.isSelected()) {
      return ResetType.SOFT;
    } else if (mixedResetButton.isSelected()) {
      return ResetType.MIXED;
    } else if (hardResetButton.isSelected()) {
      return ResetType.HARD;
    }
    return null;
  }
}
