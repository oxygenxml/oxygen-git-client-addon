package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.ibm.icu.text.MessageFormat;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * 
 * Dialog used for detecting and selecting git submodules
 * 
 * @author Beniamin Savu
 *
 */
@SuppressWarnings("java:S110")
public class SubmoduleSelectDialog extends OKCancelDialog {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(SubmoduleSelectDialog.class);

	/**
	 * Combo box for showing and selectind the submodule
	 */
	private JComboBox<String> submoduleList;

	/**
	 * The translator for the messages that are displayed in this dialog
	 */
	private static Translator translator = Translator.getInstance();

	/**
	 * Constructor.
	 */
	public SubmoduleSelectDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		    translator.getTranslation(Tags.SUBMODULE_DIALOG_TITLE), true);

		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addLabel(gbc);
		addSubmoduleSelectCombo(gbc);

		this.setMinimumSize(new Dimension(320, 140));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setVisible(true);
	}

	/**
	 * Populates the combo box with data and adds it to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addSubmoduleSelectCombo(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		submoduleList = new JComboBox<>();
		submoduleList.setPreferredSize(new Dimension(200, submoduleList.getPreferredSize().height));
		for (String submodule : GitAccess.getInstance().getSubmoduleAccess().getSubmodules()) {
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
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JLabel label = new JLabel(translator.getTranslation(Tags.SUBMODULE_DIALOG_SUBMODULE_SELECTION_LABEL));
		getContentPane().add(label, gbc);
	}

	/**
	 * Sets as the current working copy the submodule repository selected from the
	 * combo box
	 */
	@Override
	protected void doOK() {
		super.doOK();
		String submodule = (String) submoduleList.getSelectedItem();
		try {
			GitAccess.getInstance().setSubmodule(submodule);
		} catch (IOException | GitAPIException e) {
      String message = e.getMessage();
      if (message == null) {
        // The exception's toString() adds more data, like the class name.
        message = e.toString();
      }
      String formated = MessageFormat.format(translator.getTranslation(Tags.SUBMODULE_LOAD_FAIL), message);
      
      PluginWorkspaceProvider.getPluginWorkspace().showErrorMessage(formated);
			if (logger.isDebugEnabled()) {
				logger.debug(e, e);
			}
		}
	}
}
