package com.oxygenxml.sdksamples.workspace.git.view;

import javax.swing.JPanel;

public class StagingPanel extends JPanel {

	private WorkingCopySelectionPanel workingCopySelectionPanel;
	private UnstagedChangesPanel unstagedChangesPanel;
	private StagedChangesPanel stagedChangesPanel;
	private CommitPanel commitPanel;

	public StagingPanel(WorkingCopySelectionPanel workingCopySelectionPanel, UnstagedChangesPanel unstagedChangesPanel,
			StagedChangesPanel stagedChangesPanel, CommitPanel commitPanel) {
		this.workingCopySelectionPanel = workingCopySelectionPanel;
		this.unstagedChangesPanel = unstagedChangesPanel;
		this.stagedChangesPanel = stagedChangesPanel;
		this.commitPanel = commitPanel;
		init();
	}

	private void init() {
		this.add(workingCopySelectionPanel);
		
	}

}
