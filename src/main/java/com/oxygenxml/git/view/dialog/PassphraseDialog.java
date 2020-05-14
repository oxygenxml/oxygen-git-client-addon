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
import com.oxygenxml.git.options.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Obtains a new pass phrase from the user.
 * 
 * @author alex_jitianu
 */
public class PassphraseDialog extends OKCancelDialog {
  /**
   * The pass phrase given by the user. <code>null</code> if the user canceled the interaction.
   */
	private String passphrase;
	/**
	 * Password field.
	 */
	private JPasswordField tfPassphrase;

	public PassphraseDialog(String message) {
		super((JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getParentFrame(),
				"SSH Passphrase", true);
		
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

		tfPassphrase = new JPasswordField();
		tfPassphrase.setPreferredSize(new Dimension(250, tfPassphrase.getPreferredSize().height));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(tfPassphrase, gbc);

		this.getContentPane().add(panel);
		this.setMinimumSize(new Dimension(380, 150));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(
		    (JFrame) ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).getParentFrame());
		this.setVisible(true);
	}

	@Override
	protected void doOK() {
		passphrase = String.valueOf(tfPassphrase.getPassword());
		OptionsManager.getInstance().saveSshPassphare(passphrase);
		this.dispose();
	}

	/**
	 * @return The pass phrase obtained from the user or <code>null</code> if the user canceled the dialog.
	 */
	public String getPassphrase() {
		return passphrase;
	}

}
