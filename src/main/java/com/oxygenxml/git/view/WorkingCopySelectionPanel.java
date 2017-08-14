package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.utils.OptionsManager;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

public class WorkingCopySelectionPanel extends JPanel {

	private JLabel label;
	private JComboBox<String> workingCopySelector;
	private JButton browseButton;
	private GitAccess gitAccess;
	private Translator translator;

	public WorkingCopySelectionPanel(GitAccess gitAccess, Translator translator) {
		this.translator = translator;
		this.gitAccess = gitAccess;
	}

	public JComboBox<String> getWorkingCopySelector() {
		return workingCopySelector;
	}

	public void setWorkingCopySelector(JComboBox<String> workingCopySelector) {
		this.workingCopySelector = workingCopySelector;
	}

	public JButton getBrowseButton() {
		return browseButton;
	}

	public void setBrowseButton(JButton browseButton) {
		this.browseButton = browseButton;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addWorkingCopySelector(gbc);
		addBrowseButton(gbc);

		addFileChooserOn(browseButton);
		addWorkingCopySelectorListener();

		this.setMinimumSize(new Dimension(Constants.PANEL_WIDTH, Constants.WORKINGCOPY_PANEL_HEIGHT));
	}

	private void addWorkingCopySelectorListener() {

		final StagingPanel parent = (StagingPanel) this.getParent();

		workingCopySelector.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					// get and save the selected Option so that at restart the same
					// repository will be selected
					String path = (String) workingCopySelector.getSelectedItem();

					try {

						gitAccess.setRepository(path);
						OptionsManager.getInstance().saveSelectedRepository(path);
						List<FileStatus> unstagedFiles = gitAccess.getUnstagedFiles();
						List<FileStatus> stagedFiles = gitAccess.getStagedFile();

						// generate content for FLAT_VIEW
						parent.getUnstagedChangesPanel().updateFlatView(unstagedFiles);
						parent.getStagedChangesPanel().updateFlatView(stagedFiles);

						// generate content for TREE_VIEW
						parent.getUnstagedChangesPanel().createTreeView(path, unstagedFiles);
						parent.getStagedChangesPanel().createTreeView(path, stagedFiles);

						parent.getCommitPanel().clearCommitMessage();
						if (gitAccess.getStagedFile().size() > 0) {
							parent.getCommitPanel().getCommitButton().setEnabled(true);
						} else {
							parent.getCommitPanel().getCommitButton().setEnabled(false);
						}
						parent.getUnstagedChangesPanel().getStageSelectedButton().setEnabled(false);
						parent.getStagedChangesPanel().getStageSelectedButton().setEnabled(false);

						SwingUtilities.invokeLater(new Runnable() {

							public void run() {
								gitAccess.fetch();
								parent.getToolbarPanel().setPullsBehind(GitAccess.getInstance().getPullsBehind());
								parent.getToolbarPanel().setPushesAhead(GitAccess.getInstance().getPushesAhead());
								parent.getToolbarPanel().updateInformationLabel();
							}
						});
					} catch (RepositoryNotFoundException ex) {
						OptionsManager.getInstance().removeSelectedRepository(path);
						if (workingCopySelector.getItemCount() > 0) {
							workingCopySelector.setSelectedItem(0);
						} else {
							workingCopySelector.setSelectedItem(null);
							gitAccess.close();
						}
						workingCopySelector.removeItem(path);

						// clear content from FLAT_VIEW
						parent.getUnstagedChangesPanel().updateFlatView(new ArrayList<FileStatus>());
						parent.getStagedChangesPanel().updateFlatView(new ArrayList<FileStatus>());

						// clear content from TREE_VIEW
						parent.getUnstagedChangesPanel().createTreeView("", new ArrayList<FileStatus>());
						parent.getStagedChangesPanel().createTreeView("", new ArrayList<FileStatus>());
						SwingUtilities.invokeLater(new Runnable() {

							public void run() {
								JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
										"The selected repository was not found");

							}
						});
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"Could not load the repository");
					}
				}
			}
		});

	}

	private void addFileChooserOn(JButton button) {
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					String directoryPath = directory.getAbsolutePath();
					if (FileHelper.isGitRepository(directoryPath) && directoryPath != null) {

						if (!OptionsManager.getInstance().getRepositoryEntries().contains(directoryPath)) {
							workingCopySelector.addItem(directoryPath);
							OptionsManager.getInstance().addRepository(directoryPath);
						}
						workingCopySelector.setSelectedItem(directoryPath);
					} else {
						JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
								"Please select a git directory");
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
		label = new JLabel(translator.getTraslation(Tags.WORKING_COPY_LABEL));
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
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		workingCopySelector.setRenderer(renderer);
		workingCopySelector.setMinimumSize(new Dimension(10, 20));

		for (String repositoryEntry : OptionsManager.getInstance().getRepositoryEntries()) {
			workingCopySelector.addItem(repositoryEntry);
		}
		String repositoryPath = OptionsManager.getInstance().getSelectedRepository();
		try {
			if (!repositoryPath.equals("")) {
				workingCopySelector.setSelectedItem(repositoryPath);
				gitAccess.setRepository(repositoryPath);
			} else if(workingCopySelector.getItemCount() > 0){
				workingCopySelector.setSelectedIndex(0);
				gitAccess.setRepository((String) workingCopySelector.getSelectedItem());
			}
		} catch (IOException e) {
			OptionsManager.getInstance().removeSelectedRepository(repositoryPath);
			workingCopySelector.removeItem(repositoryPath);
			if (workingCopySelector.getItemCount() > 0) {
				workingCopySelector.setSelectedIndex(0);
				try {
					gitAccess.setRepository((String) workingCopySelector.getSelectedItem());
				} catch (RepositoryNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} else {
				workingCopySelector.setSelectedItem(null);
				gitAccess.close();
			}
			OptionsManager.getInstance().saveSelectedRepository("");

			JOptionPane.showMessageDialog((Component) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
					"Last selected repository not found. It may have been deleted");
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
		browseButton = new ToolbarButton(null, false);
		browseButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImageConstants.FILE_CHOOSER_ICON)));
		browseButton.setToolTipText(translator.getTraslation(Tags.BROWSE_BUTTON_TOOLTIP));
		JToolBar browswtoolbar = new JToolBar();
		browswtoolbar.add(browseButton);
		browswtoolbar.setFloatable(false);
		this.add(browswtoolbar, gbc);
	}

	class ComboboxToolTipRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			
			if (value != null) {
				comp.setToolTipText((String) value);
				String path = (String) value;
				path = path.replace("\\", "/");
				String rootFolder = path.substring(path.lastIndexOf("/") + 1);
				comp.setText(rootFolder);
			}
			return comp;
		}
	}

}
