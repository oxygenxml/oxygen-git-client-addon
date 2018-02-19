package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.JTextComponent;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import com.oxygenxml.git.auth.AuthenticationInterceptor;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.UndoSupportInstaller;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

public class CloneRepositoryDialog extends OKCancelDialog {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(CloneRepositoryDialog.class);

	/**
	 * HTML start tag.
	 */
	private static final String HTML_END_TAG = "</html>";
	
	/**
	 * HTML end tag.
	 */
	private static final String HTML_START_TAG = "<html>";
	
	/**
	 * Clone worker.
	 */
	private class CloneWorker extends SwingWorker<Void, Void> {
    private final ProgressDialog progressDialog;
		private final URL url;
		private final File file;

		private CloneWorker(ProgressDialog progressDialog, URL url, File file) {
			this.progressDialog = progressDialog;
			this.url = url;
			this.file = file;
		}

		@Override
		protected Void doInBackground() throws Exception {
			CloneRepositoryDialog.this.setVisible(false);
			// Intercept all authentication requests.
			AuthenticationInterceptor.bind(url.getHost());
			GitAccess.getInstance().clone(url, file, progressDialog);
			progressDialog.dispose();
			return null;
		}

		@Override
		protected void done() {
			try {
			  // TODO Strange...when done is called, the entire processing should be done.
				get();
				OptionsManager.getInstance().saveDestinationPath(file.getAbsolutePath());
			} catch (InterruptedException e) {
				if (logger.isDebugEnabled()) {
				  logger.debug(e, e);
				}
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				progressDialog.dispose();
				Throwable cause = e.getCause();
				while (cause != null) {
				  boolean doBreak = false;
					if (cause.getMessage().contains("Download cancelled")) {
						try {
							FileUtils.cleanDirectory(file);
						} catch (IOException e1) {
							if (logger.isDebugEnabled()) {
								logger.debug(e1, e1);
							}
						}
						doBreak = true;
					} else if (cause instanceof NoRemoteRepositoryException) {
					  CloneRepositoryDialog.this.setVisible(true);
					  CloneRepositoryDialog.this.setMinimumSize(new Dimension(400, 190));
					  informationLabel.setText(
					      HTML_START_TAG 
					      + translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY) 
					      + HTML_END_TAG);
					  doBreak = true;
					} else if (cause instanceof org.eclipse.jgit.errors.TransportException) {
						UserCredentials userCredentials = new LoginDialog(url.getHost(),
								translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_LOGIN_MESSAGE))
						        .getUserCredentials();
						if (userCredentials != null) {
							doOK();
						}
						doBreak = true;
					}
					if (doBreak) {
					  break;
					}
					cause = cause.getCause();
				}
				return;
			} finally {
			  AuthenticationInterceptor.unbind(url.getHost());
			}
		}
	}

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Source URL text field.
	 */
	private JTextField sourceUrlTextField;

	/**
	 * Destination path combo box.
	 */
	private JComboBox<String> destinationPathCombo;

	/**
	 * Label for displaying information.
	 */
	private JLabel informationLabel;
	
	/**
	 * Constructor.
	 *  
	 * @param parentFrame Parent frame.
	 * @param translator Translation support.
	 */
	public CloneRepositoryDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_TITLE), true);

		createGUI();

		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setMinimumSize(new Dimension(475, 160));
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

	private void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JLabel lblURL = new JLabel(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_URL_LABEL) + ":");
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(lblURL, gbc);

		sourceUrlTextField = UIUtil.createTextField();
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		panel.add(sourceUrlTextField, gbc);

		JLabel lblPath = new JLabel(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_LABEL));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(lblPath, gbc);

		destinationPathCombo = new JComboBox<String>();
		UndoSupportInstaller.installUndoManager(((JTextComponent) destinationPathCombo.getEditor().getEditorComponent()));
		destinationPathCombo.setEditable(true);
		List<String> destinationPaths = OptionsManager.getInstance().getDestinationPaths();
		for (String string : destinationPaths) {
			destinationPathCombo.addItem(string);
		}
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		panel.add(destinationPathCombo, gbc);

		Action browseButtonAction = new AbstractAction() {

			@Override
      public void actionPerformed(ActionEvent e) {
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					destinationPathCombo.setSelectedItem(directory.getAbsolutePath());
				}
			}
		};
		ToolbarButton browseButton = new ToolbarButton(browseButtonAction, false);
		ImageUtilities imageUtilities = PluginWorkspaceProvider.getPluginWorkspace().getImageUtilities();
		URL resource = getClass().getResource(ImageConstants.FILE_CHOOSER_ICON);
		if (resource != null) {
		  ImageIcon icon = (ImageIcon) imageUtilities.loadIcon(resource);
		  browseButton.setIcon(icon);
		}
		browseButton.setToolTipText(translator.getTranslation(Tags.BROWSE_BUTTON_TOOLTIP));
		browseButton.setOpaque(false);

		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 2;
		gbc.gridy = 1;
		panel.add(browseButton, gbc);

		informationLabel = new JLabel();
		informationLabel.setForeground(Color.RED);
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 3;
		panel.add(informationLabel, gbc);

		this.add(panel, BorderLayout.NORTH);
	}

	@Override
	protected void doOK() {
	  boolean doOK = false;
		final String selectedPath = (String) destinationPathCombo.getSelectedItem();
		try {
			final URL url = new URL(sourceUrlTextField.getText());
			final File file = new File(selectedPath);
			if (!destinationPathIsValid(file)) {
				return;
			}
			
			// Progress dialog.
	    final ProgressDialog progressDialog = new ProgressDialog(
	        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());

	    new CloneWorker(progressDialog, url, file).execute();
	    
	    // Make sure we present the dialog after this one is closed.
	    // TODO There is a progress dialog support in Java. Maybe is better to use that.
	    SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          progressDialog.setVisible(true);
        }
      });
	    
	    doOK = true;
		} catch (MalformedURLException e) {
			informationLabel.setText(translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_URL));
			this.pack();
		}
		
		if (doOK) {
		  // Close the dialog.
		  super.doOK();
		}

	}

	private boolean destinationPathIsValid(final File file) {
		if (file.exists()) {
			if (file.list().length > 0) {
				CloneRepositoryDialog.this.setVisible(true);
				this.setMinimumSize(new Dimension(400, 190));
				informationLabel.setText(
						HTML_START_TAG 
						+ translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY) 
						+ HTML_END_TAG);
				return false;
			}
		} else {
			File tempFile = file.getParentFile();
			while (tempFile != null) {
				if (tempFile.exists()) {
					file.mkdirs();
					break;
				}
				tempFile = tempFile.getParentFile();
			}
			if (tempFile == null) {
				CloneRepositoryDialog.this.setVisible(true);
				this.setMinimumSize(new Dimension(400, 180));
				informationLabel.setText(
						HTML_START_TAG 
						+ translator.getTranslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH) 
						+ HTML_END_TAG);
				return false;
			}
		}
		return true;
	}

}
