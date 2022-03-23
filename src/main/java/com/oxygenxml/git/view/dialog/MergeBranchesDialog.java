package com.oxygenxml.git.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.TextFormatUtil;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Dialog for merge branch "A" into merge "B".
 * 
 * @author alex_smarandache
 *
 */
public class MergeBranchesDialog extends OKCancelDialog {
  
  /**
   * Preferred height for this dialog.
   */
  private static final int HEIGHT = 200;
  
  /**
   * Preferred width for this dialog.
   */
  private static final int WIDTH = 600;

	/**
	 * Translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * Squash info.
	 */
	private final JTextArea squashInfo;
	
	/**
	 * Merge info.
	 */
	private final JTextArea mergeInfo;
	
	/**
	 * If is selected, the action "Squash and merge" will be performed, else just "Merge".
	 */
	private final JCheckBox squashOption = new JCheckBox(TRANSLATOR.getTranslation(Tags.SQUASH_MERGE));
	

	
	/**
	 * Constructor.
	 *  
	 * @param selectedBranch The selected branch by user.
	 * 
	 */
	public MergeBranchesDialog(final String currentBranch, final String selectedBranch) {
		super(
				(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.MERGE_BRANCHES),
				true);
		
		mergeInfo = UIUtil.createMessageArea(MessageFormat.format(
        TRANSLATOR.getTranslation(Tags.MERGE_INFO),
        TextFormatUtil.shortenText(selectedBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "..."),
        TextFormatUtil.shortenText(currentBranch, UIConstants.BRANCH_NAME_MAXIMUM_LENGTH, 0, "...")));
		
		squashInfo = UIUtil.createMessageArea(TRANSLATOR.getTranslation(Tags.SQUASH_MERGE_INFO));
		
		createGUI();
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setVisible(true);
	}
	
	/**
	 * Adds to the dialog the labels and the text fields.
	 */
	public void createGUI() {
		final JPanel panel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;

		gbc.insets = new Insets(0, squashOption.getInsets().left, 0, 0);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		panel.add(mergeInfo, gbc);

		// Added squash check box
		gbc.gridy ++;
		gbc.gridwidth = 1;
	  gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, 0, 0, 0);
		panel.add(squashOption, gbc);

	  // Added squash info label
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, squashOption.getInsets().left, 0, 0);
		gbc.gridx = 0;
		gbc.gridwidth = 2;
	  gbc.weightx = 1;
	  gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy ++;
		panel.add(squashInfo, gbc);

		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy ++;
		panel.add(new JPanel(), gbc);
    
		this.add(panel, BorderLayout.CENTER);

		setOkButtonText(TRANSLATOR.getTranslation(Tags.MERGE));	    
	}

	
	/**
	 * @return <code>true</code> if the squash option is also selected.
	 */
	public boolean isSquashSelected() {
	  return squashOption.isSelected();
	}
	

}
