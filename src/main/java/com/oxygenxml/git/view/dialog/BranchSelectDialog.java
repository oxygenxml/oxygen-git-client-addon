package com.oxygenxml.git.view.dialog;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.BranchInfo;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.GitOperationScheduler;
import com.oxygenxml.git.utils.GitRefreshSupport;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog used for detecting and selecting Git branches.
 * 
 * @author Beniamin Savu
 *
 */
public class BranchSelectDialog extends OKCancelDialog {

	/**
	 * Logger for logging.
	 */
	private static Logger logger = Logger.getLogger(BranchSelectDialog.class);

	/**
	 * The combo box with all the branches for a repository.
	 */
	private JComboBox<String> branchesCombo;

	/**
	 * Git refresh support.
	 */
	private GitRefreshSupport gitRefreshSupport;

	/**
	 * The translator for i18n.
	 */
	private static Translator translator = Translator.getInstance();
	
	/**
	 * Plugin workspace access.
	 */
	private static StandalonePluginWorkspace pluginWS = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();

	/**
	 * Constructor.
	 * 
	 * @param parentFrame  The parent frame.
	 * @param title        The dialog's title.
	 */
	public BranchSelectDialog(GitRefreshSupport refresh) {
		super(
		    (JFrame) pluginWS.getParentFrame(),
		    translator.getTranslation(Tags.BRANCH_SELECTION_DIALOG_TITLE),
		    true);
		this.gitRefreshSupport = refresh;

		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		addSelectBranchLabel(gbc);
		addBranchSelectCombo(gbc);

		this.setMinimumSize(new Dimension(320, 140));
		this.setResizable(true);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo((Component) pluginWS.getParentFrame());
		this.setModal(true);
		this.setVisible(true);
	}

	/**
	 * Populates the combo box with data and adds it to the dialog
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addBranchSelectCombo(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		branchesCombo = new JComboBox<>();
		branchesCombo.setPreferredSize(new Dimension(200, branchesCombo.getPreferredSize().height));
		List<String> branches = new ArrayList<>();
		for (Ref branch : GitAccess.getInstance().getLocalBranchList()) {
			String name = branch.getName();
			name = name.replace("refs/heads/", "");
			branchesCombo.addItem(name);
			branches.add(name);
		}
		BranchInfo branchInfo = GitAccess.getInstance().getBranchInfo();
		if (!branchInfo.isDetached()) {
			if (branches.contains(branchInfo.getBranchName())) {
				branchesCombo.setSelectedItem(GitAccess.getInstance().getBranchInfo().getBranchName());
			} else {
				branchesCombo.addItem(GitAccess.getInstance().getBranchInfo().getBranchName());
				branchesCombo.setSelectedItem(GitAccess.getInstance().getBranchInfo().getBranchName());
			}
		}

		getContentPane().add(branchesCombo, gbc);
	}

	/**
	 * Adds the label on the left side of the combo box
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addSelectBranchLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		JLabel label = new JLabel(translator.getTranslation(Tags.BRANCH_DIALOG_BRANCH_SELECTION_LABEL));
		getContentPane().add(label, gbc);
	}

	/**
	 * Sets as current branch the branch that the user selects from the combo box
	 */
	@Override
	protected void doOK() {
	  BranchSelectDialog.this.getLayeredPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	  // Disable the widgets to avoid another input from the user.
    getOkButton().setEnabled(false);
    getCancelButton().setEnabled(false);
    branchesCombo.setEnabled(false);
    
	  GitOperationScheduler.getInstance().schedule(() -> {
	    try {
	      GitAccess.getInstance().setBranch((String) branchesCombo.getSelectedItem());
	      gitRefreshSupport.call();
	    } catch (CheckoutConflictException e) {
	      logger.debug(e, e);
	      showErrorMessage(translator.getTranslation(Tags.COMMIT_CHANGES_BEFORE_CHANGING_BRANCH));
	    } catch (GitAPIException e) {
	      logger.debug(e, e);
	      showErrorMessage(e.getMessage());
	    } finally {
	      // Finish the initial doOK action and let the dialog close.
	      SwingUtilities.invokeLater(BranchSelectDialog.super::doOK);
	    }
	  });
	}

	/**
	 * Show error message.
	 * 
	 * @param message The error message.
	 */
  private void showErrorMessage(String message) {
    try {
      SwingUtilities.invokeAndWait(() -> pluginWS.showErrorMessage(message));
    } catch (InvocationTargetException e1) {
      logger.debug(e1, e1);
    } catch (InterruptedException e1) {
      logger.debug(e1, e1);
    }
  }

}
