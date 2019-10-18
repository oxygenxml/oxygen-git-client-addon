package com.oxygenxml.git.view.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URL;
import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.images.ImageUtilities;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;

/**
 * Show pull status and corresponding files.
 * 
 * @author Beniamin Savu
 *
 */
public class PullStatusAndFilesDialog extends OKCancelDialog {

	public PullStatusAndFilesDialog(String title, Collection<String> conflictFiles, String message) {
		super(
		    PluginWorkspaceProvider.getPluginWorkspace() != null ? 
		        (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null,
		    title,
		    true);
		JLabel label = new JLabel("<html>" + message + "</html>");


		// populating the JList with the conflict files
		DefaultListModel<String> model = new DefaultListModel<>();
		for (String fileName : conflictFiles) {
			File file = new File(fileName);
			String listElement = file.getName();
			if (file.getParent() != null) {
				listElement += " - ";
				listElement += file.getParent();
			}
			model.addElement(listElement);
		}
		JList<String> filesInConflictList = new JList<>(model);
		JScrollPane scollPane = new JScrollPane(filesInConflictList);
		scollPane.setPreferredSize(new Dimension(scollPane.getPreferredSize().width, 50));
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel iconLabel = new JLabel();
        iconLabel.setIcon(Icons.getIcon(Icons.WARNING_ICON));
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 2;
		panel.add(iconLabel, gbc);
		
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		panel.add(label, gbc);
		
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
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
		
		this.setResizable(true);
		this.setMinimumSize(new Dimension(420, 220));
		this.setDefaultCloseOperation(OKCancelDialog.DISPOSE_ON_CLOSE);
		this.pack();
		if (PluginWorkspaceProvider.getPluginWorkspace() != null) {
		  this.setLocationRelativeTo((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame());
		}
		this.setVisible(true);
	}

}
