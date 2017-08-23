package com.oxygenxml.git.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class AddRemoteDialog extends OKCancelDialog{

	private Translator translator;
	
	private JTextField remoteNameTextField;
	
	private JTextField remoteRepoTextField;
	
	public AddRemoteDialog(JFrame parentFrame, String title, boolean modal, Translator translator) {
		super(parentFrame, title, modal);
		this.translator = translator;
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addInformationLabel(gbc);
		//addRemoteNameLabel(gbc);
		//addRemoteNameTextField(gbc);
		addRemoteRepoLabel(gbc);
		addRemoteRepoTextField(gbc);
		
		this.pack();
		this.setLocationRelativeTo(parentFrame);
		this.setMinimumSize(new Dimension(320, 130));
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

	private void addInformationLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		JLabel label = new JLabel(translator.getTraslation(Tags.ADD_REMOTE_DIALOG_INFO_LABEL));
		getContentPane().add(label, gbc);
	}

	private void addRemoteRepoTextField(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		remoteRepoTextField = new JTextField();
		getContentPane().add(remoteRepoTextField, gbc);
	}

	private void addRemoteRepoLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		JLabel label = new JLabel(translator.getTraslation(Tags.ADD_REMOTE_DIALOG_ADD_REMOTE_REPO_LABEL));
		getContentPane().add(label, gbc);
	}

	private void addRemoteNameTextField(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		remoteNameTextField = new JTextField();
		getContentPane().add(remoteNameTextField, gbc);
	}

	private void addRemoteNameLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		JLabel label = new JLabel(translator.getTraslation(Tags.ADD_REMOTE_DIALOG_ADD_REMOTE_NAME_LABEL));
		getContentPane().add(label, gbc);
	}

	@Override
	protected void doOK() {
		super.doOK();
		StoredConfig config;
		try {
			config = GitAccess.getInstance().getRepository().getConfig();
			RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
			URIish uri = new URIish(remoteRepoTextField.getText());
			remoteConfig.addURI(uri);
			RefSpec spec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
			remoteConfig.addFetchRefSpec(spec);
			remoteConfig.update(config);
			config.save();
		} catch (NoRepositorySelected e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dispose();
	}
}
