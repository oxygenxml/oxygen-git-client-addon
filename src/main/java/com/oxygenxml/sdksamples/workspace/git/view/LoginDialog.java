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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.service.GitAccess;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;
import com.oxygenxml.sdksamples.workspace.git.utils.UserCredentials;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * TODO In the API there is a OKCancelDialog class that we could use. It will integrate better in Oxygen. 
 */
public class LoginDialog extends OKCancelDialog {

	private boolean push;
	private GitAccess gitAccess;
	private JTextField tfUsername;
	private JPasswordField pfPassword;
	private JLabel lbUsername;
	private JLabel lbPassword;
	private boolean succeeded;

	public LoginDialog(final GitAccess gitAccess, final boolean push) {
		super(null, "GitAccount", true);
		this.gitAccess = gitAccess;
		this.push = push;

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel lblGitRemote = new JLabel("Enter details: "+ gitAccess.getRemoteName());
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		panel.add(lblGitRemote, gbc);
		
		
		lbUsername = new JLabel("Username: ");
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

		lbPassword = new JLabel("Password: ");
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

		
		
		this.getOkButton().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String username = getUsername();
				String password = getPassword();
				UserCredentials userCredentials = new UserCredentials(username, password);
				OptionsManager.getInstance().saveGitCredentials(userCredentials);
				Thread thread = new Thread(new AppWorker(userCredentials, gitAccess, push));
				dispose();
				thread.start();
			}
		});
	
		this.getCancelButton().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
	

		this.add(panel, BorderLayout.CENTER);
		
		pack();
    setResizable(false);
    setVisible(true);
    setModal(true);
    setLocationRelativeTo(null);
		
	}

	public String getUsername() {
		return tfUsername.getText().trim();
	}

	public String getPassword() {
		return new String(pfPassword.getPassword());
	}

	public boolean isSucceeded() {
		return succeeded;
	}
}