package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import com.oxygenxml.sdksamples.workspace.git.constants.Constants;
import com.oxygenxml.sdksamples.workspace.git.jaxb.entities.RepositoryOption;
import com.oxygenxml.sdksamples.workspace.git.utils.Folder;
import com.oxygenxml.sdksamples.workspace.git.utils.OptionsManager;

public class WorkingCopySelectionPanel extends JPanel {

	private JLabel label;
	private JComboBox<String> workingCopySelector;
	private JButton browseButton;

	public WorkingCopySelectionPanel() {
		init();
	}

	private void init() {
		this.setBorder(BorderFactory.createTitledBorder("WorkingCopy"));

		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addWorkingCopySelector(gbc);
		addBrowseButton(gbc);

		addFileChooserOn(browseButton);
		//addWorkingCopySelectorListener();

	}

	public void addWorkingCopySelectorListener() {
		
		final StagingPanel parent = (StagingPanel) this.getParent();
		
		workingCopySelector.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String path = (String) workingCopySelector.getSelectedItem();
					OptionsManager.getInstance().saveSelectedRepository(path);
					Folder folder = new Folder();
					List<String> fileNames = folder.search(path);
					FileTableModel model = (FileTableModel) parent.getUnstagedChangesPanel().getFilesTable().getModel();
					model.setFileNames(fileNames);
				}
			}
		});

	}

	private void addFileChooserOn(JButton button) {
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Choose a directory to open your repository: ");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int returnValue = fileChooser.showOpenDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					if (fileChooser.getSelectedFile().isDirectory()) {
						String directoryPath = fileChooser.getSelectedFile().getAbsolutePath();

						RepositoryOption repositoryOption = new RepositoryOption(directoryPath);

						if (!OptionsManager.getInstance().getRepositoryEntries().contains(repositoryOption)) {
							workingCopySelector.addItem(directoryPath);
							OptionsManager.getInstance().addRepository(repositoryOption);
							
						}

						workingCopySelector.setSelectedItem(directoryPath);
					}
				}
			}
		});

	}

	private void addLabel(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		label = new JLabel("Working copy: ");
		this.add(label, gbc);

	}

	private void addWorkingCopySelector(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;

		workingCopySelector = new JComboBox<String>();

		for (RepositoryOption repositoryOption : OptionsManager.getInstance().getRepositoryEntries()) {
			workingCopySelector.addItem(repositoryOption.getLocation());
		}
		this.add(workingCopySelector, gbc);
	}

	private void addBrowseButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		browseButton = new JButton("Browse");
		this.add(browseButton, gbc);
	}

}
