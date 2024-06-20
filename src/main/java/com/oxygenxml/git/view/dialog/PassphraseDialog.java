package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

import com.oxygenxml.git.constants.UIConstants;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Obtains a new pass phrase from the user.
 * 
 * @author alex_jitianu
 */
@SuppressWarnings("java:S110")
public abstract class PassphraseDialog extends OKCancelDialog {
  /**
   * Dialog minimum height.
   */
  private static final int DLG_MIN_HEIGHT = 150;
  /**
   * Dialog minimum width.
   */
  private static final int DLG_MIN_WIDTH = 380;
  /**
   * Dialog preferred width.
   */
  private static final int DLG_PREFERRED_WIDTH = 250;
  /**
   * The pass phrase given by the user. <code>null</code> if the user canceled the interaction.
   */
	private String passphrase;
	/**
	 * Passphrase field.
	 */
	private JPasswordField passphraseField;

	/**
	 * Constructor.
	 * 
	 * @param title   Dialog title.
	 * @param message Message.
	 */
	public PassphraseDialog(String title, String message) {
		super(
		    (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    title,
		    true);
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JLabel infrmation = new JLabel("<html>" + message + "</html>");
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		panel.add(infrmation, gbc);

		JLabel label = new JLabel("Passphrase: ");
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(label, gbc);

		passphraseField = new JPasswordField();
		passphraseField.setPreferredSize(new Dimension(DLG_PREFERRED_WIDTH, passphraseField.getPreferredSize().height));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(passphraseField, gbc);

		this.getContentPane().add(panel);
		this.setMinimumSize(new Dimension(DLG_MIN_WIDTH, DLG_MIN_HEIGHT));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(
		    (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setVisible(true);
	}

	/**
	 * @return The pass phrase obtained from the user or <code>null</code> if the user canceled the dialog.
	 */
	public String getPassphrase() {
		return passphrase;
	}
	
	/**
   * Do OK.
   */
  @Override
  protected void doOK() {
    passphrase = String.valueOf(passphraseField.getPassword());
    savePassphrase(passphrase);
    dispose();
  }
  
  /**
   * Save the given passphrase in the options storage.
   * 
   * @param passphrase the passphrase to be saved.
   */
  protected abstract void savePassphrase(String passphrase);

}
