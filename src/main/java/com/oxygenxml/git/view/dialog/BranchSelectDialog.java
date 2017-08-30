package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

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

/**
 * Dialog used for detecting and selecting git branches
 * 
 * @author Beniamin Savu
 *
 */
public class BranchSelectDialog extends OKCancelDialog {

	/**
	 * Shows the user a combo box with all the branches for that repository
	 */
	private JComboBox<String> branchesList;

	/**
	 * An information label that is displayed in case of some errors to let the
	 * user know what to do
	 */
	private JLabel information;

	/**
	 * Main panel refresh
	 */
	private Refresh refresh;

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
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

	/**
	 * Adds the information label to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
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

	/**
	 * Populates the combo box with data and adds it to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
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

	/**
	 * Adds the label on the left side of the combo box
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
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

	/**
	 * Sets as current branch the branch that the user selects from the combo box
	 */
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
