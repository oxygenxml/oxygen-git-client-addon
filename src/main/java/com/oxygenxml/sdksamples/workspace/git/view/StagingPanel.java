package com.oxygenxml.sdksamples.workspace.git.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

public class StagingPanel extends JPanel {

	private WorkingCopySelectionPanel workingCopySelectionPanel;
	private UnstagedChangesPanel unstagedChangesPanel;
	private UnstagedChangesPanel stagedChangesPanel;
	private CommitPanel commitPanel;

	public StagingPanel(WorkingCopySelectionPanel workingCopySelectionPanel, UnstagedChangesPanel unstagedChangesPanel,
			UnstagedChangesPanel stagedChangesPanel, CommitPanel commitPanel) {
		this.workingCopySelectionPanel = workingCopySelectionPanel;
		this.unstagedChangesPanel = unstagedChangesPanel;
		this.stagedChangesPanel = stagedChangesPanel;
		this.commitPanel = commitPanel;
	}

	public WorkingCopySelectionPanel getWorkingCopySelectionPanel() {
		return workingCopySelectionPanel;
	}

	public void setWorkingCopySelectionPanel(WorkingCopySelectionPanel workingCopySelectionPanel) {
		this.workingCopySelectionPanel = workingCopySelectionPanel;
	}

	public UnstagedChangesPanel getUnstagedChangesPanel() {
		return unstagedChangesPanel;
	}

	public void setUnstagedChangesPanel(UnstagedChangesPanel unstagedChangesPanel) {
		this.unstagedChangesPanel = unstagedChangesPanel;
	}

	public UnstagedChangesPanel getStagedChangesPanel() {
		return stagedChangesPanel;
	}

	public void setStagedChangesPanel(UnstagedChangesPanel stagedChangesPanel) {
		this.stagedChangesPanel = stagedChangesPanel;
	}

	public CommitPanel getCommitPanel() {
		return commitPanel;
	}

	public void setCommitPanel(CommitPanel commitPanel) {
		this.commitPanel = commitPanel;
	}

	public void createGUI() {
		this.setLayout(new GridBagLayout());
		
		

		GridBagConstraints gbc = new GridBagConstraints();

		addWorkingCopySelectionPanel(gbc);
		addUnstagedChangesPanel(gbc);
		addStagedChangesPanel(gbc);
		addCommitPanel(gbc);
		
		unstagedChangesPanel.createGUI();
		stagedChangesPanel.createGUI();
		commitPanel.createGUI();
		workingCopySelectionPanel.createGUI();

	}

	private void addWorkingCopySelectionPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(workingCopySelectionPanel, gbc);

	}

	private void addUnstagedChangesPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(unstagedChangesPanel, gbc);

	}

	private void addStagedChangesPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(stagedChangesPanel, gbc);
	}

	private void addCommitPanel(GridBagConstraints gbc) {
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1;
		gbc.weighty = 0;
		this.add(commitPanel, gbc);
	}

}
