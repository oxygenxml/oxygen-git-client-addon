package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitOperationScheduler;
import com.oxygenxml.git.service.IGitViewProgressMonitor;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.actions.GitOperationProgressMonitor;
import com.oxygenxml.git.view.branches.BranchesUtil;
import com.oxygenxml.git.view.util.CoalescingDocumentListener;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;

/**
 * Dialog for checkout a commit.
 * 
 * @author alex_smarandache
 *
 */
public class CheckoutCommitDialog extends OKCancelDialog {

  /**
   * Left inset for the inner panels.
   */
  private static final int LEFT_INDENT = 23;

  /**
   * Translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();

  /**
   *  Logger for logging.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutCommitDialog.class); 

  /**
   * TextField for entering the branch name.
   */
  private JTextField branchNameTextField;

  /**
   * Radio button for create a new branch.
   */
  private JRadioButton createNewBranchRadio;

  /**
   * Radio button for detached HEAD.
   */
  private final JRadioButton detachedHEADRadio = new JRadioButton(TRANSLATOR.getTranslation(Tags.DETACHED_HEAD));

  /**
   * Create branch panel.
   */
  private JPanel createBranchPanel;

  /**
   * The error message area.
   */
  private final JTextArea errorMessageTextArea = UIUtil.createMessageArea("");

  /**
   * The commit.
   */
  private final RevCommit commit;

  /**
   * The commit path.
   */
  private final String commitPath;

  /**
   * Contains the previous branch name.
   */
  private String previousBranchNameUpdate = "";


  /**
   * Constructor.
   * 
   * @param commit   The RevCommit to checkout.
   */
  public CheckoutCommitDialog(RevCommit commit) {
    super(
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        TRANSLATOR.getTranslation(Tags.CHECKOUT),
        true);

    this.commit = commit;
    this.commitPath = null;

    createGUI();

    this.setResizable(true);
    this.pack();
    this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
    this.setVisible(true);
  }

  /**
   * Constructor.
   * 
   * @param commit   The path of commit to checkout.
   */
  public CheckoutCommitDialog(String commit) {
    super(
        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
        TRANSLATOR.getTranslation(Tags.CHECKOUT),
        true);

    this.commit = null;
    this.commitPath = commit;

    createGUI();

    this.setResizable(true);
    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setVisible(true);
  }


  /**
   * Adds to the dialog the labels and the text fields.
   */
  public void createGUI() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;

    // Create new branch radio
    ButtonGroup buttonGroup = new ButtonGroup();
    createNewBranchRadio = new JRadioButton(TRANSLATOR.getTranslation(Tags.CREATE_A_NEW_BRANCH));
    createNewBranchRadio.setFocusPainted(false);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(createNewBranchRadio, gbc);
    buttonGroup.add(createNewBranchRadio);

    // Create branch panel.
    createBranchPanel = createNewBranchPanel();
    gbc.gridy ++;
    gbc.insets = new Insets(0, 23, 0, 0);
    panel.add(createBranchPanel, gbc);

    // Detached HEAD radio
    detachedHEADRadio.setFocusPainted(false);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy ++;
    panel.add(detachedHEADRadio, gbc);
    buttonGroup.add(detachedHEADRadio);

    // Warning about detached HEAD.
    final JLabel warningAboutDeteachedHEAD = new JLabel();
    gbc.insets = new Insets(0, LEFT_INDENT, 0, 0);
    warningAboutDeteachedHEAD.setText(TextFormatUtil.toHTML(TRANSLATOR.getTranslation(
        Tags.DETACHED_HEAD_WARNING_MESSAGE)));
    warningAboutDeteachedHEAD.setIcon(Icons.getIcon(Icons.SMALL_WARNING_ICON));
    gbc.insets = new Insets(
        0,
        LEFT_INDENT,
        UIConstants.LAST_LINE_COMPONENT_BOTTOM_PADDING,
        0);
    gbc.gridx = 0;
    gbc.gridy ++;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(warningAboutDeteachedHEAD, gbc);

    this.add(panel, BorderLayout.CENTER);

