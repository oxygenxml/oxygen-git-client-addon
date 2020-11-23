package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

@SuppressWarnings("java:S110")
public class LoginDialog extends OKCancelDialog {
  /**
   *  Logger for logging.
   */
  private static Logger logger = Logger.getLogger(LoginDialog.class); 
	/**
	 * The host for which to enter the credentials
	 */
	private String host;

	/**
	 * The error message
	 */
	private String message;

	/**
	 * TextField for entering the username
	 */
	private JTextField tfUsername;

	/**
	 * TextField for entering the password
	 */
	private JPasswordField pfPassword;

	/**
	 * The new user credentials stored by this dialog
	 */
	private UserCredentials userCredentials;

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Constructor.
	 * 
	 * @param host         The host for which to provide the credentials.
	 * @param loginMessage The login message.
	 */
	public LoginDialog(String host, String loginMessage) {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.LOGIN_DIALOG_TITLE), true);
		if (logger.isDebugEnabled()) {
		  logger.debug(new Exception("LOGIN DIALOG WAS SHOWN..."));
		}
		this.host = host;
		this.message = loginMessage;
		createGUI();

		this.setMinimumSize(new Dimension(250, 200));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setVisible(true);
	}

	/**
	 * Adds to the dialog the labels and the text fields.
	 */
	public void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel lblGitRemote = new JLabel(
				"<html>" + message + "<br>" + translator.getTranslation(Tags.LOGIN_DIALOG) + " <b>" + host + "</b></html>");
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		panel.add(lblGitRemote, gbc);

		JLabel lbUsername = new JLabel(translator.getTranslation(Tags.LOGIN_DIALOG_USERNAME_LABEL));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(lbUsername, gbc);

		tfUsername = new JTextField();
		tfUsername.setPreferredSize(new Dimension(250, tfUsername.getPreferredSize().height));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		panel.add(tfUsername, gbc);

		JLabel lbPassword = new JLabel(translator.getTranslation(Tags.LOGIN_DIALOG_PASS_WORD_LABEL));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		panel.add(lbPassword, gbc);

		pfPassword = new JPasswordField();
		pfPassword.setPreferredSize(new Dimension(250, pfPassword.getPreferredSize().height));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		panel.add(pfPassword, gbc);

		this.add(panel, BorderLayout.CENTER);
	}

	@Override
	protected void doOK() {
		String username = tfUsername.getText().trim();
		String password = new String(pfPassword.getPassword());
		userCredentials = new UserCredentials(username, password, host);
		OptionsManager.getInstance().saveGitCredentials(userCredentials);
		super.doOK();
	}

	/**
	 * @return The user credentials retreived from the user. <code>null</code> if the user canceled
	 * the dialog.
	 */
	public UserCredentials getUserCredentials() {
		return userCredentials;
	}
}