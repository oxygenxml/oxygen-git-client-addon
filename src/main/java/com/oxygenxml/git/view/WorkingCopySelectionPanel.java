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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.oxygenxml.git.constants.Constants;
import com.oxygenxml.git.constants.ImageConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.entities.FileStatus;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.ui.Icons;

/**
 * Panel containing a label with showing the current working copy, a combo box
 * used for selected other working copies and a browse button to add new working
 * copies
 * 
 * @author Beniamin Savu
 *
 */
public class WorkingCopySelectionPanel extends JPanel {

	/**
	 * Label for the working copy selector, informing the user on what working
	 * copy he is
	 */
	private JLabel label;

	/**
	 * A combo box for the user to change his working copy
	 */
	private JComboBox<String> workingCopySelector;

	/**
	 * A file system browser for the user to add new git repositories to the combo
	 * box
	 */
	private JButton browseButton;

	/**
	 * The git API, containing the commands
	 */
	private GitAccess gitAccess;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private Translator translator;

	public WorkingCopySelectionPanel(GitAccess gitAccess, Translator translator) {
		this.translator = translator;
		this.gitAccess = gitAccess;
	}

	public JComboBox<String> getWorkingCopySelector() {
		return workingCopySelector;
	}

	public JButton getBrowseButton() {
		return browseButton;
	}

	/**
	 * Creates the components and adds listeners to some of them. Basically this
	 * creates the panel
	 */
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

	/**
	 * Adds a state change listener on the working copy selector combo box. When a
	 * new working copy is selected this listener will execute
	 */
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

						// whan a new working copy is selected clear the commit text area
						parent.getCommitPanel().clearCommitMessage();

						// checks what buttons to keep active and what buttons to deactivate
						if (gitAccess.getStagedFile().size() > 0) {
							parent.getCommitPanel().getCommitButton().setEnabled(true);
						} else {
							parent.getCommitPanel().getCommitButton().setEnabled(false);
						}
						parent.getUnstagedChangesPanel().getStageSelectedButton().setEnabled(false);
						parent.getStagedChangesPanel().getStageSelectedButton().setEnabled(false);

						// calculate the how many pushes ahead and pulls behind the current
						// selected working copy is from the base. It is on thread because
						// the fetch command takes a longer time
						new Thread(new Runnable() {

							public void run() {
								gitAccess.fetch();
								parent.getToolbarPanel().setPullsBehind(GitAccess.getInstance().getPullsBehind());
								parent.getToolbarPanel().setPushesAhead(GitAccess.getInstance().getPushesAhead());
								parent.getToolbarPanel().updateInformationLabel();
							}
						}).start();
					} catch (RepositoryNotFoundException ex) {
						// We are here if the selected Repository doesn't exists anymore
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
								PluginWorkspaceProvider.getPluginWorkspace()
										.showInformationMessage(translator.getTraslation(Tags.WORKINGCOPY_REPOSITORY_NOT_FOUND));
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

	/**
	 * Adds a file chooser on a button
	 * 
	 * @param button
	 *          - the button to add a file chooser on
	 */
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
						PluginWorkspaceProvider.getPluginWorkspace()
								.showInformationMessage(translator.getTraslation(Tags.WORKINGCOPY_NOT_GIT_DIRECTORY));
					}
				}
			}
		});

	}

	/**
	 * Adds the label to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
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

	/**
	 * Adds the combo box to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addWorkingCopySelector(GridBagConstraints gbc) {
		gbc.insets = new Insets(Constants.COMPONENT_TOP_PADDING, Constants.COMPONENT_LEFT_PADDING,
				Constants.COMPONENT_BOTTOM_PADDING, Constants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;

		workingCopySelector = new JComboBox<String>();
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		workingCopySelector.setRenderer(renderer);
		int height = (int) workingCopySelector.getPreferredSize().getHeight();
		workingCopySelector.setMinimumSize(new Dimension(10, height));

		// Populates the combo box with the previously added repositories. Basically
		// restore the state before the application was closed
		for (String repositoryEntry : OptionsManager.getInstance().getRepositoryEntries()) {
			workingCopySelector.addItem(repositoryEntry);
		}
		String repositoryPath = OptionsManager.getInstance().getSelectedRepository();
		try {
			if (!repositoryPath.equals("")) {
				workingCopySelector.setSelectedItem(repositoryPath);
				gitAccess.setRepository(repositoryPath);
			} else if (workingCopySelector.getItemCount() > 0) {
				workingCopySelector.setSelectedIndex(0);
				gitAccess.setRepository((String) workingCopySelector.getSelectedItem());
			}
		} catch (IOException e) {
			// We are here if between the starts of the application the last selected
			// repository has been deleted

			// Removes that repository from the combo box and the option file. If the
			// combo box still has some repositories, it will select the one
			// positioned on index 0, otherwise it will clear everything.
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
			PluginWorkspaceProvider.getPluginWorkspace()
					.showInformationMessage(translator.getTraslation(Tags.WORKINGCOPY_LAST_SELECTED_REPOSITORY_DELETED));
		}
		this.add(workingCopySelector, gbc);
	}

	/**
	 * Adds the browse button to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 * 
	 */
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
		browseButton.setIcon(Icons.getIcon(ImageConstants.FILE_CHOOSER_ICON));
		browseButton.setToolTipText(translator.getTraslation(Tags.BROWSE_BUTTON_TOOLTIP));
		JToolBar browswtoolbar = new JToolBar();
		browswtoolbar.add(browseButton);
		browswtoolbar.setFloatable(false);
		browswtoolbar.setOpaque(false);
		this.add(browswtoolbar, gbc);
	}

	/**
	 * 
	 * Renderer for the combo box. Displaying only the folder project. Not the
	 * full path to the folder project
	 * 
	 * @author Beniamin Savu
	 *
	 */
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