    ItemListener radioItemListener = e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateGUI();
      }
    };
    detachedHEADRadio.addItemListener(radioItemListener);
    createNewBranchRadio.addItemListener(radioItemListener);

    setOkButtonText(TRANSLATOR.getTranslation(Tags.CHECKOUT));

    boolean selectNewBranchRadio = OptionsManager.getInstance().getCreateBranchWhenCheckoutCommit();
    if(selectNewBranchRadio) {
      createNewBranchRadio.doClick();
    } else {
      detachedHEADRadio.doClick();
    }

  }


  /**
   * Update GUI.
   */
  private void updateGUI() {
    Component[] components = createBranchPanel.getComponents();
    for (Component component : components) {
      component.setEnabled(createNewBranchRadio.isSelected());
    }

    if(detachedHEADRadio.isSelected()) {
      getOkButton().setEnabled(true);
      errorMessageTextArea.setText("");
      previousBranchNameUpdate = null;
    } else {
      updateUI(branchNameTextField.getText());
    }

    SwingUtilities.invokeLater(() -> {
      if (branchNameTextField.isEnabled()) {
        branchNameTextField.requestFocus();
      }
    });

    OptionsManager.getInstance().setCreateBranchWhenCheckoutCommit(createNewBranchRadio.isSelected());
  }


  /**
   * @return The panel for create a new branch.
   */
  private JPanel createNewBranchPanel() {
    JPanel createNewBranchPanel = new JPanel(new GridBagLayout());

    JLabel branchNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.ENTER_BRANCH_NAME) + ":");
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(
        0, 0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
    c.anchor = GridBagConstraints.WEST;
    c.weightx = 0;
    c.weighty = 0;
    c.gridx = 0;
    c.gridy = 0;
    createNewBranchPanel.add(branchNameLabel, c);

    branchNameTextField = OxygenUIComponentsFactory.createTextField();
    updateUI("");
    Runnable updateRunnable = () -> {
      errorMessageTextArea.setVisible(true);
      updateUI(branchNameTextField.getText());
    };

    branchNameTextField.getDocument().addDocumentListener(new CoalescingDocumentListener(updateRunnable));
    branchNameTextField.requestFocus();
    branchNameTextField.setPreferredSize(new Dimension(250, branchNameTextField.getPreferredSize().height));
    c.insets = new Insets(
        0, 0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        0);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    c.gridx ++;
    createNewBranchPanel.add(branchNameTextField, c);

    c.gridy++;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    errorMessageTextArea.setVisible(true);
    errorMessageTextArea.setForeground(Color.RED);
    Font font = errorMessageTextArea.getFont();
    errorMessageTextArea.setFont(font.deriveFont(font.getSize() - 1.0f));
    createNewBranchPanel.add(errorMessageTextArea, c);

    return createNewBranchPanel;
  }

  @Override
  protected void doOK() {
    if(createNewBranchRadio.isSelected() && branchNameTextField.getText().isBlank()) {
      previousBranchNameUpdate = null;
      updateUI("");
      
      return;
    }
    
    GitOperationScheduler.getInstance().schedule(() -> {
      try {
        final Optional<IGitViewProgressMonitor> progMon = Optional.of(
            new GitOperationProgressMonitor(
                new ProgressDialog(
                    Translator.getInstance().getTranslation(Tags.CHECKOUT),
                    false)));

        GitAccess gitAccess = GitAccess.getInstance();
        String branchName = createNewBranchRadio.isSelected() ? branchNameTextField.getText() : null;
        if (commit != null) {
          gitAccess.checkoutCommit(commit, branchName, progMon);
        } else {
          gitAccess.checkoutCommit(commitPath, branchName, progMon); 
        }
      } catch (GitAPIException e) {
        LOGGER.error(e.getMessage(),  e);
      }
    });
    
    super.doOK();
  }


  /**
   * Update UI components depending whether the provided branch name is valid or not.
   * 
   * @param branchName The branch title provided in the input field.
   */
  private void updateUI(String branchName) {

    boolean titleAlreadyExists        = false;
    boolean titleContainsSpace        = false;
    boolean titleContainsInvalidChars = false;
    boolean titleTooLong              = false;

    if(previousBranchNameUpdate != null && branchName.equals(previousBranchNameUpdate)) {
      return;
    }
    if (!branchName.isEmpty()) {
      titleContainsSpace = branchName.contains(" ");
      titleContainsInvalidChars = !Repository.isValidRefName(Constants.R_HEADS + branchName);
      titleTooLong = branchName.length() > BranchesUtil.BRANCH_NAME_MAXIMUM_LENGTH;

      if (titleContainsSpace) {
        errorMessageTextArea.setText(TRANSLATOR.getTranslation(Tags.BRANCH_CONTAINS_SPACES));
      } else if (titleContainsInvalidChars) {
        errorMessageTextArea.setText(TRANSLATOR.getTranslation(Tags.BRANCH_CONTAINS_INVALID_CHARS));
      } else if (titleTooLong) {
        errorMessageTextArea.setText(MessageFormat.format(TRANSLATOR.getTranslation(Tags.BRANCH_NAME_TOO_LONG),
            BranchesUtil.BRANCH_NAME_MAXIMUM_LENGTH));
      } else {
        try {
          titleAlreadyExists = BranchesUtil.existsLocalBranch(branchName);
          errorMessageTextArea.setText(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH_ALREADY_EXISTS));
        } catch (NoRepositorySelected e) {
          LOGGER.error(e.getMessage(), e);
        }
      }
    } else {
      errorMessageTextArea.setText(TRANSLATOR.getTranslation(Tags.EMPTY_BRANCH_NAME));
    }

    boolean isBranchNameValid = !branchName.isEmpty() && !titleAlreadyExists && !titleContainsSpace && !titleContainsInvalidChars && !titleTooLong;
    if(!detachedHEADRadio.isSelected()) {
      getOkButton().setEnabled(isBranchNameValid);	
    }

    if(isBranchNameValid) {
      errorMessageTextArea.setText("");
    }

    previousBranchNameUpdate = branchName;

  }

}
