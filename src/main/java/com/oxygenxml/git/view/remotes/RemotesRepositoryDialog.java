package com.oxygenxml.git.view.remotes;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

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

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
import com.oxygenxml.git.view.util.CoalescingDocumentListener;
import com.oxygenxml.git.view.util.UIUtil;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.Button;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;
import ro.sync.exml.workspace.api.standalone.ui.OxygenUIComponentsFactory;
import ro.sync.exml.workspace.api.standalone.ui.Table;

public class RemotesRepositoryDialog extends OKCancelDialog {

	/**
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The dialog width.
	 */
	private static final int DIALOG_WIDTH = 300;

	/**
	 * The dialog height.
	 */
	private static final int DIALOG_HEIGHT = 50;


	/**
	 * Constructor.
	 */
	public RemotesRepositoryDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.REMOTE), true
				);

		try {
			getContentPane().add(createRemotesPanel());
		} catch (NoRepositorySelected e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pack();

		JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
				(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
				if (parentFrame != null) {
					setIconImage(parentFrame.getIconImage());
					setLocationRelativeTo(parentFrame);
				}

				setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
				setMaximumSize(getPreferredSize());

				this.setVisible(true);
				this.setResizable(false);
	}


	private JPanel createRemotesPanel() throws NoRepositorySelected {
		JPanel remotesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		UIComponentsFactory factory = new UIComponentsFactory();

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(
				UIConstants.COMPONENT_TOP_PADDING,
				UIConstants.COMPONENT_LEFT_LARGE_PADDING,
				UIConstants.COMPONENT_BOTTOM_PADDING,
				0);
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		JScrollPane tableRemotesScrollPane = new JScrollPane(factory.createRemotesTable());
		tableRemotesScrollPane.setMinimumSize(tableRemotesScrollPane.getPreferredSize());
		tableRemotesScrollPane.setMaximumSize(tableRemotesScrollPane.getPreferredSize());

		remotesPanel.add(tableRemotesScrollPane, constraints);

		constraints.fill = GridBagConstraints.NONE;
		constraints.gridy++;
		remotesPanel.add(factory.createButtonsPanel(), constraints);

		constraints.gridy++;
		constraints.weightx = 1;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		remotesPanel.add(new JPanel(), constraints);

		return remotesPanel;

	}



	private class UIComponentsFactory {

		/**
		 * Width for remote name column.
		 */
		private static final int REMOTE_NAME_WIDTH = 150;

		/**
		 * Add remote button;
		 */
		private JButton addButton;

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

			return buttonsPanel;
		}

		private JButton createAddButton() {

			addButton = new Button(new AbstractAction("Add") {
				@Override
				public void actionPerformed(ActionEvent e) {
					AddOrEditRemoteDialog dialog = new AddOrEditRemoteDialog("Add remote", 
							null, null);
					if(dialog.getResult() == OKCancelDialog.RESULT_OK) {
						remotesModel.addRemote(dialog.getRemoteName(), dialog.getRemoteURL());	
					}
				}
			});

			return addButton;
		}

		private JButton createEditButton() {
			editButton = new Button(new AbstractAction("Edit") {
				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedRow = remotesTable.getSelectedRow();
					if(selectedRow >= 0) {
						AddOrEditRemoteDialog dialog = new AddOrEditRemoteDialog("Add remote", 
								(String)remotesModel.getValueAt(selectedRow, RemotesTableModel.REMOTE_COLUMN), 
								(String)remotesModel.getValueAt(selectedRow, RemotesTableModel.URL_COLUMN));
						if(dialog.getResult() == OKCancelDialog.RESULT_OK) {
							remotesModel.addRemote(dialog.getRemoteName(), dialog.getRemoteURL());	
						}
					}
				}
			});

			editButton.setEnabled(false);

			return editButton;
		}

		private JButton createDeleteButton() {
			deleteButton = new Button(new AbstractAction("Delete") {
				@Override
				public void actionPerformed(ActionEvent e) {
					remotesModel.addRemote("test name", "test url");	
				}
			});

			deleteButton.setEnabled(false);

			return deleteButton;
		}




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

			if(remoteName != null) {
				remoteNameTF.setText(remoteName);
				remoteNameTF.setCaretPosition(0);
				remoteNameTF.selectAll();
			}

			if(remoteURL != null ) {
				remoteURLTF.setText(remoteURL);
				remoteURLTF.setCaretPosition(0);
			}
			
			this.getOkButton().setEnabled(remoteName != null && remoteURL != null);

			getContentPane().add(createGUIPanel());
			pack();

			CoalescingDocumentListener updateOkButtonListener = new CoalescingDocumentListener(() -> {
				this.getOkButton().setEnabled(remoteURLTF.getText() != null && 
						!remoteURLTF.getText().isEmpty() && remoteNameTF.getText() != null 
						&& !remoteNameTF.getText().isEmpty());
			});

			remoteNameTF.getDocument().addDocumentListener(updateOkButtonListener);
			remoteURLTF.getDocument().addDocumentListener(updateOkButtonListener);

			JFrame parentFrame = PluginWorkspaceProvider.getPluginWorkspace() != null ? 
					(JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame() : null;
					if (parentFrame != null) {
						setIconImage(parentFrame.getIconImage());
						setLocationRelativeTo(parentFrame);
					}

					setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					setPreferredSize(new Dimension(DIALOG_WIDTH * 2, DIALOG_HEIGHT));
					setMaximumSize(getPreferredSize());

					this.setVisible(true);
					this.setResizable(false);
		}

		private JPanel createGUIPanel() {
			JPanel guiPanel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();

			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridwidth = 1;
			constraints.gridheight = 1;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.insets = new Insets(0, 0, 0, 0);
			constraints.weightx = 0;
			constraints.weighty = 0;
			constraints.fill = GridBagConstraints.NONE;
			
			JLabel remoteNameLabel = new JLabel("Remote name" + ":");
			guiPanel.add(remoteNameLabel, constraints);
			
			constraints.gridx++;
			constraints.weightx = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			guiPanel.add(remoteNameTF, constraints);
			
			constraints.gridx = 0;
			constraints.gridy++;
			constraints.fill = GridBagConstraints.NONE;
			constraints.weightx = 0;
			JLabel remoteURLLabel = new JLabel("Remote URL" + ":");
			guiPanel.add(remoteURLLabel, constraints);
			
			constraints.gridx++;
			constraints.weightx = 1;
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

	}





}
