package com.oxygenxml.git.view.remotes;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;

import com.oxygenxml.git.constants.UIConstants;
import com.oxygenxml.git.service.GitAccess;
import com.oxygenxml.git.service.NoRepositorySelected;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;
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
	 * The translator.
	 */
	private static final Translator TRANSLATOR = Translator.getInstance();

	/**
	 * The minimum default dialog width.
	 */
	private static final int MIN_WIDTH = 200;

	/**
	 * Logger for logging.
	 */
	private static final Logger LOGGER = LogManager.getLogger(CurrentBranchRemotesDialog.class);

	/**
	 * Combo box with all remotes from current repository.
	 */
	private final JComboBox<String> remotes = new JComboBox<>();

	/**
	 * Combo box with all branches from current repository.
	 */
	private final JComboBox<String> branches = new JComboBox<>();

	/**
	 * The current branch.
	 */
	private String currentBranch;

	/**
	 * The first remote selected.
	 */
	private String firstRemoteSelection;

	/**
	 * The first remote branch selected.
	 */
	private String firstBranchSelection;



	/**
	 * Constructor.
	 */
	public CurrentBranchRemotesDialog() {
		super((JFrame) PluginWorkspaceProvider.getPluginWorkspace().getParentFrame(),
				TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH), true
				);
		try {
			currentBranch = GitAccess.getInstance().getBranchInfo().getBranchName();
			firstRemoteSelection = GitAccess.getInstance().getRemoteFromCurrentBranch();
			final StoredConfig config = GitAccess.getInstance().getRepository().getConfig();

			List<String> remotesNames = new ArrayList<>(GitAccess.getInstance()
					.getRemotesFromConfig().keySet());	

			remotes.addActionListener(e -> {
				branches.removeAllItems();
				GitAccess.getInstance();
				try {
					URIish sourceURL = new URIish(config.getString(ConfigConstants.CONFIG_REMOTE_SECTION,
							(String)remotes.getSelectedItem(), ConfigConstants.CONFIG_KEY_URL));
					Collection<Ref> branchesConfig = GitAccess.getInstance().doListRemoteBranchesInternal(
							sourceURL, null);
					for(Ref branch: branchesConfig) {
						branches.addItem(remotes.getSelectedItem() + "/" + branch.getName());
					}
				} catch (URISyntaxException e1) {
					LOGGER.error(e1); 
				}
			});

			for(String remote: remotesNames) {
				remotes.addItem(remote);
				if(remote.equals(firstRemoteSelection)) {
					remotes.setSelectedItem(remote);
				}
			}

			firstBranchSelection = (String)branches.getSelectedItem();
			BranchConfigurations branchConfig = new BranchConfigurations(config, currentBranch);
			branches.setSelectedItem(remotes.getSelectedItem() + "/" + branchConfig.getMerge());

		} catch (NoRepositorySelected e) {
			LOGGER.error(e, e);
		}

		getContentPane().add(createGUIPanel());

		setSize(MIN_WIDTH, MIN_WIDTH);

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

		JLabel remoteNameLabel = new JLabel(TRANSLATOR.getTranslation(Tags.LOCAL_BRANCH));
		guiPanel.add(remoteNameLabel, constraints);

		constraints.gridx++;
		guiPanel.add(new JLabel(currentBranch), constraints);

		constraints.gridx = 0;
		constraints.gridy++;
		guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.CONFIGURE_REMOTE_FOR_BRANCH) + ":"), constraints);

		constraints.weightx = 1;
		constraints.gridx++;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		guiPanel.add(remotes, constraints);

		constraints.gridx = 0;
		constraints.gridy++;
		constraints.fill = GridBagConstraints.NONE;
		guiPanel.add(new JLabel(TRANSLATOR.getTranslation(Tags.REMOTE_BRANCH)), constraints);

		constraints.weightx = 1;
		constraints.gridx++;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		guiPanel.add(branches, constraints);

		return guiPanel;
	}


	@Override
	protected void doOK() {
		if(firstRemoteSelection != null  
				&& (!firstRemoteSelection.equals(remotes.getSelectedItem()) 
						|| (firstBranchSelection != null 
						&& !firstBranchSelection.equals(branches.getSelectedItem())))) {
			try {
				BranchConfigurations branchConfig = new BranchConfigurations(
						GitAccess.getInstance().getRepository().getConfig(), currentBranch);
				String selectedRemote = (String)remotes.getSelectedItem();
				branchConfig.setRemote(selectedRemote);
				branchConfig.setMerge(((String)branches.getSelectedItem()).substring(selectedRemote.length() + 1));
				GitAccess.getInstance().updateConfigFile();
			} catch (NoRepositorySelected e) {
				LOGGER.error(e, e);
			}
		}

		super.doOK();
	}

}
