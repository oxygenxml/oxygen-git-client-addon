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

import org.eclipse.jgit.lib.Ref;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.utils.Refresh;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

public class BranchSelectDialog extends OKCancelDialog {

	private JComboBox<String> branchesList;

	private Refresh refresh;

	public BranchSelectDialog(JFrame parentFrame, String title, boolean modal, Refresh refresh) {
		super(parentFrame, title, modal);
		this.refresh = refresh;

		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addLabel(gbc);
		addBranchSelectCombo(gbc);

		this.pack();
		this.setLocationRelativeTo(parentFrame);
		this.setMinimumSize(new Dimension(300, 120));
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
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
		JLabel label = new JLabel("Branch: ");
		getContentPane().add(label, gbc);
	}

	protected void doOK() {
		String selectedBranch = (String) branchesList.getSelectedItem();
		GitAccess.getInstance().setBranch(selectedBranch);
		refresh.call();
		dispose();
	}

}
