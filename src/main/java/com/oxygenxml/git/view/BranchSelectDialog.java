package com.oxygenxml.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Refresh;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class BranchSelectDialog extends OKCancelDialog {

	private JComboBox<String> branchesList;
	
	private JLabel information;

	private Refresh refresh;
	
	private Translator translator;

	public BranchSelectDialog(JFrame parentFrame, String title, boolean modal, Refresh refresh, Translator translator) {
		super(parentFrame, title, modal);
		this.refresh = refresh;
		this.translator = translator;

		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addLabel(gbc);
		addBranchSelectCombo(gbc);
		addInformationLabel(gbc);

		this.pack();
		this.setLocationRelativeTo(parentFrame);
		this.setMinimumSize(new Dimension(320, 140));
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
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 2;
		information = new JLabel();
		getContentPane().add(information, gbc);
	}

	private void addBranchSelectCombo(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		branchesList = new JComboBox<String>();
		for (Ref branch : GitAccess.getInstance().getBrachList()) {
			String name = branch.getName();
			name = name.substring(name.lastIndexOf("/") + 1);
			branchesList.addItem(name);
		}
		branchesList.setSelectedItem(GitAccess.getInstance().getCurrentBranch());

		getContentPane().add(branchesList, gbc);
	}

	private void addLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JLabel label = new JLabel(translator.getTraslation(Tags.BRANCH_DIALOG_BRANCH_SELECTION_LABEL));
		getContentPane().add(label, gbc);
	}

	protected void doOK() {
		String selectedBranch = (String) branchesList.getSelectedItem();
		try {
			GitAccess.getInstance().setBranch(selectedBranch);
		} catch (RefAlreadyExistsException e) {
			e.printStackTrace();
		} catch (RefNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidRefNameException e) {
			e.printStackTrace();
		} catch (CheckoutConflictException e) {
			information.setText(translator.getTraslation(Tags.CHANGE_BRANCH_ERROR_MESSAGE));
			return;
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		refresh.call();
		dispose();
	}

}
