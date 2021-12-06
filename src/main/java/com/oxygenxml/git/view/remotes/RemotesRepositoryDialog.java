package com.oxygenxml.git.view.remotes;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.dialog.FileStatusDialog;
import com.oxygenxml.git.view.stash.StashCellRendersFactory;
import com.oxygenxml.git.view.util.CoalescingDocumentListener;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;
import ro.sync.exml.workspace.api.standalone.ui.Table;


/**
 * Dialog to present the repositories remote.
 * 
 * @author alex_smarandache
 *
 */
@SuppressWarnings("serial")
public class RemotesRepositoryDialog extends OKCancelDialog {

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LogManager.getLogger(RemotesRepositoryDialog.class);

	/**
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The dialog width.
	 */
	private static final int DIALOG_WIDTH = 650;

	/**
	 * The dialog height.
	 */
	private static final int DIALOG_HEIGHT = 250;
	
	/**
	 * The table that contains the remotes.
	 */
	private JTable remoteTable;

	/**
	 * Queue with actions to execute after user confirmation.
	 */
	private final transient Queue<Runnable> actionsToExecute = new LinkedList<>();


	/**
	 * Constructor.
	 */
	public RemotesRepositoryDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.REMOTES_DIALOG_TITLE), true
				);
		
		this.setResizable(false);

		try {
			getContentPane().add(createRemotesPanel());
		} catch (NoRepositorySelected e) {
			LOGGER.error(e, e);
		}

		pack();
		
		JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
				(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
				if (parentFrame != null) {
					setIconImage(parentFrame.getIconImage());
					setLocationRelativeTo(parentFrame);
				}

				setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				
				revalidate();
				pack();
				repaint();
	}
	
	@Override
	public Dimension getSize() {
		return new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT);
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getSize();
	}
	
	@Override
	public Dimension getPreferredSize() {
		return getSize();
	}
	
    @Override
    public Dimension getMaximumSize() {
    	return getSize();
    }
	
    
    /**
     * Present the current remote repositories, offers the user edit capabilities 
     * and saves the new remotes in the config file is the user confirms it.
     */
    public void configureRemotes() {
      super.setVisible(true);

      if(getResult() == RESULT_OK) {
        while(!actionsToExecute.isEmpty()) {
          actionsToExecute.remove().run();
        }
        try {
          GitAccess.getInstance().updateConfigFile();
        } catch (NoRepositorySelected e) {
          LOGGER.error(e, e);
        }
      }
    }

    
    /**
     * Create the main panel.
     * 
     * @return The created panel.
     * 
     * @throws NoRepositorySelected
     */
	private JPanel createRemotesPanel() throws NoRepositorySelected {
		JPanel remotesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		UIComponentsFactory factory = new UIComponentsFactory();

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;

		remoteTable = factory.createRemotesTable();
		JScrollPane tableRemotesScrollPane = new JScrollPane(remoteTable);

		remotesPanel.add(tableRemotesScrollPane, constraints);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(0, 0, 0, 0);
		constraints.gridy++;
		constraints.weightx = 1;
		constraints.weighty = 0;
		remotesPanel.add(factory.createButtonsPanel(), constraints);

		return remotesPanel;
	}
	
	
	/**
	 * Used for tests !!!
	 * 
	 * @return The table model.
	 */
	public RemotesTableModel getModel() {
	  return (RemotesTableModel) this.remoteTable.getModel();
	}
	
	/**
   * Used for tests !!!
   * 
   * @return The remote table.
   */
  public JTable getTable() {
    return remoteTable;
  }
	
	
	
	/**
	 * Factory for UI components on this dialog.
	 * 
	 * @author alex_smarandache
	 *
	 */
	private class UIComponentsFactory {

		/**
		 * Width for remote name column.
		 */
		private static final int REMOTE_NAME_WIDTH = 150;

		/**
		 * Edit remote button.
		 */
		private JButton editButton;

		/**
		 * Delete remote button.
		 */
		private JButton deleteButton;

		/**
		 * Model for remotes.
		 */
		private RemotesTableModel remotesModel;

		/**
		 * The remotes table.
		 */
		private JTable remotesTable;


		
		/**
	   * Add an new action in queue.
	   * 
	   * @param oldRemote   Old remote name.
	   * @param newRemote   New remote name.
	   * @param newURL      New URL.
	   */
	  private void scheduleRemoteUpdate(String oldRemote, String newRemote, String newURL) {
	    actionsToExecute.add(() -> {
	      try {
	        GitAccess.getInstance().updateRemote(oldRemote, newRemote, newURL);
	      } catch (NoRepositorySelected e1) {
	        LOGGER.error(e1, e1);
	      } 
	    });
	  }
	  
	  
		/**
		 * Creates the remotes table.
		 * 
		 * @return The remotes table.
		 * 
		 * @throws NoRepositorySelected
		 */
		JTable createRemotesTable() throws NoRepositorySelected {
			remotesModel = new RemotesTableModel();
			remotesModel.setRemotes(GitAccess.getInstance().getRemotesFromConfig());

			remotesTable = new Table(remotesModel) {

				@Override
				public JToolTip createToolTip() {
					return UIUtil.createMultilineTooltip(this).orElseGet(super::createToolTip);
				}

			};
			
			remotesTable.setDefaultRenderer(String.class, StashCellRendersFactory.getMessageCellRender());

			remotesTable.setFillsViewportHeight(true);
			remotesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			remotesTable.getTableHeader().setReorderingAllowed(false);
			remotesTable.getTableHeader().setVisible(true);

			remotesTable.getSelectionModel().addListSelectionListener(e -> {
				int selectedRow = remotesTable.getSelectedRow();
				if(selectedRow >= 0) {
					editButton.setEnabled(true);
					deleteButton.setEnabled(true);
				}
			});

			TableColumn statusCol = remotesTable.getColumnModel().getColumn(RemotesTableModel.REMOTE_COLUMN);
			statusCol.setMinWidth(REMOTE_NAME_WIDTH);
			statusCol.setPreferredWidth(REMOTE_NAME_WIDTH);
			statusCol.setMaxWidth(REMOTE_NAME_WIDTH);

			return remotesTable;
		}
		

		/**
		 * Create panel with buttons.
		 * 
		 * @return
		 */
		JPanel createButtonsPanel() {
			JPanel buttonsPanel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();

			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridwidth = 1;
			constraints.gridheight = 1;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.insets = new Insets(0, 0, 0, UIConstants.COMPONENT_RIGHT_PADDING);
			constraints.weightx = 0;
			constraints.weighty = 0;
			constraints.fill = GridBagConstraints.NONE;

			buttonsPanel.add(createAddButton(), constraints);

			constraints.gridx++;
			buttonsPanel.add(createEditButton(), constraints);

			constraints.gridx++;
			buttonsPanel.add(createDeleteButton(), constraints);
			
			constraints.gridx++;
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsPanel.add(new JPanel(), constraints);

			return buttonsPanel;
		}


		/**
		 * Crate the add button.
		 * 
		 * @return The created button.
		 */
		private JButton createAddButton() {

			return new Button(new AbstractAction(TRANSLATOR.getTranslation(Tags.ADD)) {
				@Override
				public void actionPerformed(ActionEvent e) {
					AddOrEditRemoteDialog dialog = new AddOrEditRemoteDialog(
							TRANSLATOR.getTranslation(Tags.ADD_REMOTE), 
							null, null);
					if(dialog.getResult() == OKCancelDialog.RESULT_OK) {
						final String remoteName = dialog.getRemoteName();
						final String remoteURL = dialog.getRemoteURL();
						final boolean remoteNameAlreadyExists = remotesModel.remoteAlreadyExists(remoteName);
						
						if(remoteNameAlreadyExists) {
							scheduleRemoteUpdate(remoteName, remoteName, remoteURL);
							remotesModel.editRemote(remoteName,remoteName, remoteURL);	
						} else {
							scheduleRemoteUpdate(null, remoteName, remoteURL);
							remotesModel.addRemote(dialog.getRemoteName(), dialog.getRemoteURL());	
						}
					}
				}
			});

		}					


		/**
		 * Create the edit button.
		 * 
		 * @return The created button.
		 */
		private JButton createEditButton() {
			editButton = new Button(new AbstractAction(TRANSLATOR.getTranslation(Tags.EDIT)) {
				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedRow = remotesTable.getSelectedRow();
					if(selectedRow >= 0) {
						AddOrEditRemoteDialog dialog = new AddOrEditRemoteDialog(TRANSLATOR.getTranslation(Tags.EDIT_REMOTE), 
								(String)remotesModel.getValueAt(selectedRow, RemotesTableModel.REMOTE_COLUMN), 
								(String)remotesModel.getValueAt(selectedRow, RemotesTableModel.URL_COLUMN));
						if(dialog.getResult() == OKCancelDialog.RESULT_OK) {
							final String oldRemoteName = (String)remotesModel.getValueAt(selectedRow, RemotesTableModel.REMOTE_COLUMN);
							final String remoteName = dialog.getRemoteName();
							final String remoteURL = dialog.getRemoteURL();

							scheduleRemoteUpdate(oldRemoteName, remoteName, remoteURL);
							remotesModel.editRemote(selectedRow, dialog.getRemoteName(), dialog.getRemoteURL());
						}
					}
				}
			});

			editButton.setEnabled(false);

			return editButton;
		}


		/**
		 * Create the delete button.
		 * 
		 * @return The created button.
		 */
		private JButton createDeleteButton() {
			deleteButton = new Button(new AbstractAction(TRANSLATOR.getTranslation(Tags.DELETE)) {
				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedRow = remotesTable.getSelectedRow();
					if(selectedRow >= 0) {
						final String remoteName = (String)remotesModel.getValueAt(selectedRow, 
								RemotesTableModel.REMOTE_COLUMN);
						 int answer = FileStatusDialog.showWarningMessageWithConfirmation(
						            TRANSLATOR.getTranslation(Tags.DELETE_REMOTE),
						            MessageFormat.format(
						            		TRANSLATOR.getTranslation(Tags.DELETE_REMOTE_CONFIRMATION_MESSAGE),
						            		remoteName),
						            TRANSLATOR.getTranslation(Tags.YES),
						            TRANSLATOR.getTranslation(Tags.NO));
						    if (OKCancelDialog.RESULT_OK == answer) {

						    
								actionsToExecute.add(new Runnable() {
									
									@Override
									public void run() {
										try {
											GitAccess.getInstance().removeRemote(remoteName);
										} catch (NoRepositorySelected e1) {
											LOGGER.error(e1, e1);
										}	
										
									}
								});

								remotesModel.deleteRemote(selectedRow);
						    }
					}
				}
			});

			deleteButton.setEnabled(false);

			return deleteButton;
		}
		
		
		/**
		 * Dialog for adding or editing a remote.
		 * 
		 * @author alex_smarandache
		 *
		 */
		private class AddOrEditRemoteDialog extends OKCancelDialog {

			/**
			 * Text field for remote name.
			 */
			private final JTextField remoteNameTF = OxygenUIComponentsFactory.createTextField();

			/**
			 * Text field for remote URL.
			 */
			private final JTextField remoteURLTF = OxygenUIComponentsFactory.createTextField();

			/**
			 * The dialog width.
			 */
			private static final int WIDTH = 350;
			
			/**
			 * The dialog height.
			 */
			private static final int HEIGHT = 150;
			
			/**
			 * The old remote rename.
			 */
			private final String oldRemoteName;
			
			/**
			 * The old remote URL.
			 */
			private final String oldRemoteURL;

			
			/**
			 * Constructor.
			 * 
			 * @param title       The dialog title.
			 * @param remoteName  The remote name. May be null if the dialog is for adding a new remote.
			 * @param remoteURL   The remote  URL.  May be null if the dialog is for adding a new remote.
			 */
			public AddOrEditRemoteDialog(String title, String remoteName, String remoteURL) {
				super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
						title, true
						);

				oldRemoteName = remoteName;
				oldRemoteURL = remoteURL;
				
				if(remoteName != null) {
					remoteNameTF.setText(remoteName);
					remoteNameTF.setCaretPosition(0);
					remoteNameTF.setEditable(false);
				}

				if(remoteURL != null ) {
					remoteURLTF.setText(remoteURL);
					remoteURLTF.setCaretPosition(0);
					remoteURLTF.selectAll();
				}

				this.getOkButton().setEnabled(remoteName != null && remoteURL != null);

				getContentPane().add(createGUIPanel());
				pack();

				CoalescingDocumentListener updateOkButtonListener = new CoalescingDocumentListener(() -> 
					this.getOkButton().setEnabled(remoteURLTF.getText() != null && 
							!remoteURLTF.getText().isEmpty() && remoteNameTF.getText() != null 
							&& !remoteNameTF.getText().isEmpty())
				);

				remoteNameTF.getDocument().addDocumentListener(updateOkButtonListener);
				remoteURLTF.getDocument().addDocumentListener(updateOkButtonListener);

				JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
						(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
						if (parentFrame != null) {
							setIconImage(parentFrame.getIconImage());
							setLocationRelativeTo(parentFrame);
						}

						setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

						this.setVisible(true);
						this.setResizable(false);
			}
			
			@Override
			public Dimension getSize() {
				return new Dimension(WIDTH, HEIGHT);
			}
			
			@Override
			public Dimension getPreferredSize() {
				return getSize();
			}
			
			@Override
			public Dimension getMinimumSize() {
				return getSize();
			}
			
			@Override
			public Dimension getMaximumSize() {
				return getSize();
			}

			/**
			 * Create the dialog GUI.
			 * 
			 * @return The created panel.
			 */
			private JPanel createGUIPanel() {
				JPanel guiPanel = new JPanel(new GridBagLayout());
				GridBagConstraints constraints = new GridBagConstraints();

				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.gridwidth = 1;
				constraints.gridheight = 1;
				constraints.anchor = GridBagConstraints.WEST;
				constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, UIConstants.COMPONENT_RIGHT_PADDING);
				constraints.weightx = 0;
				constraints.weighty = 0;
				constraints.fill = GridBagConstraints.NONE;

				JLabel remoteNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.REMOTE_NAME) + ":");
				guiPanel.add(remoteNameLabel, constraints);

				constraints.gridx++;
				constraints.weightx = 1;
				constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
				constraints.fill = GridBagConstraints.HORIZONTAL;
				guiPanel.add(remoteNameTF, constraints);

				constraints.gridx = 0;
				constraints.insets = new Insets(0, 0, 0, UIConstants.COMPONENT_RIGHT_PADDING);
				constraints.gridy++;
				constraints.fill = GridBagConstraints.NONE;
				constraints.weightx = 0;
				JLabel remoteURLLabel = new JLabel(TRANSLATOR.getTranslation(Tags.REMOTE_URL) + ":");
				guiPanel.add(remoteURLLabel, constraints);

				constraints.gridx++;
				constraints.weightx = 1;
				constraints.insets = new Insets(0, 0, 0, 0);
				constraints.fill = GridBagConstraints.HORIZONTAL;
				guiPanel.add(remoteURLTF, constraints);

				return guiPanel;
			}


			/**
			 * @return The remote name.
			 */
			public String getRemoteName() {
				return remoteNameTF.getText();
			}


			/**
			 * @return The remote URL.
			 */
			public String getRemoteURL() {
				return remoteURLTF.getText();
			}
			
			@Override
			protected void doOK() {
				if(oldRemoteName == null || oldRemoteURL == null) {
	                if(remotesModel.remoteAlreadyExists(remoteNameTF.getText())) {
	                	int answer = FileStatusDialog.showWarningMessageWithConfirmation(
					            TRANSLATOR.getTranslation(Tags.ADD_REMOTE),
					            MessageFormat.format(
					            		TRANSLATOR.getTranslation(Tags.REMOTE_ALREADY_EXISTS_CONFIRMATION_MESSAGE),
					            		remoteNameTF.getText()),
					            TRANSLATOR.getTranslation(Tags.YES),
					            TRANSLATOR.getTranslation(Tags.NO));
					    if (OKCancelDialog.RESULT_OK == answer) {
					    	super.doOK();
					    }
	                } else {
	                	super.doOK();
	                }
				} else if(!oldRemoteURL.equals(remoteURLTF.getText())) {
					super.doOK();
				} else {
					super.doCancel();
				}
			}
		}

	}


}
