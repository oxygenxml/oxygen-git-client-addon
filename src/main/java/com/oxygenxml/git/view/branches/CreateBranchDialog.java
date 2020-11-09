package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to create a new local branch (from commit, from another branch, etc.)
 * or checkout a remote one.
 * 
 * @author Bogdan Draghici
 *
 */
public class CreateBranchDialog extends OKCancelDialog { // NOSONAR (java:S110)
  /**
   * Translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  /**
   * A text field for the branch name.
   */
  private JTextField branchNameField;
  /**
   * The error message area.
   */
  private JTextArea errorMessageTextArea;
  /**
   * The list with all the local branches.
   */
  private List<String> existingBranches;

  /**
   * Public constructor.
   * 
   * @param title            The title of the dialog.
   * @param sourceBranch     The name of the branch from which to create a new one. Can be <code>null</code>,
   *                         for example when creating a branch from a commit.
   * @param existingBranches A list with all existing local branches.
   */
  public CreateBranchDialog(String title, String sourceBranch, List<String> existingBranches) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, title, true);

    this.existingBranches = existingBranches;
    
    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel, sourceBranch);
    getContentPane().add(panel);
    setResizable(true);
    pack();
    
    // Enable or disable the OK button based on the user input
    updateUI(branchNameField.getText());
    branchNameField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        updateUI(branchNameField.getText());
      }
    });
    branchNameField.requestFocus();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
    setMinimumSize(new Dimension(300, 135));
    setVisible(true);
  }

  /**
   * Adds the elements to the user interface/
   * 
   * @param panel The panel in which the components are added.
   * @param sourceBranch The name of the source branch. Can be <code>null</code>,
   *                         for example when creating a branch from a commit.
   */
  private void createGUI(JPanel panel, String sourceBranch) {
    // Branch name label.
    JLabel label = new JLabel(TRANSLATOR.getTranslation(Tags.BRANCH_NAME) + ": ");
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        UIConstants.COMPONENT_RIGHT_PADDING);
    panel.add(label, gbc);
    
    // Branch name field.
    branchNameField = UIUtil.createTextField();
    if (sourceBranch != null) {
      branchNameField.setText(sourceBranch);
    }
    branchNameField.setPreferredSize(new Dimension(200, branchNameField.getPreferredSize().height));
    branchNameField.selectAll();
    gbc.gridx ++;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(branchNameField, gbc);
    
    label.setLabelFor(branchNameField);

    // Error message area
    errorMessageTextArea = UIUtil.createMessageArea("");
    errorMessageTextArea.setForeground(Color.RED);
    gbc.gridx = 0;
    gbc.gridy ++;
    gbc.gridwidth = 2;
    panel.add(errorMessageTextArea, gbc);
  }

  /**
   * @see ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog.doOK()
   */
  @Override
  protected void doOK() {
    updateUI(branchNameField.getText());
    if (getOkButton().isEnabled()) {
      super.doOK();
    }
  }

  /**
   * Check if the given branch already exists.
   * 
   * @param branchName The branch name to check.
   * 
   * @return <code>true</code> if a locals branch with the given name already exists.
   */
  private boolean doesBranchAlreadyExist(String branchName) {
    return existingBranches.stream().anyMatch((String branch) -> branch.equals(branchName));
  }
  
  /**
   * Update UI components depending whether the provided branch name
   * is valid or not.
   * 
   * @param branchName The branch name provided in the input field.
   */
  private void updateUI(String branchName) {
    boolean branchAlreadyExists = doesBranchAlreadyExist(branchName);
    errorMessageTextArea.setText(branchAlreadyExists ? TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH_ALREADY_EXISTS) : "");
    
    boolean isBranchNameValid = !branchName.isEmpty() && !branchAlreadyExists;
    getOkButton().setEnabled(isBranchNameValid);
  }

  /**
   * Gets the name to be set for the new branch.
   * 
   * @return The name for the branch.
   */
  public String getBranchName() {
    return branchNameField.getText();
  }
}
