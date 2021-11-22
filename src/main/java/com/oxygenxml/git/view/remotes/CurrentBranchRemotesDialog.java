package com.oxygenxml.git.view.remotes;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.view.branches.BranchConfigurations;

import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.standalone.ui.OKCancelDialog;


/**
 * Dialog used to edit the current branch remote.
 * 
 * @author alex_smarandache
 *
 */
public class CurrentBranchRemotesDialog extends OKCancelDialog {
	
	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LogManager.getLogger(CurrentBranchRemotesDialog.class);

	/**
	 * Combo box with all remotes from current repository.
	 */
	private final JComboBox<String> remotes = new JComboBox<>();
	
	/**
	 * The current branch.
	 */
	private final String currentBranch;
	
	/**
	 * The first remote selected.
	 */
	private final String firstSelection;
	
	
	/**
	 * Constructor.
	 */
	public CurrentBranchRemotesDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				Tags.REMOTE, true
				);
		
		currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();
		firstSelection = GitAccess.getInstance().getRemoteFromCurrentBranch();
		
		try {
			List<String> remotesNames = new ArrayList<>(GitAccess.getInstance()
					.getRemotesFromConfig().keySet());
			for(String remote: remotesNames) {
				remotes.addItem(remote);
				if(remote.equals(firstSelection)) {
				   remotes.setSelectedItem(remote);
				}
			}
		} catch (NoRepositorySelected e) {
			LOGGER.error(e, e);
		}

		getContentPane().add(createGUIPanel());
		pack();
		
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
		constraints.insets = new Insets(0, 0, UIConstants.COMPONENT_BOTTOM_PADDING, 0);
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.fill = GridBagConstraints.NONE;

		JLabel remoteNameLabel = new JLabel("Current branch: ");
		guiPanel.add(remoteNameLabel, constraints);

		constraints.gridx++;
		guiPanel.add(new JLabel(currentBranch), constraints);
		
		constraints.gridx = 0;
		constraints.gridy++;
		guiPanel.add(new JLabel("Branch remote: "), constraints);
		
		constraints.weightx = 1;
		constraints.gridx++;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		guiPanel.add(remotes, constraints);

		return guiPanel;
	}
	
	
	@Override
	protected void doOK() {
		if(firstSelection != null && !firstSelection.equals(remotes.getSelectedItem())) {
			try {
				BranchConfigurations branchConfig = new BranchConfigurations(
						GitAccess.getInstance().getRepository().getConfig(), currentBranch);
				branchConfig.setRemote((String)remotes.getSelectedItem());
				GitAccess.getInstance().updateConfigFile();
			} catch (NoRepositorySelected e) {
				LOGGER.error(e, e);
			}
		}
		
		super.doOK();
	}
	
}
