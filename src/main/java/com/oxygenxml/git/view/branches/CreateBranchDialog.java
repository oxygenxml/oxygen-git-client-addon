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
import com.oxygenxml.git.view.CoalescedEventUpdater;
import com.oxygenxml.git.view.dialog.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * A dialog used to get the name for a new branch and verifies if that name
 * already exists.
 * 
 * @author Bogdan Draghici
 *
 */
public class CreateBranchDialog extends OKCancelDialog {
  /**
   * The warning message in case the name for the branch already exists.
   */
  private final static String warningMessage = "The local branch already exists!";
  /**
   * A text field for the branch name.
   */
  private JTextField branchNameField;
  /**
   * The warning message area.
   */
  private JTextArea warningMessageArea;
  /**
   * The list with all the local branches.
   */
  private List<String> existingBranches;

  /**
   * Public constructor.
   * 
   * @param title            The title of the dialog.
   * @param currentBranch    The name of the branch from which to do the checkout.
   * @param existingBranches A list with all existing local branches.
   */
  public CreateBranchDialog(String title, String currentBranch, List<String> existingBranches) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, title, true);

    this.existingBranches = existingBranches;
    
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel, currentBranch);
    getContentPane().add(panel);
    setResizable(true);
    pack();
    
    getOkButton().setEnabled(!verifyBranchExistence());
    addCoalescingForBranchField();

    if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
      setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    }
    setVisible(true);
  }

  /**
   * Adds the elements to the user interface in a graphical way.
   * 
   * @param panel The panel in which the components are added.
   * @param gbc   A GridBagConstraints instance.
   */
  private void createGUI(JPanel panel,String currentBranch) {
    // Branch name label.
    JLabel label = new JLabel(Translator.getInstance().getTranslation(Tags.BRANCH_NAME));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(
        UIConstants.COMPONENT_TOP_PADDING, 
        UIConstants.COMPONENT_LEFT_PADDING,
        UIConstants.COMPONENT_BOTTOM_PADDING, 
        UIConstants.COMPONENT_RIGHT_PADDING);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(label, gbc);
    
    // Branch name field.
    branchNameField = UIUtil.createTextField();
    if (currentBranch != null) {
      branchNameField.setText(currentBranch);
    }
    branchNameField.setPreferredSize(new Dimension(200, branchNameField.getPreferredSize().height));
    branchNameField.selectAll();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.weighty = 0;
    gbc.gridx = 1;
    gbc.gridy = 0;
    panel.add(branchNameField, gbc);

    // Warning message for branch name.
    warningMessageArea = UIUtil.createMessageArea(warningMessage);
    warningMessageArea.setForeground(Color.RED);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    panel.add(warningMessageArea, gbc);
  }

  /**
   * Adds coalescing for the branch name field that also sets enable the ok button.
   */
  private void addCoalescingForBranchField() {
    CoalescedEventUpdater updater = new CoalescedEventUpdater(500, () -> {
      getOkButton().setEnabled(!verifyBranchExistence());
    });
    branchNameField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        updater.update();
      }
    });
  }

  /**
   * {@see ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog.doOK()}
   */
  @Override
  protected void doOK() {
    boolean branchAlreadyExists = verifyBranchExistence();
    getOkButton().setEnabled(!branchAlreadyExists);
    if (!branchAlreadyExists) {
      super.doOK();
    }
  }

  /**
   * Verifies the existence of a branch with the same name as the one typed in the
   * TextField.
   */
  private boolean verifyBranchExistence() {
    String text = branchNameField.getText();
    boolean isBranchExistent = false;
    for (String string : existingBranches) {
      if (string.equals(text)) {
        isBranchExistent = true;
      }
    }
    warningMessageArea.setText(isBranchExistent ? warningMessage : "");
    if (isBranchExistent || text.isEmpty()) {
      return true;
    }
    return false;
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
