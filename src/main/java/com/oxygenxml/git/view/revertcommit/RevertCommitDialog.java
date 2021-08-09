package com.oxygenxml.git.view.revertcommit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used to revert commit from history view
 * 
 * @author Razvan Tudosie
 *
 */
public class RevertCommitDialog extends OKCancelDialog { // NOSONAR (java:S110)
  /**
   * Logger for logging.
   */
  private static final Logger logger = LogManager.getLogger(RevertCommitDialog.class.getName());
  /**
   * Translator.
   */
  private static final Translator TRANSLATOR = Translator.getInstance();
  
  /**
   * The error message area.
   */
  private JTextArea errorMessageTextArea;


  /**
   * Public constructor.
   * 
   * @param title            The title of the dialog.
   * @param isCheckoutRemote <code>true</code> if we create by checking out a remote branch.
   */
  public RevertCommitDialog(
      String title) {
    super(PluginWorkspaceProvider.getPluginWorkspace() != null
        ? (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame()
        : null, title, true);
    
    // Create GUI
    JPanel panel = new JPanel(new GridBagLayout());
    createGUI(panel);
    getContentPane().add(panel);
    setResizable(true);
    pack();
    
    setOkButtonText(TRANSLATOR.getTranslation(Tags.YES));
    setCancelButtonText(TRANSLATOR.getTranslation(Tags.NO));
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
  private void createGUI(JPanel panel) {
    // Branch name label.
    JLabel label = new JLabel(TRANSLATOR.getTranslation(Tags.REVERT_COMMIT_WARNING) + "? ");
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(label, gbc);
    
    
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
    

  }

  /**
   * @see ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog.doOK()
   */
  @Override
  protected void doOK() {
    if (getOkButton().isEnabled()) {
     
      super.doOK();
    }
  }
  
}
