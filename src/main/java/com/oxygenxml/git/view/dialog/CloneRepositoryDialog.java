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
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;

import com.oxygenxml.git.CustomAuthenticator;
import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.options.UserCredentials;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Refresh;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

public class CloneRepositoryDialog extends OKCancelDialog {
	
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
			CustomAuthenticator.bind(url.getHost());
			GitAccess.getInstance().clone(url, file, progressDialog);
			progressDialog.dispose();
			return null;
		}

		@Override
		protected void done() {
			try {
				get();
				refresh.call();
				OptionsManager.getInstance().saveDestinationPath(file.getAbsolutePath());
				dispose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				progressDialog.dispose();
				Throwable cause = e.getCause();
				while (cause != null) {
					if (cause.getMessage().contains("Download cancelled")) {
						try {
							FileUtils.cleanDirectory(file);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					}
					if (cause instanceof NoRemoteRepositoryException) {
						CloneRepositoryDialog.this.setVisible(true);
						CloneRepositoryDialog.this.setMinimumSize(new Dimension(400, 190));
						information.setText("<html>"
								+ translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_URL_IS_NOT_A_REPOSITORY) + "</html>");
						break;
					}
					if (cause instanceof org.eclipse.jgit.errors.TransportException) {
						UserCredentials userCredentials = new LoginDialog(
								(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								translator.getTraslation(Tags.LOGIN_DIALOG_TITLE), true, url.getHost(),
								translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_LOGIN_MESSAGE), translator)
										.getUserCredentials();
						if (userCredentials != null) {
							doOK();
						}
						break;
					}
					cause = cause.getCause();
				}
				return;
			} finally {
				CustomAuthenticator.unbind(url.getHost());
			}
		}
	}

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private Translator translator;

	private JTextField tfURL;

	private JComboBox<String> comboBoxPath;

	private ToolbarButton browseButton;

	private JLabel information;

	/**
	 * Main panel refresh
	 */
	private Refresh refresh;

	public CloneRepositoryDialog(JFrame parentFrame, String title, boolean modal, Translator translator,
			Refresh refresh) {
		super(parentFrame, title, modal);
		this.translator = translator;
		this.refresh = refresh;

		createGUI();

		this.pack();
		this.setLocationRelativeTo(parentFrame);
		this.setMinimumSize(new Dimension(400, 160));
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

	private void createGUI() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JLabel lblURL = new JLabel(translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_URL_LABEL));
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(lblURL, gbc);

		tfURL = new JTextField();
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		panel.add(tfURL, gbc);

		JLabel lblPath = new JLabel(translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_LABEL));
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 1;
		panel.add(lblPath, gbc);

		comboBoxPath = new JComboBox<String>();
		comboBoxPath.setEditable(true);
		Set<String> destinationPaths = OptionsManager.getInstance().getDestinationPaths();
		for (String string : destinationPaths) {
			comboBoxPath.addItem(string);
		}
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		panel.add(comboBoxPath, gbc);

		Action browseButtonAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					comboBoxPath.setSelectedItem(directory.getAbsolutePath());
				}
			}
		};
		browseButton = new ToolbarButton(browseButtonAction, false);
		browseButton.setIcon(Icons.getIcon(ImageConstants.FILE_CHOOSER_ICON));
		browseButton.setToolTipText(translator.getTraslation(Tags.BROWSE_BUTTON_TOOLTIP));
		browseButton.setOpaque(false);

		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 2;
		gbc.gridy = 1;
		panel.add(browseButton, gbc);

		information = new JLabel();
		information.setForeground(Color.RED);
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 3;
		panel.add(information, gbc);

		this.add(panel, BorderLayout.NORTH);
	}

	@Override
	protected void doOK() {

		final String selectedPath = (String) comboBoxPath.getSelectedItem();
		try {
			final URL url = new URL(tfURL.getText());
			final File file = new File(selectedPath);
			if (destinationPathIsValid(file)) {
				return;
			}
			cloneRepository(url, file);

		} catch (MalformedURLException e) {
			CloneRepositoryDialog.this.setVisible(true);
			this.setMinimumSize(new Dimension(400, 180));
			information.setText("<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_URL) + "</html>");
			return;
		}

	}

	private void cloneRepository(final URL url, final File file) {
		final ProgressDialog progressDialog = new ProgressDialog(
				(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		new Thread(new Runnable() {

			public void run() {
				progressDialog.setVisible(true);
			}
		}).start();
		new CloneWorker(progressDialog, url, file).execute();

	}

	private boolean destinationPathIsValid(final File file) {
		if (file.exists()) {
			if (file.list().length > 0) {
				CloneRepositoryDialog.this.setVisible(true);
				this.setMinimumSize(new Dimension(400, 190));
				information.setText(
						"<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY) + "</html>");
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
				information.setText(
						"<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH) + "</html>");
				return false;
			}
		}
		return true;
	}

}
