package com.oxygenxml.git.view.staging;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.exceptions.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileUtil;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Panel containing a label with showing the current working copy, a combo box
 * used for selected other working copies and a browse button to add new working
 * copies.
 * 
 * @author Beniamin Savu
 *
 */
public class WorkingCopySelectionPanel extends JPanel {

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER =  LoggerFactory.getLogger(WorkingCopySelectionPanel.class);

	/**
	 * Clear history.
	 */
	private static final String CLEAR_HISTORY_ENTRY = "CLEAR_HISTORY";

	/**
	 * The git API, containing the commands
	 */
	private GitAccess gitAccess;

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * A combo box for the user to change his working copy
	 */
	private JComboBox<String> workingCopyCombo;

	/**
	 * A file system browser for the user to add new git repositories to the combo
	 * box
	 */
	private JButton browseButton;

	/**
	 * <code>true</code> to inhibit repository update when the selection changes in the combo.
	 * <code>false</code> to update the repository when the selection changes in the combo.
	 */
	private boolean inhibitRepoUpdate = false;

	/**
	 * <code>true</code> if the panel has a label attached.
	 */
	private final boolean isLabeled;


	/**
	 * Constructor.
	 * 
	 * @param gitController Git operations controller.
	 * @param isLabeled     <code>true</code> if the panel has a label attached.
	 */
	public WorkingCopySelectionPanel(GitControllerBase gitController, boolean isLabeled) {
		this.isLabeled = isLabeled;
		this.gitAccess = gitController.getGitAccess();
		createGUI();
		gitController.addGitListener(new GitEventUpdater());
		addHierarchyListener(new HierarchyListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent e
					) {
				if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
						&& WorkingCopySelectionPanel.this.isShowing()) {
					initializeWorkingCopyCombo();
					removeHierarchyListener(this);
				}
			}
		});
	}


	/**
	 * Creates the components and adds listeners to some of them. Basically this
	 * creates the panel
	 */
	private void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		if(isLabeled) {
			addLabel(gbc);
		}
		addWorkingCopySelector(gbc);
		addBrowseButton(gbc);

		addFileChooserListner();

	}


	/**
	 * Adds a state change listener on the working copy selector combo box. When a
	 * new working copy is selected this listener will execute
	 */
	private void addWorkingCopySelectorListener() {
		// NOSONAR
		workingCopyCombo.addItemListener(
				e -> {
					// Don't do anything if the event was originated by us.
					if (!inhibitRepoUpdate && e.getStateChange() == ItemEvent.SELECTED) {
						loadRepository();
					}
				});
	}

	/**
	 * Loads in the GitAccess the repository selected in the working copy.
	 */
	private void loadRepository() {
	  SwingUtilities.invokeLater(() -> {
	    inhibitRepoUpdate = true;
	    try {
	      // get and save the selected Option so that at restart the same
	      // repository will be selected
	      String selectedEntry = (String) workingCopyCombo.getSelectedItem();
	      if (LOGGER.isDebugEnabled()) {
	        LOGGER.debug("Selected working copy: " + selectedEntry);
	      }
	      if (CLEAR_HISTORY_ENTRY.equals(selectedEntry)) {

	        String[] options = new String[] {
	            "   " + TRANSLATOR.getTranslation(Tags.YES) + "   ",
	            "   " + TRANSLATOR.getTranslation(Tags.NO) + "   "};
	        int[] optionIds = new int[] { 0, 1 };
	        PluginWorkspace pluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
	        if (pluginWorkspace != null) {
	          int result = pluginWorkspace.showConfirmDialog(
	              TRANSLATOR.getTranslation(Tags.CLEAR_HISTORY),
	              TRANSLATOR.getTranslation(Tags.CLEAR_HISTORY_CONFIRMATION),
	              options,
	              optionIds);
	          if (result == optionIds[0]) {
	            clearHistory();
	          } else {
	            workingCopyCombo.setSelectedItem(workingCopyCombo.getModel().getElementAt(0));
	          }
	        }
	      } else {
	        if (selectedEntry != null) {
	          gitAccess.setRepositoryAsync(selectedEntry);
	        }
	      }
	    } finally {
	      inhibitRepoUpdate = false;
	    }
	  });
	}


	/**
	 * Clear history.
	 */
	private void clearHistory() {
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) workingCopyCombo.getModel();
		LinkedList<String> entries = new LinkedList<>();
		for (int i = 0; i < model.getSize(); i++) {
			entries.add(model.getElementAt(i));
		}

		model.removeAllElements(); 

		// The previously selected entry should remain
		if (!entries.isEmpty()) {
			String prevSelEntry = entries.removeFirst();
			model.addElement(prevSelEntry);
			workingCopyCombo.setSelectedItem(prevSelEntry);
		}

		// Remove the repositories from the options
		OptionsManager.getInstance().removeRepositoryLocations(entries);
	}


	/**
	 * Adds a file chooser to the browse button.
	 */
	private void addFileChooserListner() {
		// NOSONAR
		browseButton.addActionListener(
				e -> {
					File directory = PluginWorkspaceProvider.getPluginWorkspace().chooseDirectory();
					if (directory != null) {
						String directoryPath = directory.getAbsolutePath();
						if (FileUtil.isGitRepository(directoryPath)) {
							// adds the directory path to the combo box if it doesn't already exist
							OptionsManager.getInstance().addRepository(directoryPath);

							// Insert it first.
							DefaultComboBoxModel<String> defaultComboBoxModel = (DefaultComboBoxModel<String>) workingCopyCombo.getModel();
							defaultComboBoxModel.removeElement(directoryPath);
							defaultComboBoxModel.insertElementAt(directoryPath, 0);
							if (defaultComboBoxModel.getSize() == 2
									&& defaultComboBoxModel.getIndexOf(CLEAR_HISTORY_ENTRY) == -1) {
								// When there is another entry in the model except for the selected one,
								// also add the "Clear history..." entry
								defaultComboBoxModel.addElement(CLEAR_HISTORY_ENTRY);
							}

							// sets the directory path as the selected repository
							SwingUtilities.invokeLater(() -> workingCopyCombo.setSelectedItem(directoryPath));
						} else {
							PluginWorkspaceProvider.getPluginWorkspace()
							.showInformationMessage(TRANSLATOR.getTranslation(Tags.WORKINGCOPY_NOT_GIT_DIRECTORY));
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
		gbc.insets = new Insets(
				UIConstants.COMPONENT_TOP_PADDING,
				UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING,
				UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		this.add(new JLabel(TRANSLATOR.getTranslation(Tags.WORKING_COPY_LABEL)), gbc);

	}


	/**
	 * Adds the combo box to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 */
	private void addWorkingCopySelector(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = isLabeled ? 1 : 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;

		workingCopyCombo = new JComboBox<>();
		WorkingCopyToolTipRenderer renderer = new WorkingCopyToolTipRenderer(workingCopyCombo.getRenderer());
		workingCopyCombo.setRenderer(renderer);

		addWorkingCopySelectorListener();

		this.add(workingCopyCombo, gbc);
	}

	@Override
	public Dimension getMinimumSize() {
	  return new Dimension(UIUtil.DUMMY_MIN_WIDTH, super.getMinimumSize().height);
	}

	/**
	 * Load the recorded working copy locations into the combo and load repository.
	 */
	void initializeWorkingCopyCombo() {
		if (workingCopyCombo.getModel().getSize() == 0) {
			initWorkingCopyCombo();
			// The listener was inhibited to avoid unnecessary repository load. Do it now.
			loadRepository();
		}
	}


    /**
     * Load the recorded working copy locations into the combo
     */
	private void initWorkingCopyCombo() {
		try {
			inhibitRepoUpdate = true;
			List<String> repositoryEntries = new ArrayList<>(OptionsManager.getInstance().getRepositoryEntries());
			for (String repositoryEntry : repositoryEntries) {
				workingCopyCombo.addItem(repositoryEntry);
			}
			if (repositoryEntries.size() > 1) {
				workingCopyCombo.addItem(CLEAR_HISTORY_ENTRY);
			}

			String repositoryPath = OptionsManager.getInstance().getSelectedRepository();
			if (!repositoryPath.equals("")) {
				workingCopyCombo.setSelectedItem(repositoryPath);
			} else if (workingCopyCombo.getItemCount() > 0) {
				workingCopyCombo.setSelectedIndex(0);
			}
		} finally {
			inhibitRepoUpdate = false;
		}
	}


	/**
	 * Adds the browse button to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 * 
	 */
	private void addBrowseButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, 0,
				UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = isLabeled ? 2 : 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		browseButton = new ToolbarButton(null, false);
		browseButton.setIcon(Icons.getIcon(Icons.FILE_CHOOSER_ICON));
		browseButton.setToolTipText(TRANSLATOR.getTranslation(Tags.BROWSE_BUTTON_TOOLTIP));
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
	@SuppressWarnings("java:S110")
	private static final class WorkingCopyToolTipRenderer extends DefaultListCellRenderer {

	  /**
	   * This rendered is a wrapper over an old render.
	   */
	  private final ListCellRenderer<? super String> renderer;
	  
	  /**
     * Constructor.
     *
	   * @param listCellRenderer Render to wrap.
	   */
		public WorkingCopyToolTipRenderer(ListCellRenderer<? super String> listCellRenderer) {
      this.renderer = listCellRenderer;
    }

    @Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			@SuppressWarnings("unchecked")
      JLabel comp = (JLabel) renderer.getListCellRendererComponent((JList<String>)list, (String)value, index, isSelected, cellHasFocus);
			if (value != null) {
				if (value.equals(CLEAR_HISTORY_ENTRY)) {
					comp.setText(TRANSLATOR.getTranslation(Tags.CLEAR_HISTORY) + "...");
					comp.setFont(comp.getFont().deriveFont(Font.ITALIC));
				} else {
					comp.setToolTipText((String) value);
					String path = (String) value;
					path = path.replace("\\", "/");
					String rootFolder = path.substring(path.lastIndexOf('/') + 1);
					comp.setText(rootFolder);
				}
			}
			return comp;
		}
	}


	/**
	 * Listens on Git events and updates the GUI accordingly.
	 */
	private class GitEventUpdater extends GitEventAdapter {


		@Override
		public void operationAboutToStart(GitEventInfo info) {
			if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
				setWCSelectorsEnabled(false);
			}
		}


		@Override
		public void operationSuccessfullyEnded(GitEventInfo info) {
		  if (info.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
		    Runnable r = () -> {
		      setWCSelectorsEnabled(true);
		      updateComboboxModelAfterRepositoryChanged();
		      if (OptionsManager.getInstance().isDetectAndOpenXprFiles() && info instanceof WorkingCopyGitEventInfo) {
		        File wcDirectory = ((WorkingCopyGitEventInfo) info).getWorkingCopy();
		        List<File> xprFiles = FileUtil.findAllFilesByExtension(wcDirectory, ".xpr");
		        URL xprUrl = getXprURLfromXprFiles(xprFiles);
		        if (xprUrl != null) {
		          PluginWorkspaceProvider.getPluginWorkspace().open(xprUrl);
		        }
		      }
		    };
		    if (!SwingUtilities.isEventDispatchThread()) {
		      SwingUtilities.invokeLater(r);
		    } else {
		      r.run();
		    }
		  }
		}

		/**
		 * Get the project file URL from a list of project files.
		 * A dialog will be displayed for choosing one project if there are multiple files
		 *  
		 * @param xprFiles The project files 
		 * 
		 * @return A URL for the project or <code>null</code> if the URL is malformed or xprFiles is empty
		 */
		private URL getXprURLfromXprFiles(List<File> xprFiles) {
		  URL xprUrl = null;
		  URI currentXprURI = getCurrentXprURI();
		  try {
		    if(!xprFiles.isEmpty() && (currentXprURI == null || !xprFiles.contains(new File(currentXprURI)))) {
		      if (xprFiles.size() == 1) {
		        xprUrl = xprFiles.get(0).toURI().toURL();
		      } else {
		        OpenProjectDialog dialog= new OpenProjectDialog(
		            (JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
		            xprFiles);
		        dialog.setVisible(true);
		        if (dialog.getResult() == OKCancelDialog.RESULT_OK) {
		          xprUrl = dialog.getSelectedFile().toURI().toURL();
		        } 
		      }
		    }
		  } catch (MalformedURLException e) {
		    if (LOGGER.isDebugEnabled()) {
		      LOGGER.debug(e.getMessage(), e);
		    }
		  }
		  return xprUrl;
		}
	
  /**
   * @return The uri of the current project(xpr) or <code>null</code>
   */
  private URI getCurrentXprURI(){
    URI currentXPRuri = null;
    StandalonePluginWorkspace spw = (StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    try {
      Optional<URL> projectURLOpt = Optional.ofNullable(spw.getProjectManager().getCurrentProjectURL());
      currentXPRuri = projectURLOpt.isPresent() && FileUtil.isURLForLocalFile(projectURLOpt.get()) ? projectURLOpt.get().toURI() : null;
    } catch (URISyntaxException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(),e);
      }
    }
    return currentXPRuri;
  }
		
    /**
		 * Update the WC selectors enabled.
		 * 
		 * @param isEnabled the new state.
		 */
		private void setWCSelectorsEnabled(boolean isEnabled) {
			if (workingCopyCombo != null) {
				workingCopyCombo.setEnabled(isEnabled);
			}
			if (browseButton != null) {
				browseButton.setEnabled(isEnabled);
			}
		}


		@Override
		public void operationFailed(GitEventInfo info, Throwable t) {
			if (info instanceof WorkingCopyGitEventInfo) {
				WorkingCopyGitEventInfo wcInfo = (WorkingCopyGitEventInfo) info;
				if (wcInfo.getGitOperation() == GitOperation.OPEN_WORKING_COPY) {
					if (workingCopyCombo != null) {
						if (t instanceof RepositoryNotFoundException) {
							removeInexistentRepo(wcInfo.getWorkingCopy());

							SwingUtilities.invokeLater(() -> PluginWorkspaceProvider.getPluginWorkspace()
									.showInformationMessage(TRANSLATOR.getTranslation(Tags.WORKINGCOPY_REPOSITORY_NOT_FOUND)));
						} else if (t instanceof IOException) {
							SwingUtilities.invokeLater(() -> PluginWorkspaceProvider.getPluginWorkspace()
									.showErrorMessage("Could not load the repository. " + t.getMessage()));
						}
						workingCopyCombo.setEnabled(true);
					}
					if (browseButton != null) {
						browseButton.setEnabled(true);
					}
				}
			}
		}


		/**
		 * Remove inexistent repo (actually working-copy).
		 * 
		 * @param wc Working-copy.
		 */
		private void removeInexistentRepo(File wc) {
		  
		  String wcDir = wc.getAbsolutePath();
		  OptionsManager.getInstance().removeRepositoryLocation(wcDir);
		  workingCopyCombo.removeItem(wcDir);
		  if (workingCopyCombo.getItemCount() <= 2) {
		    workingCopyCombo.removeItem(CLEAR_HISTORY_ENTRY);
		  }
		  workingCopyCombo.setSelectedItem(null);
		}


		/**
		 * Updates the combo box model with the currently loaded repository.
		 */
		public void updateComboboxModelAfterRepositoryChanged() {
			if(workingCopyCombo.getModel().getSize() == 0) {
				initWorkingCopyCombo();
			}
			// The event was not triggered by the combo.
			try {
				File wc = GitAccess.getInstance().getWorkingCopy();
				String absolutePath = wc.getAbsolutePath();

				OptionsManager.getInstance().addRepository(absolutePath);
				OptionsManager.getInstance().saveSelectedRepository(absolutePath);

				if (!inhibitRepoUpdate) {
					inhibitRepoUpdate = true;
					try {
						if (FileUtil.isGitSubmodule(absolutePath)) {
							// An ugly hack to select the path in the combo without keeping it
							// in the model. We want to avoid adding it in the model because 
							// this path is not exactly an working copy (no .git in it)
							workingCopyCombo.setEditable(true);
							workingCopyCombo.setSelectedItem(absolutePath);
							workingCopyCombo.setEditable(false);
						} else {
							// Add it on the first position. 
							DefaultComboBoxModel<String> defaultComboBoxModel = (DefaultComboBoxModel<String>) workingCopyCombo.getModel();
							defaultComboBoxModel.removeElement(absolutePath);
							defaultComboBoxModel.insertElementAt(absolutePath, 0);
							if (// It makes sense to clear the history when you have at least 2 entries in the model. 
									defaultComboBoxModel.getSize() == 2 &&
									// No entry to clear history yet...
									defaultComboBoxModel.getIndexOf(CLEAR_HISTORY_ENTRY) == -1) {
								// It makes sense to clear the history when you have at least 2 entries in the model. 
								defaultComboBoxModel.addElement(CLEAR_HISTORY_ENTRY);
							}

							// Select it.
							workingCopyCombo.setSelectedItem(absolutePath);
						}
					} finally {
						inhibitRepoUpdate = false;
					}
				}
			} catch (NoRepositorySelected e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * @return the working copy combo.
	 */
	public JComboBox<String> getWorkingCopyCombo() {
		return workingCopyCombo;
	}


	/**
	 * @return the "Browse" button.
	 */
	public JButton getBrowseButton() {
		return browseButton;
	}

}
