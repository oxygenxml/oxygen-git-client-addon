package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.CustomAuthenticator;
import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
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
	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private Translator translator;

	private JTextField tfURL;

	private JTextField tfPath;

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

		tfPath = new JTextField();
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 1;
		panel.add(tfPath, gbc);

		Action browseButtonAction = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					tfPath.setText(directory.getAbsolutePath());
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
		try {
			URL url = new URL(tfURL.getText());
			URI uri = url.toURI();
			File file = new File(tfPath.getText());
			if (file.exists()) {
				if (file.list().length > 0) {
					this.setMinimumSize(new Dimension(400, 190));
					information.setText(
							"<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_DESTINATION_PATH_NOT_EMPTY) + "</html>");
					return;
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
					this.setMinimumSize(new Dimension(400, 180));
					information.setText(
							"<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_DESTINATION_PATH) + "</html>");
					return;
				}
			}
			
			CustomAuthenticator.bind(url.getHost());
			try {
			  GitAccess.getInstance().clone(uri.toString(), file);
			} finally {
			  CustomAuthenticator.unbind(url.getHost());
			}
			
			refresh.call();
		} catch (MalformedURLException e) {
			this.setMinimumSize(new Dimension(400, 180));
			information.setText("<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_INVALID_URL) + "</html>");
			return;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			this.setMinimumSize(new Dimension(400, 180));
			information.setText("<html>" + translator.getTraslation(Tags.CLONE_REPOSITORY_DIALOG_CLONE_ERROR) + "</html>");
			return;
		}
		dispose();
	}

}
