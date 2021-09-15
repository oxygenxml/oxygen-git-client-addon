package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.CredentialsBase;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.PersonalAccessTokenInfo;
import com.oxygenxml.git.options.UserAndPasswordCredentials;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.PlatformDetectionUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

@SuppressWarnings("java:S110")
public class LoginDialog extends OKCancelDialog {
  /**
   * GitHub host.
   */
  private static final String GITHUB_COM = "github.com";
  /**
   * Left inset for the inner panels.
   */
  private static final int INNER_PANELS_LEFT_INSET = 21;
  /**
   * Dialog preferred height.
   */
  private static final int DLG_PREF_HEIGHT = PlatformDetectionUtil.isMacOS() ? 305 : 250;
  /**
   * Dialog preferred width.
   */
  private static final int DLG_PREF_WIDTH = 400;
  /**
   * The translator for the messages that are displayed in this dialog
   */
  private static Translator translator = Translator.getInstance();
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
	 * The new credentials stored by this dialog
	 */
	private CredentialsBase credentials;
	/**
	 * Basic (user + password) authentication radio button.
	 */
  private JRadioButton basicAuthRadio;
  /**
   * Personal access token authentication radio button.
   */
  private JRadioButton tokenAuthRadio;
  /**
   * Personal access token text field.
   */
  private JTextField tokenTextField;
  /**
   * Username and password panel.
   */
  private JPanel userAndPasswordPanel;

	/**
	 * Constructor.
	 * 
	 * @param host         The host for which to provide the credentials.
	 * @param loginMessage The login message.
	 */
	public LoginDialog(String host, String loginMessage) {
		super(
		    (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.LOGIN_DIALOG_TITLE),
		    true);
		
		if (logger.isDebugEnabled()) {
		  logger.debug(new Exception("LOGIN DIALOG WAS SHOWN..."));
		}
		
		this.host = host;
		this.message = loginMessage;
		
		createGUI();

		this.setPreferredSize(new Dimension(DLG_PREF_WIDTH, DLG_PREF_HEIGHT));
		this.setResizable(false);
		this.pack();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setVisible(true);
	}

	/**
	 * Adds to the dialog the labels and the text fields.
	 */
	public void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// Info label
		JLabel lblGitRemote = new JLabel(
				"<html>" + message + "<br/>" 
				    + translator.getTranslation(Tags.LOGIN_DIALOG_MAIN_LABEL) 
				    + " <b>" + host + "</b>"
				    + "."
				    + "</html>");
		gbc.insets = new Insets(
		    0,
		    0,
		    UIConstants.COMPONENT_BOTTOM_PADDING,
				0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(lblGitRemote, gbc);
		
		// Basic authentication radio
		ButtonGroup buttonGroup = new ButtonGroup();
		basicAuthRadio = new JRadioButton(translator.getTranslation(Tags.BASIC_AUTHENTICATION));
		basicAuthRadio.setFocusPainted(false);
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridx = 0;
    gbc.gridy ++;
    panel.add(basicAuthRadio, gbc);
    buttonGroup.add(basicAuthRadio);
    
    // User + password
    userAndPasswordPanel = createUserAndPasswordPanel();
    gbc.gridy ++;
    gbc.insets = new Insets(0, INNER_PANELS_LEFT_INSET, 0, 0);
    panel.add(userAndPasswordPanel, gbc);
    
    // Personal access token radio
    tokenAuthRadio = new JRadioButton(translator.getTranslation(Tags.PERSONAL_ACCESS_TOKEN));
    tokenAuthRadio.setFocusPainted(false);
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 0;
    gbc.gridy ++;
    panel.add(tokenAuthRadio, gbc);
    buttonGroup.add(tokenAuthRadio);
    
    // Token field
    tokenTextField = new JTextField();
    gbc.insets = new Insets(
        0,
        INNER_PANELS_LEFT_INSET,
        UIConstants.LAST_LINE_COMPONENT_BOTTOM_PADDING,
        0);
    gbc.gridx = 0;
    gbc.gridy ++;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(tokenTextField, gbc);

		this.add(panel, BorderLayout.CENTER);
		
		ItemListener radioItemListener = e -> {
		  if (e.getStateChange() == ItemEvent.SELECTED) {
		    updateGUI();
		  }
		};
    tokenAuthRadio.addItemListener(radioItemListener);
		basicAuthRadio.addItemListener(radioItemListener);
		
		initGUI();
	}
	
  /**
   * Init GUI.
   */
  private void initGUI() {
    setOkButtonText(translator.getTranslation(Tags.AUTHENTICATE));
    
    if (GITHUB_COM.equals(host)) {
      tokenAuthRadio.doClick();
    } else {
      basicAuthRadio.doClick();
    }
  }

	/**
	 * Update GUI.
	 */
  private void updateGUI() {
    Component[] components = userAndPasswordPanel.getComponents();
    for (Component component : components) {
      component.setEnabled(basicAuthRadio.isSelected());
    }
    tokenTextField.setEnabled(tokenAuthRadio.isSelected());

    SwingUtilities.invokeLater(() -> {
      if (tokenTextField.isEnabled()) {
        tokenTextField.requestFocus();
      } else if (tfUsername.isEnabled()) {
        tfUsername.requestFocus();
      }
    });
  }

	/**
	 * @return The username and password panel.
	 */
  private JPanel createUserAndPasswordPanel() {
    JPanel userAndPassPanel = new JPanel(new GridBagLayout());
    
    // Username label
		JLabel lbUsername = new JLabel(translator.getTranslation(Tags.LOGIN_DIALOG_USERNAME_LABEL));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(
		    0,
		    0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		userAndPassPanel.add(lbUsername, c);

		// Username text field
		tfUsername = new JTextField();
		tfUsername.setPreferredSize(new Dimension(250, tfUsername.getPreferredSize().height));
		c.insets = new Insets(
        0,
        0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx ++;
		userAndPassPanel.add(tfUsername, c);

		// Password label
		JLabel lbPassword = new JLabel(translator.getTranslation(Tags.LOGIN_DIALOG_PASS_WORD_LABEL));
		c.insets = new Insets(
        0,
        0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        UIConstants.COMPONENT_RIGHT_PADDING);
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy ++;
		userAndPassPanel.add(lbPassword, c);

		// Password text field
		pfPassword = new JPasswordField();
		pfPassword.setPreferredSize(new Dimension(250, pfPassword.getPreferredSize().height));
		c.insets = new Insets(
        0,
        0,
        UIConstants.COMPONENT_BOTTOM_PADDING,
        0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx ++;
		userAndPassPanel.add(pfPassword, c);
		
		return userAndPassPanel;
  }

	@Override
	protected void doOK() {
	  if (basicAuthRadio.isSelected()) {
	    String username = tfUsername.getText().trim();
	    String password = new String(pfPassword.getPassword());
	    credentials = new UserAndPasswordCredentials(username, password, host);
    } else {
      String tokenValue = tokenTextField.getText().trim();
      credentials = new PersonalAccessTokenInfo(host, tokenValue);
    }
	  
	  OptionsManager.getInstance().saveGitCredentials(credentials);
	  
		super.doOK();
	}

	/**
	 * @return The user credentials retrieved from the user. <code>null</code> if the user canceled
	 * the dialog.
	 */
	public CredentialsBase getCredentials() {
		return credentials;
	}
	
}