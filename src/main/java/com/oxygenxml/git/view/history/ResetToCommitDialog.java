package com.oxygenxml.git.view.history;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.jgit.api.ResetCommand.ResetType;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to reset the current branch to a specified commit.
 * 
 * @author Bogdan Draghici
 *
 */
@SuppressWarnings("java:S110")
public class ResetToCommitDialog extends OKCancelDialog {
  /**
   * Maximum length for commit message. When longer than this,
   * we truncate it and add three dots.
   */
  private static final int COMMIT_MSG_MAX_LENGTH = 60;
  /**
   * Soft reset option label.
   */
  private static final String SOFT_RESET = "Soft";
  /**
   * Mixed reset option label.
   */
  private static final String MIXED_RESET = "Mixed";
  /**
   * Hard reset option label.
   */
  private static final String HARD_RESET = "Hard";
  /**
   * The soft reset option button.
   */
  private JRadioButton softResetButton;
  /**
   * The hard reset option button.
   */
  private JRadioButton hardResetButton;
  
  /**
   * The translator instance.
   */
  private static Translator translator = Translator.getInstance();

  /**
   * Public constructor.
   * 
   * @param branchName The name of the current branch.
   * @param commitCharacteristics   The commit id to which to reset the branch.
   */
  public ResetToCommitDialog(String branchName, CommitCharacteristics commitCharacteristics) {
    super(
        PluginWorkspaceProvider.getPluginWorkspace() != null ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
        translator.getTranslation(Tags.RESET_BRANCH_TO_COMMIT),
        true);

    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel, branchName, commitCharacteristics);
    getContentPane().add(panel);
    pack();
    setResizable(false);

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
  }

  /**
   * Adds the elements to the user interface
   * 
   * @param panel      The panel in which the components are added.
   * @param branchName The name of the current branch.
   * @param commitCharacteristics  The commit to which to reset the branch.
   */
  private void createGUI(JPanel panel, String branchName, CommitCharacteristics commitCharacteristics) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 7, 0);

    // Branch
    JLabel branchTagLabel = new JLabel(translator.getTranslation(Tags.BRANCH) + ": ");
    panel.add(branchTagLabel, gbc);
    
    JLabel branchNameLabel = new JLabel(branchName);
    gbc.gridx++;
    panel.add(branchNameLabel, gbc);

    
    // Commit 
    JLabel commitTagLabel = new JLabel(translator.getTranslation(Tags.COMMIT) + ": ");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    panel.add(commitTagLabel, gbc);
    
    JLabel commitMessageLabel = new JLabel ();
    String commitMessage = commitCharacteristics.getCommitMessage().replace("\n", " ");
    if(commitMessage.length() > COMMIT_MSG_MAX_LENGTH) {
      commitMessageLabel.setToolTipText(
              "<html>" + commitCharacteristics.getCommitMessage().replace("\n", "<br>") + "</html>");
      commitMessage = commitMessage.substring(0, COMMIT_MSG_MAX_LENGTH - "...".length()) + "...";
    }
    commitMessageLabel.setText("[" + commitCharacteristics.getCommitAbbreviatedId() + "] " 
        + commitMessage);
    gbc.gridx++;
    panel.add(commitMessageLabel, gbc);
    
    // Reset mode
    JLabel resetModeLabel = new JLabel(translator.getTranslation(Tags.RESET_MODE) + ":");
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(0, 0, 0, 0);
    panel.add(resetModeLabel, gbc);

    addRadioButtons(panel, gbc);

    setOkButtonText(translator.getTranslation(Tags.RESET));
  }

  /**
   * Adds the radio buttons with the reset options to the panel.
   * 
   * @param panel The panel in which the components are added.
   * @param gbc   The GridBagConstraints instance.
   */
  private void addRadioButtons(JPanel panel, GridBagConstraints gbc) {
    ButtonGroup buttonGroup = new ButtonGroup();
    
    softResetButton = new JRadioButton(SOFT_RESET + " - " + translator.getTranslation(Tags.SOFT_RESET_INFO));
    gbc.gridy++;
    buttonGroup.add(softResetButton);
    panel.add(softResetButton, gbc);

    JRadioButton mixedResetButton = new JRadioButton(MIXED_RESET + " - " + translator.getTranslation(Tags.MIXED_RESET_INFO));
    gbc.gridy++;
    buttonGroup.add(mixedResetButton);
    panel.add(mixedResetButton, gbc);

    hardResetButton = new JRadioButton(HARD_RESET + " - " + translator.getTranslation(Tags.HARD_RESET_INFO));
    gbc.gridy++;
    buttonGroup.add(hardResetButton);
    panel.add(hardResetButton, gbc);

    mixedResetButton.setSelected(true);
  }

  /**
   * Gets the reset type chosen.
   * 
   * @return The reset type. ResetType.MIXED by default.
   */
  public ResetType getResetType() {
    ResetType resetType = ResetType.MIXED;
    if (softResetButton.isSelected()) {
      resetType = ResetType.SOFT;
    } else if (hardResetButton.isSelected()) {
      resetType = ResetType.HARD;
    }
    return resetType;
  }
}
