package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.UserCredentials;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.view.event.PushPullController;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * TODO In the API there is a OKCancelDialog class that we could use. It will integrate better in Oxygen. 
 */
public class LoginDialog extends OKCancelDialog {

	private PushPullController pushPullController;
	private String host;
	private JTextField tfUsername;
	private JPasswordField pfPassword;
	
	public LoginDialog(PushPullController pushPullController, String host) {
		super(null, "GitAccount", true);
		this.pushPullController = pushPullController;
		this.host = host;
		createGUI();
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
		
	}

	public void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel lblGitRemote = new JLabel("<html>Enter <b>"+ host + "</b> account: </html>");
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		panel.add(lblGitRemote, gbc);
		
		
		JLabel lbUsername = new JLabel("Username: ");
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(lbUsername, gbc);

		tfUsername = new JTextField(20);
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		panel.add(tfUsername, gbc);

		JLabel lbPassword = new JLabel("Password: ");
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		panel.add(lbPassword, gbc);

		pfPassword = new JPasswordField(20);
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		panel.add(pfPassword, gbc);

		
		this.add(panel, BorderLayout.CENTER);
	}
	
	protected void doOK() {
		String username = tfUsername.getText().trim();
		String password = new String(pfPassword.getPassword());
		UserCredentials userCredentials = new UserCredentials(username, password, host);
		OptionsManager.getInstance().saveGitCredentials(userCredentials);
		dispose();
		pushPullController.updateCredentials();
  }

}