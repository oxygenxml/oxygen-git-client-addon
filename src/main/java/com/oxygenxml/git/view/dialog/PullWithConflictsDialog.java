package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.ui.Icons;

/**
 * A dialog that is shown when the pull is successful but has conflicts. It
 * shows an infromation message and the files that are in conflict
 * 
 * @author Beniamin Savu
 *
 */
public class PullWithConflictsDialog extends OKCancelDialog {

	public PullWithConflictsDialog(String title, Collection<String> conflictFiles, String message) {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(), title, true);
		JLabel label = new JLabel("<html>" + message + "</html>");


		// populating the JList with the conflict files
		DefaultListModel<String> model = new DefaultListModel<String>();
		for (String fileName : conflictFiles) {
			File file = new File(fileName);
			String listElement = file.getName();
			if (file.getParent() != null) {
				listElement += " - ";
				listElement += file.getParent();
			}
			model.addElement(listElement);
		}
		JList<String> filesInConflictList = new JList<String>(model);
		JScrollPane scollPane = new JScrollPane(filesInConflictList);
		scollPane.setPreferredSize(new Dimension(scollPane.getPreferredSize().width, 50));
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel iconLabel = new JLabel();
		iconLabel.setIcon(Icons.getIcon(ImageConstants.WARNING_ICON));
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		panel.add(iconLabel, gbc);
		
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		panel.add(label, gbc);
		
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		panel.add(scollPane, gbc);
		
		getContentPane().add(panel);
		getCancelButton().setVisible(false);
		
		this.pack();
		this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		this.setResizable(true);
		this.setMinimumSize(new Dimension(420, 220));
		this.setVisible(true);
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
	}

}
