package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.Refresh;

import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * 
 * Dialog used for detecting and selecting git submodules
 * 
 * @author Beniamin Savu
 *
 */
public class SubmoduleSelectDialog extends OKCancelDialog {

	/**
	 * Combo box for showing and selectind the submodule
	 */
	private JComboBox<String> submoduleList;

	/**
	 * Main panel refresh
	 */
	private Refresh refresh;

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private Translator translator;

	public SubmoduleSelectDialog(JFrame parentFrame, String title, boolean modal, Refresh refresh,
			Translator translator) {
		super(parentFrame, title, modal);
		this.refresh = refresh;
		this.translator = translator;

		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addLabel(gbc);
		addSubmoduleSelectCombo(gbc);

		this.pack();
		this.setLocationRelativeTo(parentFrame);
		this.setMinimumSize(new Dimension(320, 140));
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

	/**
	 * Populates the combo box with data and adds it to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addSubmoduleSelectCombo(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		submoduleList = new JComboBox<String>();
		for (String submodule : GitAccess.getInstance().getSubmodules()) {
			submoduleList.addItem(submodule);
		}
		getContentPane().add(submoduleList, gbc);
	}

	/**
	 * Adds the label to the label on the left side of the combo box
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
		JLabel label = new JLabel(translator.getTraslation(Tags.SUBMODULE_DIALOG_SUBMODULE_SELECTION_LABEL));
		getContentPane().add(label, gbc);
	}

	/**
	 * Sets as the current working copy the submodule repository selected from the
	 * combo box
	 */
	protected void doOK() {
		String submodule = (String) submoduleList.getSelectedItem();
		try {
			GitAccess.getInstance().setSubmodule(submodule);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		refresh.call();
		dispose();
	}
}
