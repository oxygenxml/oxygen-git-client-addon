package com.oxygenxml.git.view.branches;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.TextField;

/**
 * Dialog used to create a new local branch (from commit, from another branch, etc.)
 * or checkout a remote one.
 * 
 * @author Bogdan Draghici
 *
 */
public class CreateBranchDialog extends OKCancelDialog { // NOSONAR (java:S110)
  /**
   * Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateBranchDialog.class.getName());
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
   * Check box to choose whether or not to checkout the branch when 
   * creating it from a local branch or commit.
   */
  private JCheckBox checkoutLocalBranchCheckBox = 
      new JCheckBox(Translator.getInstance().getTranslation(Tags.CHECKOUT_BRANCH));
  /**
   * <code>true</code> of the dialog is created for checking out a remote branch.
   */
  private boolean isCheckoutRemote;

  /**
   * Public constructor.
   * 
   * @param title            The title of the dialog.
   * @param nameToPropose    The name to propose. Can be <code>null</code>.
   * @param isCheckoutRemote <code>true</code> if we create by checking out a remote branch.
   */
  public CreateBranchDialog(
      String title,
      String nameToPropose,
      boolean isCheckoutRemote) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, title, true);

    this.isCheckoutRemote = isCheckoutRemote;
    
    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel, nameToPropose);
    getContentPane().add(panel);
    setResizable(true);
    pack();
    
    setOkButtonText(isCheckoutRemote ? TRANSLATOR.getTranslation(Tags.CHECKOUT) 
        : TRANSLATOR.getTranslation(Tags.CREATE));
    
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
   * @param panel         The panel in which the components are added.
   * @param nameToPropose The name to propose. Can be <code>null</code>.
   */
  private void createGUI(JPanel panel, String nameToPropose) {
    // Branch name label.
    JLabel label = new JLabel(TRANSLATOR.getTranslation(Tags.BRANCH_NAME) + ": ");
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(label, gbc);
    
    // Branch name field.
    branchNameField = new TextField();
    if (nameToPropose != null) {
      branchNameField.setText(nameToPropose);
    }
    branchNameField.setPreferredSize(new Dimension(300, branchNameField.getPreferredSize().height));
    branchNameField.selectAll();
    gbc.gridx ++;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(branchNameField, gbc);
    
    label.setLabelFor(branchNameField);
    
    // Error message area
    errorMessageTextArea = UIUtil.createMessageArea("");
    errorMessageTextArea.setForeground(Color.RED);
    Font font = errorMessageTextArea.getFont();
    errorMessageTextArea.setFont(font.deriveFont(font.getSize() - 1.0f));
    gbc.gridx = 1;
    gbc.gridy ++;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(3, 0, 0, 0);
    panel.add(errorMessageTextArea, gbc);
    
    // "Checkout branch" check box
    if (!isCheckoutRemote) {
      checkoutLocalBranchCheckBox = new JCheckBox(Translator.getInstance().getTranslation(Tags.CHECKOUT_BRANCH));
      checkoutLocalBranchCheckBox.setSelected(OptionsManager.getInstance().isCheckoutNewlyCreatedLocalBranch());
      gbc.gridx = 0;
      gbc.gridy ++;
      gbc.gridwidth = 2;
      gbc.insets = new Insets(0, 0, 7, 0);
      panel.add(checkoutLocalBranchCheckBox, gbc);
    }

  }

  /**
   * @see ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog.doOK()
   */
  @Override
  protected void doOK() {
    updateUI(branchNameField.getText());
    if (getOkButton().isEnabled()) {
      OptionsManager.getInstance().setCheckoutNewlyCreatedLocalBranch(
          checkoutLocalBranchCheckBox.isSelected());
      super.doOK();
    }
  }
  
  /**
   * Update UI components depending whether the provided branch name
   * is valid or not.
   * 
   * @param branchName The branch name provided in the input field.
   */
  private void updateUI(String branchName) {
    boolean branchAlreadyExists = false;
    try {
      branchAlreadyExists = BranchesUtil.existsLocalBranch(branchName);
    } catch (NoRepositorySelected e) {
      // Not really possible 
      LOGGER.debug(e.getMessage(), e);
    }
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
  
  /**
   * @return <code>true</code> to checkout the newly created branch.
   */
  public boolean shouldCheckoutNewBranch() {
    return isCheckoutRemote || checkoutLocalBranchCheckBox.isSelected();
  }
}
