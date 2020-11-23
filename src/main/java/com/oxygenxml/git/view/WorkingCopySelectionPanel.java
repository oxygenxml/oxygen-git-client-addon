package com.oxygenxml.git.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.options.OptionsManager;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.GitControllerBase;
import com.oxygenxml.git.service.GitEventAdapter;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.utils.FileHelper;
import com.oxygenxml.git.view.event.GitEventInfo;
import com.oxygenxml.git.view.event.GitOperation;
import com.oxygenxml.git.view.event.WorkingCopyGitEventInfo;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
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
	private static Logger logger = Logger.getLogger(WorkingCopySelectionPanel.class);

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
	 * The git API, containing the commands
	 */
	private GitAccess gitAccess = GitAccess.getInstance();

	/**
	 * The translator for the messages that are displayed in this panel
	 */
	private static Translator translator = Translator.getInstance();
	
	/**
	 * Clear history.
	 */
	private static final String CLEAR_HISTORY_ENTRY = "CLEAR_HISTORY";
	
  /**
   * Constructor.
   * @param gitController Git operations controller.
   */
	public WorkingCopySelectionPanel(GitControllerBase gitController) {
	  createGUI();
	  gitController.addGitListener(new GitEventUpdater());
	}

	public JComboBox<String> getWorkingCopyCombo() {
		return workingCopyCombo;
	}

	public JButton getBrowseButton() {
		return browseButton;
	}

	/**
	 * Creates the components and adds listeners to some of them. Basically this
	 * creates the panel
	 */
	private void createGUI() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		addLabel(gbc);
		addWorkingCopySelector(gbc);
		addBrowseButton(gbc);

		addFileChooserListner();
		
		this.setMinimumSize(new Dimension(UIConstants.PANEL_WIDTH, UIConstants.WORKINGCOPY_PANEL_HEIGHT));
	}
	
	/**
	 * <code>true</code> to inhibit repository update when the selection changes in the combo.
	 * <code>false</code> to update the repository when the selection changes in the combo.
	 */
	private boolean inhibitRepoUpdate = false;

	/**
	 * Adds a state change listener on the working copy selector combo box. When a
	 * new working copy is selected this listener will execute
	 */
	private void addWorkingCopySelectorListener() {
	  workingCopyCombo.addItemListener(
	      new ItemListener() { // NOSONAR
	    @Override
	    public void itemStateChanged(ItemEvent e) {
	      // Don't do anything if the event was originated by us.
	      if (!inhibitRepoUpdate && e.getStateChange() == ItemEvent.SELECTED) {
	        inhibitRepoUpdate = true;
	        try {
	          // get and save the selected Option so that at restart the same
	          // repository will be selected
	          String selectedEntry = (String) workingCopyCombo.getSelectedItem();
	          logger.debug("Selected working copy: " + selectedEntry);
	          if (CLEAR_HISTORY_ENTRY.equals(selectedEntry)) {
	            SwingUtilities.invokeLater(() -> 
	            {
	              String[] options = new String[] { 
	                  "   " + translator.getTranslation(Tags.YES) + "   ",
	                  "   " + translator.getTranslation(Tags.NO) + "   "};
	              int[] optionIds = new int[] { 0, 1 };
	              int result = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).showConfirmDialog(
	                  translator.getTranslation(Tags.CLEAR_HISTORY),
	                  translator.getTranslation(Tags.CLEAR_HISTORY_CONFIRMATION),
	                  options,
	                  optionIds);
	              if (result == optionIds[0]) {
	                clearHistory();
	              } else {
	                workingCopyCombo.setSelectedItem(workingCopyCombo.getModel().getElementAt(0));
	              }
	            });
	          } else {
	            gitAccess.setRepositoryAsync(selectedEntry);
	          }
	        } finally {
	          inhibitRepoUpdate = false;
	        }
	      }
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
		browseButton.addActionListener(
		    new ActionListener() { // NOSONAR

			@Override
      public void actionPerformed(ActionEvent e) {
				File directory = ((StandalonePluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace()).chooseDirectory();
				if (directory != null) {
					String directoryPath = directory.getAbsolutePath();
					if (FileHelper.isGitRepository(directoryPath) && directoryPath != null) {
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
								.showInformationMessage(translator.getTranslation(Tags.WORKINGCOPY_NOT_GIT_DIRECTORY));
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
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		this.add(new JLabel(translator.getTranslation(Tags.WORKING_COPY_LABEL)), gbc);

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
	  gbc.gridx = 1;
	  gbc.gridy = 0;
	  gbc.weightx = 1;
	  gbc.weighty = 0;

	  workingCopyCombo = new JComboBox<>();
	  WorkingCopyToolTipRenderer renderer = new WorkingCopyToolTipRenderer();
	  workingCopyCombo.setRenderer(renderer);
	  int height = (int) workingCopyCombo.getPreferredSize().getHeight();
	  workingCopyCombo.setMinimumSize(new Dimension(10, height));

	  addWorkingCopySelectorListener();
	  
	  this.add(workingCopyCombo, gbc);
	}

	/**
	 * Load the recorded workinf copy locations into the combo.
	 */
  void loadEntries() {
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
  }

	/**
	 * Adds the browse button to the panel
	 * 
	 * @param gbc
	 *          - the constraints used for this component
	 * 
	 */
	private void addBrowseButton(GridBagConstraints gbc) {
		gbc.insets = new Insets(UIConstants.COMPONENT_TOP_PADDING, UIConstants.COMPONENT_LEFT_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		browseButton = new ToolbarButton(null, false);
		browseButton.setIcon(Icons.getIcon(Icons.FILE_CHOOSER_ICON));
		browseButton.setToolTipText(translator.getTranslation(Tags.BROWSE_BUTTON_TOOLTIP));
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
	private static final class WorkingCopyToolTipRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value != null) {
			  if (((String) value).equals(CLEAR_HISTORY_ENTRY)) {
			    comp.setText(translator.getTranslation(Tags.CLEAR_HISTORY) + "...");
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
        };
        if (!SwingUtilities.isEventDispatchThread()) {
          SwingUtilities.invokeLater(r);
        } else {
          r.run();
        }
      }
    }
    
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
                  .showInformationMessage(translator.getTranslation(Tags.WORKINGCOPY_REPOSITORY_NOT_FOUND)));
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
      if (workingCopyCombo.getItemCount() > 0) {
        // Fallback to the previously loaded WC. We assume its the topmost in the list. Not to elegant...
        workingCopyCombo.setSelectedIndex(0);
      } else {
        workingCopyCombo.setSelectedItem(null);
        gitAccess.closeRepo();
      }
      workingCopyCombo.removeItem(wcDir);
    }
    
    /**
     * Updates the combo box model with the currently loaded repository.
     */
    public void updateComboboxModelAfterRepositoryChanged() {
      // The event was not triggered by the combo.
      try {
        File wc = GitAccess.getInstance().getWorkingCopy();
        String absolutePath = wc.getAbsolutePath();
        
        OptionsManager.getInstance().addRepository(absolutePath);
        OptionsManager.getInstance().saveSelectedRepository(absolutePath);
        
        if (!inhibitRepoUpdate) {
          inhibitRepoUpdate = true;
          try {
            if (FileHelper.isGitSubmodule(absolutePath)) {
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
        logger.debug(e, e);
      }
    }
	}
}
